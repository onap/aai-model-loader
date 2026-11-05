/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom AG Intellectual Property. All rights reserved.
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
package org.onap.aai.modelloader.distribution;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.DistributionClientTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Runs a distribution through the real sdc-distribution-client: SDC's HTTP API and Babel/A&AI are stubbed with
 * WireMock, the notification and status topics live on an embedded Kafka broker.
 */
@DirtiesContext
@AutoConfigureWireMock(port = 0)
@AutoConfigureObservability
@Import({DistributionClientTestConfiguration.class, NotificationIntegrationTest.TraceConfiguration.class})
@SpringBootTest(properties = {
    "ml.distribution.connection.enabled=true",
    "management.tracing.enabled=true",
    "management.tracing.sampling.probability=1.0",
    "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration",
    "management.tracing.propagation.type=w3c,b3",
    "management.tracing.baggage.remote-fields=tracestate",
    "ml.aai.base-url=http://localhost:${wiremock.server.port}",
    "ml.aai.model-url=/aai/%s/service-design-and-creation/models/model/"
})
@EmbeddedKafka(partitions = 1, topics = {
    NotificationIntegrationTest.NOTIFICATION_TOPIC,
    NotificationIntegrationTest.STATUS_TOPIC
})
class NotificationIntegrationTest {

  // must match the topic names that the /sdc/v1/distributionKafkaData stub (kafkaBootstrap.json) hands out
  static final String NOTIFICATION_TOPIC = "SDC-DISTR-NOTIF-TOPIC-AUTO";
  static final String STATUS_TOPIC = "SDC-DISTR-STATUS-TOPIC-AUTO";

  // ml.distribution.CONSUMER_GROUP in src/test/resources/model-loader.properties
  private static final String CONSUMER_GROUP = "aai-ml-group-test";
  private static final String DISTRIBUTION_ID = "4a6a7f2e-3b1c-4f7e-9d2a-5c8e0b1f6d31";
  private static final String CSAR_URL = "/sdc/v1/catalog/services/TestSvc/2.0/artifacts/service-TestSvc-csar.csar";
  private static final String TEMPLATE_URL = "/sdc/v1/catalog/services/TestSvc/2.0/artifacts/service-TestSvc-template.yml";
  private static final String MODEL_URL =
      "/aai/v28/service-design-and-creation/models/model/3c8bc8e7-e387-46ed-8616-70e99e2206dc";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired EmbeddedKafkaBroker embeddedKafka;
  @Autowired KafkaTemplate<String, String> kafkaTemplate;

  @Autowired RecordingSpanHandler spans;

  private Consumer<String, String> statusConsumer;

  @TestConfiguration
  static class TraceConfiguration {
    @Bean
    RecordingSpanHandler recordingSpanHandler() {
      return new RecordingSpanHandler();
    }
  }

  static class RecordingSpanHandler extends SpanHandler {
    final List<MutableSpan> finished = new CopyOnWriteArrayList<>();

    @Override
    public boolean end(TraceContext context, MutableSpan span, Cause cause) {
      finished.add(new MutableSpan(span));
      return true;
    }

    List<MutableSpan> consumers() {
      return finished.stream().filter(span -> span.kind() == brave.Span.Kind.CONSUMER).toList();
    }
  }


  @BeforeEach
  void setUp() {
    spans.finished.clear();
    com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests();
    Map<String, Object> props = KafkaTestUtils.consumerProps("status-topic-reader", "false", embeddedKafka);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    statusConsumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
        .createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(statusConsumer, true, STATUS_TOPIC);
    // Resolve Kafka's lazy seek-to-end before the callback publishes any statuses.
    statusConsumer.position(new TopicPartition(STATUS_TOPIC, 0));
  }

  @AfterEach
  void tearDown() {
    statusConsumer.close();
  }

