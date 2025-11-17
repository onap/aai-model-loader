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

package org.onap.aai.modelloader.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.DistributionClientTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import lombok.SneakyThrows;

@DirtiesContext
@AutoConfigureWireMock(port = 0)
@Import(DistributionClientTestConfiguration.class)
@SpringBootTest(properties = { "ml.distribution.connection.enabled=true" })
@EmbeddedKafka(partitions = 1, ports = 9092, topics = {"${topics.distribution.notification}"})
public class DistributionClientIntegrationTest {

  @Value("${topics.distribution.notification}")
  String topic;

  @Autowired
  KafkaTemplate<String,String> kafkaTemplate;

  @MockBean
  EventCallback eventCallback;

  private CountDownLatch latch;

  @BeforeEach
  public void setup() {
    latch = new CountDownLatch(1);

    doAnswer(invocation -> {
      latch.countDown();
      return null;
    }).when(eventCallback).activateCallback(any());
  }

  @Test
  @SneakyThrows
  void thatCallbackIsCalled() {
    Thread.sleep(10000);
    String distributionJson = "src/test/resources/messages/distribution.json";
    String message = Files.readString(Path.of(distributionJson));

    kafkaTemplate.send(topic, message);

    latch.await(10, TimeUnit.SECONDS);
    verify(eventCallback).activateCallback(any());
  }

}
