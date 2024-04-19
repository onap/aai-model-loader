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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.ArrayList;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
public class ModelArtifactHandlerTest {

  @Value("${wiremock.server.port}")
  private int wiremockPort;

  final ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  ModelLoaderConfig config;
  @InjectMocks
  ModelArtifactHandler modelArtifactHandler;

  @Autowired
  AaiRestClient restClient;

  @BeforeEach
  public void setUp() {
    when(config.getAaiBaseUrl()).thenReturn("http://localhost:" + wiremockPort);
    when(config.getAaiModelUrl(any())).thenReturn("/aai/v28/service-design-and-creation/models/model/");
  }

  @Test
  public void thatArtifactsCanBeCreated() {
    WireMock.stubFor(
        WireMock.get(urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId"))
            .withHeader("Accept", equalTo("application/xml"))
            .withHeader("X-TransactionId", equalTo("someId"))
            .withHeader("X-FromAppId", equalTo("ModelLoader"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.NOT_FOUND.value())));

    WireMock.stubFor(
        WireMock.put(urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId"))
            .withHeader("Accept", equalTo(MediaType.APPLICATION_XML_VALUE))
            .withHeader("Content-Type", equalTo(MediaType.APPLICATION_XML_VALUE))
            .withHeader("X-TransactionId", equalTo("someId"))
            .withHeader("X-FromAppId", equalTo("ModelLoader"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.CREATED.value())));

    ModelArtifact modelArtifact = new ModelArtifact();
    modelArtifact.setModelInvariantId("modelInvariantId");
    List<Artifact> artifacts = List.of(modelArtifact);
    List<Artifact> completedArtifacts = new ArrayList<>();

    boolean result = modelArtifactHandler.pushArtifacts(artifacts,
        "someId", completedArtifacts, restClient);
    assertTrue(result);
    WireMock.verify(
        WireMock.putRequestedFor(urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId")));
  }

  @Test
  public void thatArtifactsCanBeUpdated() {
    // Checks if model exists in resources service
    WireMock.stubFor(
        WireMock.get(urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId"))
            .withHeader("Accept", equalTo("application/xml"))
            .withHeader("X-TransactionId", equalTo("distributionId"))
            .withHeader("X-FromAppId", equalTo("ModelLoader"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.OK.value())));

    // Checks if specific version of model exists in aai-resources
    WireMock.stubFor(
        WireMock.get(urlEqualTo(
            "/aai/v28/service-design-and-creation/models/model/modelInvariantId/model-vers/model-ver/modelVersionId"))
            .withHeader("Accept", equalTo("application/xml"))
            .withHeader("X-TransactionId", equalTo("distributionId"))
            .withHeader("X-FromAppId", equalTo("ModelLoader"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.NOT_FOUND.value())));

    WireMock.stubFor(
        WireMock.put(urlEqualTo(
            "/aai/v28/service-design-and-creation/models/model/modelInvariantId/model-vers/model-ver/modelVersionId"))
            .withHeader("Accept", equalTo(MediaType.APPLICATION_XML_VALUE))
            .withHeader("Content-Type", equalTo(MediaType.APPLICATION_XML_VALUE))
            .withHeader("X-TransactionId", equalTo("distributionId"))
            .withHeader("X-FromAppId", equalTo("ModelLoader"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(HttpStatus.CREATED.value())));

    ModelArtifact modelArtifact = new ModelArtifact();
    modelArtifact.setModelInvariantId("modelInvariantId");
    modelArtifact.setModelVerId("modelVersionId");
    Node node = Mockito.mock(Node.class);
    modelArtifact.setModelVer(node);
    List<Artifact> artifacts = List.of(modelArtifact);
    List<Artifact> completedArtifacts = new ArrayList<>();

    boolean result = modelArtifactHandler.pushArtifacts(artifacts,
        "distributionId", completedArtifacts, restClient);
    verify(WireMock.putRequestedFor(urlEqualTo(
        "/aai/v28/service-design-and-creation/models/model/modelInvariantId/model-vers/model-ver/modelVersionId")));
    assertTrue(result);
  }

  @Test
  public void thatModelCanBeRolledBack() {
    stubFor(WireMock.get(urlEqualTo("/aai/v28/service-design-and-creation/models/model/3a40ab73-6694-4e75-bb6d-9a4a86ce35b3"))
        .withHeader("X-FromAppId", equalTo("ModelLoader"))
        .withHeader("X-TransactionId", equalTo("distributionId"))
        .willReturn(aResponse()
            .withBodyFile("modelResponse.xml")));

    stubFor(WireMock.delete(urlEqualTo("/aai/v28/service-design-and-creation/models/model/3a40ab73-6694-4e75-bb6d-9a4a86ce35b3?resource-version=1710523260974"))
        .withHeader("X-FromAppId", equalTo("ModelLoader"))
        .withHeader("X-TransactionId", equalTo("distributionId"))
        .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

    ModelArtifact modelArtifact = new ModelArtifact();
    modelArtifact.setModelInvariantId("3a40ab73-6694-4e75-bb6d-9a4a86ce35b3");
    modelArtifact.setModelVerId("modelVersionId");
    Node node = Mockito.mock(Node.class);
    modelArtifact.setModelVer(node);
    List<Artifact> completedArtifacts = new ArrayList<>();
    completedArtifacts.add(modelArtifact);

    mockModelCreation(modelArtifact);

    modelArtifactHandler.rollback(completedArtifacts, "distributionId", restClient);

    verify(
        deleteRequestedFor(
            urlEqualTo("/aai/v28/service-design-and-creation/models/model/3a40ab73-6694-4e75-bb6d-9a4a86ce35b3?resource-version=1710523260974")));
  }

  /**
   * To test the rollback of the full model, the ModelArtifact must have the
   * firstVersionOfModel = true state.
   * This flag is set during the model creation and thus needs to run before
   * testing this particular aspect.
   *
   * @param modelArtifact
   */
  private void mockModelCreation(ModelArtifact modelArtifact) {
    ResponseEntity createdResult = Mockito.mock(ResponseEntity.class);
    when(createdResult.getStatusCode()).thenReturn(HttpStatus.CREATED);
    ResponseEntity notFoundResult = Mockito.mock(ResponseEntity.class);
    when(notFoundResult.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
    AaiRestClient aaiClient = Mockito.mock(AaiRestClient.class);
    when(aaiClient.putResource(any(), any(), any(), any(), any())).thenReturn(createdResult);
    when(aaiClient.getResource(any(), any(), any(), any())).thenReturn(notFoundResult);
    modelArtifact.push(aaiClient, config, null, new ArrayList<>());
  }

  @Test
  public void thatModelVersionCanBeRolledBack() {

    // "http://localhost:10594/aai/v28/service-design-and-creation/models/model/modelInvariantId/model-vers/model-ver/modelVersionId"

    ModelArtifact modelArtifact = new ModelArtifact();
    modelArtifact.setModelInvariantId("modelInvariantId");
    modelArtifact.setModelVerId("modelVersionId");
    Node node = Mockito.mock(Node.class);
    modelArtifact.setModelVer(node);
    List<Artifact> completedArtifacts = new ArrayList<>();
    completedArtifacts.add(modelArtifact);

    modelArtifactHandler.rollback(completedArtifacts, "distributionId", restClient);

  }
}