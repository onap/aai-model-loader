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
package org.onap.aai.modelloader.restclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import com.github.tomakehurst.wiremock.client.WireMock;

@SpringBootTest(properties = {
  "spring.sleuth.enabled=true",
  "spring.zipkin.baseUrl=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWireMock(port = 0)
public class TracingTest {

  @Value("${wiremock.server.port}")
  private int wiremockPort;

  @Autowired RestTemplate restTemplate;
  
  @Test
  public void thatArtifactsCanBePushed() {
    WireMock.stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/v2/spans"))
          .willReturn(
              WireMock.aResponse()
                  .withStatus(HttpStatus.OK.value())));

    WireMock.stubFor(
      WireMock.get(WireMock.urlEqualTo("/"))
          .withHeader("X-B3-TraceId", WireMock.matching(".*"))
          .willReturn(
              WireMock.aResponse()
                  .withStatus(HttpStatus.OK.value())));


    String response = restTemplate.getForObject("http://localhost:" + wiremockPort + "/", String.class);
  }
  
}
