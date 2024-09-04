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
package org.onap.aai.modelloader.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@AutoConfigureWebTestClient
@TestPropertySource(properties = {
  "management.endpoints.web.exposure.include=prometheus,metrics,info,health"
})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ActuatorTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  public void thatLivenessEndpointReturnsOk() {
    webTestClient.get().uri("/actuator/health")
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo("UP");
  }

  // @Test
  // public void testPrometheusEndpoint() {
  //   webTestClient.get().uri("/actuator/prometheus")
  //     .exchange()
  //     .expectStatus().isOk()
  //     .expectHeader().contentType("text/plain; charset=utf-8")
  //     .expectBody(String.class)
  //     .consumeWith(response -> {
  //         String responseBody = response.getResponseBody();
  //         assert responseBody != null;
  //         assert responseBody.contains("# HELP");
  //         assert responseBody.contains("# TYPE");
  //     });
  // }
}
