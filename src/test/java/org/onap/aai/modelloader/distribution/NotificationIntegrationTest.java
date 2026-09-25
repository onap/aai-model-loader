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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.DistributionClientTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Runs a distribution through the real sdc-distribution-client: SDC's HTTP API and Babel/A&AI are stubbed with
 * WireMock, the notification and status topics live on an embedded Kafka broker.
 */
@DirtiesContext
@AutoConfigureWireMock(port = 0)
@Import(DistributionClientTestConfiguration.class)
@SpringBootTest(properties = {
    "ml.distribution.connection.enabled=true",
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

  private Consumer<String, String> statusConsumer;

  @BeforeEach
  void setUp() {
    Map<String, Object> props = KafkaTestUtils.consumerProps("status-topic-reader", "false", embeddedKafka);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    statusConsumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
        .createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(statusConsumer, STATUS_TOPIC);
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

    kafkaTemplate.send(NOTIFICATION_TOPIC, Files.readString(Path.of("src/test/resources/messages/distribution.json")));

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
    verify(1, putRequestedFor(urlEqualTo(MODEL_URL)));
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
        .withHeader("X-TransactionId", equalTo(DISTRIBUTION_ID))
        .withRequestBody(matchingJsonPath("$.artifactName", equalTo("service-TestSvc-csar.csar")))
        .withRequestBody(matchingJsonPath("$.artifactVersion", equalTo("2.0")))
        .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("service-TestSvc-csar-babel-response.json")));
  }

  private void stubAaiModelCreation() {
    stubFor(get(urlEqualTo(MODEL_URL))
        .withHeader("X-TransactionId", equalTo(DISTRIBUTION_ID))
        .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));
    stubFor(put(urlEqualTo(MODEL_URL))
        .withHeader("X-TransactionId", equalTo(DISTRIBUTION_ID))
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
