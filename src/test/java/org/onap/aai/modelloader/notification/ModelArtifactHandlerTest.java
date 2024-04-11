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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.ModelArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

import java.util.ArrayList;

@SpringBootTest(properties = { "spring.sleuth.enabled=true"})
@AutoConfigureWireMock(port = 0)
public class ModelArtifactHandlerTest {

  @Value("${wiremock.server.port}")
  private int wiremockPort;

  final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired AaiRestClient restClient;
  @Mock ModelLoaderConfig config;
  @InjectMocks ModelArtifactHandler modelArtifactHandler;

  @Autowired RestTemplate restTemplate;

  @BeforeEach
  public void setUp() {
    when(config.getAaiBaseUrl()).thenReturn("http://localhost:" + wiremockPort);
    when(config.getAaiModelUrl(any())).thenReturn("/aai/v28/service-design-and-creation/models/model/");
  }
  
  @Test
  public void thatArtifactsCanBePushed() {
    WireMock.stubFor(
      WireMock.get(WireMock.urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId"))
          .withHeader("Accept", WireMock.equalTo("application/xml"))
          .withHeader("X-TransactionId", WireMock.equalTo("someId"))
          .withHeader("X-FromAppId", WireMock.equalTo("ModelLoader"))
          .willReturn(
              WireMock.aResponse()
                  .withStatus(HttpStatus.NOT_FOUND.value())));

    WireMock.stubFor(
      WireMock.put(WireMock.urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId"))
          .withHeader("Content-Type", WireMock.equalTo("application/xml"))
          .withHeader("X-TransactionId", WireMock.equalTo("someId"))
          .withHeader("X-FromAppId", WireMock.equalTo("ModelLoader"))
          .willReturn(
              WireMock.aResponse()
                  .withStatus(HttpStatus.CREATED.value())));

    ModelArtifact modelArtifact = new ModelArtifact();
    modelArtifact.setModelInvariantId("modelInvariantId");
    List<Artifact> artifacts = List.of(modelArtifact);
    List<Artifact> completedArtifacts = new ArrayList<>();

    boolean result = modelArtifactHandler.pushArtifacts(artifacts, "someId", completedArtifacts, restClient);
    assertTrue(result);
  }
  
}
