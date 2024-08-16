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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.DistributionClientTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@AutoConfigureWireMock(port = 0)
@SpringBootTest(properties = { "ml.distribution.connection.enabled=true"})
@EmbeddedKafka(partitions = 1, ports = 9092, topics = {"${topics.distribution.notification}"})
@Import(DistributionClientTestConfiguration.class)
public class NotificationIntegrationTest {

    @Autowired EventCallbackAspect eventCallbackAspect;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${topics.distribution.notification}")
    private String topic;

    @Test
    // @Disabled("This test is not yet implemented")
    public void thatActivateCallbackIsCalled()
      throws Exception {
        String distributionEvent = new String(Files.readAllBytes(Paths.get(new ClassPathResource("__files/distributionEvent.json").getURI())));

        kafkaTemplate.send(topic, distributionEvent);

        // TODO: mock distribution client requests to /sdc/v1/artifactTypes
        // TODO: mock distribution client requests to /sdc/v1/distributionKafkaData

        // aspect will wait for EventCallback.activateCallback to be called
        eventCallbackAspect.setCountDownLatch(new CountDownLatch(1));
        boolean callbackCalled = eventCallbackAspect.getCountDownLatch().await(10, TimeUnit.SECONDS);
        assertTrue(callbackCalled);
    }
}