  @Test
  void thatDistributionIsDownloadedDeployedAndReported() throws Exception {
    stubSdcArtifactDownload();
    stubBabel();
    stubAaiModelCreation();
    awaitDistributionClientSubscribed();

    sendNotification("00-11111111111111111111111111111111-2222222222222222-01", DISTRIBUTION_ID);

    List<JsonNode> statuses = new ArrayList<>();
    await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
      KafkaTestUtils.getRecords(statusConsumer, Duration.ofMillis(500))
          .forEach(rec -> statuses.add(readJson(rec.value())));
      assertThat(statuses).extracting(s -> s.path("status").asText()).contains("COMPONENT_DONE_OK");
    });

    assertThat(statuses)
        .allSatisfy(s -> assertThat(s.path("distributionID").asText()).isEqualTo(DISTRIBUTION_ID))
        .allSatisfy(s -> assertThat(s.path("consumerID").asText()).isEqualTo("aai-ml-id-test"));
    assertThat(statuses)
        .filteredOn(s -> !s.path("status").asText().startsWith("COMPONENT_DONE"))
        .extracting(s -> s.path("artifactURL").asText() + " " + s.path("status").asText())
        .containsExactlyInAnyOrder(
            TEMPLATE_URL + " NOT_NOTIFIED",
            CSAR_URL + " NOTIFIED",
            CSAR_URL + " DOWNLOAD_OK",
            CSAR_URL + " DEPLOY_OK");

    verify(1, getRequestedFor(urlEqualTo(CSAR_URL)));
    verify(0, getRequestedFor(urlEqualTo(TEMPLATE_URL)));
    verify(1, putRequestedFor(urlEqualTo(MODEL_URL))
        .withHeader("X-TransactionId", equalTo(DISTRIBUTION_ID))
        .withHeader("tracestate", equalTo("vendor=distribution"))
        .withHeader("traceparent", matching("00-11111111111111111111111111111111-[0-9a-f]{16}-01")));
    verify(1, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
        urlEqualTo("/services/babel-service/v1/app/generateArtifacts"))
        .withHeader("X-TransactionId", equalTo(DISTRIBUTION_ID))
        .withHeader("tracestate", equalTo("vendor=distribution"))
        .withHeader("traceparent", matching("00-11111111111111111111111111111111-[0-9a-f]{16}-01")));
    await().untilAsserted(() -> assertThat(spans.consumers()).hasSize(1));
    MutableSpan consumer = spans.consumers().get(0);
    assertThat(consumer.traceId()).isEqualTo("11111111111111111111111111111111");
    assertThat(consumer.parentId()).isEqualTo("2222222222222222");
    assertThat(consumer.tags()).containsEntry("messaging.destination.name", NOTIFICATION_TOPIC)
        .containsEntry("messaging.kafka.partition", "0");
    assertThat(consumer.error()).isNull();
  }

  @Test
  void thatEachDispatchedRecordUsesOnlyItsOwnTraceContext() throws Exception {
    stubSdcArtifactDownload();
    stubBabel();
    stubAaiModelCreation();
    awaitDistributionClientSubscribed();

    sendNotification("00-33333333333333333333333333333333-4444444444444444-01");
    sendNotification("00-55555555555555555555555555555555-6666666666666666-01");
    sendNotification(null);
    sendNotification("invalid-trace-context");
    sendNotification(null, UUID.randomUUID().toString(), "99999999999999999999999999999999-aaaaaaaaaaaaaaaa-1",
        "vendor=distribution");
    sendNotification("invalid-trace-context", UUID.randomUUID().toString(),
        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb-cccccccccccccccc-1", "vendor=distribution");
    sendNotification("00-dddddddddddddddddddddddddddddddd-eeeeeeeeeeeeeeee-01", UUID.randomUUID().toString(),
        null, "invalid-tracestate");

    await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(spans.consumers()).hasSize(7));
    List<MutableSpan> consumers = spans.consumers();
    assertThat(consumers.get(0).traceId()).isEqualTo("33333333333333333333333333333333");
    assertThat(consumers.get(0).parentId()).isEqualTo("4444444444444444");
    assertThat(consumers.get(1).traceId()).isEqualTo("55555555555555555555555555555555");
    assertThat(consumers.get(1).parentId()).isEqualTo("6666666666666666");
    assertThat(consumers.subList(2, 4)).allSatisfy(span -> assertThat(span.parentId()).isNull());
    assertThat(consumers.get(4).traceId()).isEqualTo("99999999999999999999999999999999");
    assertThat(consumers.get(4).parentId()).isEqualTo("aaaaaaaaaaaaaaaa");
    assertThat(consumers.get(5).traceId()).isEqualTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    assertThat(consumers.get(5).parentId()).isEqualTo("cccccccccccccccc");
    assertThat(consumers.get(6).traceId()).isEqualTo("dddddddddddddddddddddddddddddddd");
    assertThat(consumers.get(6).parentId()).isEqualTo("eeeeeeeeeeeeeeee");
    assertThat(consumers).extracting(MutableSpan::traceId).doesNotHaveDuplicates();
    assertThat(consumers).allSatisfy(span -> assertThat(span.error()).isNull());
    for (MutableSpan root : consumers.subList(2, 7)) {
      verify(1, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
          urlEqualTo("/services/babel-service/v1/app/generateArtifacts"))
          .withHeader("traceparent", matching("00-" + root.traceId() + "-[0-9a-f]{16}-01"))
          .withHeader("tracestate", absent()));
    }
  }

  @Test
  void thatAFailedDistributionDoesNotLeakIntoTheNextRecord() throws Exception {
    stubFor(get(urlEqualTo(CSAR_URL)).willReturn(aResponse().withStatus(500)));
    stubBabel();
    stubAaiModelCreation();
    awaitDistributionClientSubscribed();

    sendNotification("00-77777777777777777777777777777777-8888888888888888-01");
    await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(spans.consumers()).hasSize(1));
    assertThat(spans.consumers().get(0).error()).isNotNull();

    stubSdcArtifactDownload();
    sendNotification(null);
    await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(spans.consumers()).hasSize(2));
    MutableSpan next = spans.consumers().get(1);
    assertThat(next.traceId()).isNotEqualTo("77777777777777777777777777777777");
    assertThat(next.parentId()).isNull();
    assertThat(next.error()).isNull();
  }

  private void sendNotification(String traceparent) throws Exception {
    sendNotification(traceparent, UUID.randomUUID().toString());
  }

  private void sendNotification(String traceparent, String distributionId) throws Exception {
    sendNotification(traceparent, distributionId, null, "vendor=distribution");
  }

  private void sendNotification(String traceparent, String distributionId, String b3, String traceState) throws Exception {
    ProducerRecord<String, String> record = new ProducerRecord<>(NOTIFICATION_TOPIC,
        Files.readString(Path.of("src/test/resources/messages/distribution.json"))
            .replace(DISTRIBUTION_ID, distributionId));
    if (traceparent != null) {
      record.headers().add("traceparent", traceparent.getBytes(StandardCharsets.UTF_8));
    }
    record.headers().add("tracestate", traceState.getBytes(StandardCharsets.UTF_8));
    if (b3 != null) {
      record.headers().add("b3", b3.getBytes(StandardCharsets.UTF_8));
    }
    kafkaTemplate.send(record).get();
  }

  /**
   * The client's consumer starts at the latest offset, so a notification sent before it has a position on the
   * partition would be skipped. Its first auto-commit proves that the position exists.
   */
  private void awaitDistributionClientSubscribed() {
    TopicPartition partition = new TopicPartition(NOTIFICATION_TOPIC, 0);
    try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString()))) {
      await().atMost(Duration.ofSeconds(60)).until(() -> admin.listConsumerGroupOffsets(CONSUMER_GROUP)
          .partitionsToOffsetAndMetadata().get().get(partition) != null);
    }
  }

  private void stubSdcArtifactDownload() {
    stubFor(get(urlEqualTo(CSAR_URL))
        .withHeader("X-ECOMP-RequestID", matching(".+"))
        .withHeader("X-ECOMP-InstanceID", equalTo("aai-ml-id-test"))
        .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .withBodyFile("service-TestSvc-csar.csar")));
  }

  private void stubBabel() {
    stubFor(post(urlEqualTo("/services/babel-service/v1/app/generateArtifacts"))
        .withHeader("X-TransactionId", matching("[0-9a-f-]{36}"))
        .withRequestBody(matchingJsonPath("$.artifactName", equalTo("service-TestSvc-csar.csar")))
        .withRequestBody(matchingJsonPath("$.artifactVersion", equalTo("2.0")))
        .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("service-TestSvc-csar-babel-response.json")));
  }

  private void stubAaiModelCreation() {
    stubFor(get(urlEqualTo(MODEL_URL))
        .withHeader("X-TransactionId", matching("[0-9a-f-]{36}"))
        .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
    stubFor(put(urlEqualTo(MODEL_URL))
        .withHeader("X-TransactionId", matching("[0-9a-f-]{36}"))
        .withHeader("Content-Type", equalTo(MediaType.APPLICATION_XML_VALUE))
        .willReturn(aResponse().withStatus(HttpStatus.CREATED.value())));
  }

  private JsonNode readJson(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (Exception e) {
      throw new IllegalStateException("Status message is not JSON: " + value, e);
    }
  }
}
