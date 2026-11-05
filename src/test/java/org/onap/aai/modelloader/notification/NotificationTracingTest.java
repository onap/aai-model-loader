/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2026 Deutsche Telekom AG Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.modelloader.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.W3CPropagation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.service.ArtifactDeploymentManager;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.NotificationMessageContext;
import org.slf4j.MDC;

class NotificationTracingTest {
    private final List<MutableSpan> finished = new ArrayList<>();
    private final ObservationRegistry registry = ObservationRegistry.create();
    private Tracing brave;
    private Tracer tracer;
    private NotificationTracing tracing;

    @BeforeEach
    void setUp() {
        brave = Tracing.newBuilder().traceId128Bit(true).sampler(Sampler.ALWAYS_SAMPLE)
                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
                        .addScopeDecorator(MDCScopeDecorator.get()).build())
                .propagationFactory(new W3CPropagation())
                .addSpanHandler(new SpanHandler() {
                    @Override
                    public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                        finished.add(new MutableSpan(span));
                        return true;
                    }
                }).build();
        tracer = new BraveTracer(brave.tracer(), new BraveCurrentTraceContext(brave.currentTraceContext()));
        tracing = new NotificationTracing(Optional.of(tracer), Optional.of(new BravePropagator(brave)), registry);
        registry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        });
    }

    @AfterEach
    void tearDown() {
        brave.close();
        MDC.clear();
    }

    @Test
    void restoresAmbientTraceObservationAndMdcAfterFailureAndStartsCleanNextMessage() {
        Span ambient = tracer.nextSpan().name("ambient").start();
        Observation observation = Observation.start("ambient", registry);
        try (Tracer.SpanInScope ignored = tracer.withSpan(ambient);
                Observation.Scope observationScope = observation.openScope()) {
            MDC.put("caller", "preserved");
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            assertThatThrownBy(() -> tracing.trace(notification(Map.of("traceparent", "invalid")), span -> {
                assertThat(span.context().traceId()).isNotEqualTo(ambient.context().traceId());
                assertThat(registry.getCurrentObservation()).isNull();
                assertThat(MDC.get("traceId")).isEqualTo(span.context().traceId());
                assertThat(MDC.get("spanId")).isEqualTo(span.context().spanId());
                MDC.put("caller", "modified");
                throw new IllegalStateException("failed callback");
            })).hasMessage("failed callback");
            assertThat(tracer.currentSpan().context()).isEqualTo(ambient.context());
            assertThat(registry.getCurrentObservation()).isSameAs(observation);
            assertThat(MDC.getCopyOfContextMap()).isEqualTo(previousMdc);

            tracing.trace(notification(Map.of()), span -> {
                assertThat(span.context().traceId()).isNotEqualTo(ambient.context().traceId());
                assertThat(registry.getCurrentObservation()).isNull();
            });
            assertThat(tracer.currentSpan().context()).isEqualTo(ambient.context());
            assertThat(MDC.getCopyOfContextMap()).isEqualTo(previousMdc);
        } finally {
            ambient.end();
            observation.stop();
        }
        assertThat(finished.get(0).error()).hasMessage("failed callback");
        assertThat(finished.get(0).parentId()).isNull();
        assertThat(finished.get(1).parentId()).isNull();
        assertThat(finished.get(0).traceId()).isNotEqualTo(finished.get(1).traceId());
        assertThat(tracer.currentSpan()).isNull();
    }

    @Test
    void recordsCaughtDeploymentFailureAsSpanError() throws Exception {
        ArtifactDownloadManager downloads = mock(ArtifactDownloadManager.class);
        ArtifactDeploymentManager deployments = mock(ArtifactDeploymentManager.class);
        INotificationData data = notification(Map.of());
        when(data.getServiceArtifacts()).thenReturn(List.of());
        when(data.getResources()).thenReturn(List.of());
        when(data.getDistributionID()).thenReturn("distribution-id");
        when(downloads.downloadArtifacts(data, List.of())).thenReturn(List.of());
        when(deployments.deploy("distribution-id", List.of(), List.of())).thenReturn(false);
        EventCallback callback = new EventCallback(mock(IDistributionClient.class), deployments, downloads,
                mock(NotificationPublisher.class), tracing);
        callback.activateCallback(data);
        assertThat(finished).hasSize(1);
        assertThat(finished.get(0).error()).hasMessage(EventCallback.DEPLOY_FAILURE);
        assertThat(tracer.currentSpan()).isNull();
    }

    private INotificationData notification(Map<String, String> headers) {
        INotificationData data = mock(INotificationData.class);
        when(data.getMessageContext()).thenReturn(new NotificationMessageContext("notifications", 0, 42L, headers));
        return data;
    }
}
