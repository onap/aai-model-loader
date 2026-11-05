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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.NotificationMessageContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class NotificationTracing {
    private static final TextMapGetter<Map<String, String>> HEADER_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };
    private final Tracer tracer;
    private final Propagator propagator;
    private final ObservationRegistry observationRegistry;

    public NotificationTracing(Optional<Tracer> tracer, Optional<Propagator> propagator,
            ObservationRegistry observationRegistry) {
        this.tracer = tracer.orElse(Tracer.NOOP);
        this.propagator = propagator.orElse(Propagator.NOOP);
        this.observationRegistry = observationRegistry;
    }

    public void trace(INotificationData data, Consumer<Span> callback) {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        Observation.Scope previousObservationScope = observationRegistry.getCurrentObservationScope();
        // A polled message must not inherit the scheduler's observation or a previous message's trace.
        observationRegistry.setCurrentObservationScope(null);
        try (CurrentTraceContext.Scope ignored = tracer.currentTraceContext().newScope(null)) {
            NotificationMessageContext context = data.getMessageContext();
            if (context == null) {
                context = NotificationMessageContext.EMPTY;
            }
            Span.Builder builder = propagator.extract(validatedHeaders(context.getHeaders()), Map::get)
                    .name("sdc.distribution process").kind(Span.Kind.CONSUMER)
                    .tag("messaging.system", "kafka").tag("messaging.operation", "process");
            if (context.getTopic() != null) {
                builder.tag("messaging.destination.name", context.getTopic())
                        .tag("messaging.kafka.partition", context.getPartition())
                        .tag("messaging.kafka.message.offset", context.getOffset());
            }
            MdcContext.initialize(data.getDistributionID(), "ModelLoader", "", "Event-Bus", "");
            Span span = builder.start();
            try (Tracer.SpanInScope spanScope = tracer.withSpan(span)) {
                callback.accept(span);
            } catch (RuntimeException | Error e) {
                span.error(e);
                throw e;
            } finally {
                span.end();
            }
        } finally {
            observationRegistry.setCurrentObservationScope(previousObservationScope);
            if (previousMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousMdc);
            }
        }
    }

    private Map<String, String> validatedHeaders(Map<String, String> headers) {
        Map<String, String> validated = new HashMap<>(headers);
        validated.remove("tracestate");
        // The Brave bridge treats tracestate as opaque baggage, including with a B3 fallback parent.
        W3CTraceContextPropagator w3c = W3CTraceContextPropagator.getInstance();
        Context parent = w3c.extract(Context.root(), headers, HEADER_GETTER);
        Map<String, String> w3cHeaders = new HashMap<>();
        w3c.inject(parent, w3cHeaders, Map::put);
        String traceState = w3cHeaders.get("tracestate");
        if (traceState != null) {
            validated.put("tracestate", traceState);
        }
        return validated;
    }

}
