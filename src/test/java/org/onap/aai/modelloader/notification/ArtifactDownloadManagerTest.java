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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.DistributionClientTestConfiguration;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.service.ArtifactInfoImpl;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@AutoConfigureWireMock(port = 0)
@EmbeddedKafka(partitions = 1, ports = 9092, topics = {"${topics.distribution.notification}"})
@SpringBootTest(properties = { "ml.distribution.connection.enabled=true" })
@Import(DistributionClientTestConfiguration.class)
public class ArtifactDownloadManagerTest {

  @Autowired ArtifactDownloadManager artifactDownloadManager;

  @Test
  public void downloadArtifacts() throws Exception {
    NotificationDataImpl notificationData = new NotificationDataImpl();
    notificationData.setDistributionID("distributionID");
    notificationData.setServiceVersion("2.0");

    stubFor(get(urlEqualTo("/sdc/v1/catalog/services/DemovlbCds/1.0/artifacts/service-TestSvc-csar.csar"))
        .withHeader("Accept", equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE))
        .withHeader("X-ECOMP-RequestID", matching(".+"))
        .withHeader("X-ECOMP-InstanceID", equalTo("aai-ml-id-test"))
        .willReturn(aResponse()
          .withHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
          .withBodyFile("service-TestSvc-csar.csar")));
    
    stubFor(
        post(urlEqualTo("/services/babel-service/v1/app/generateArtifacts"))
            .withHeader("X-TransactionId", equalTo("distributionID"))
            .withHeader("X-FromAppId", equalTo("ModelLoader"))
            .withHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON_VALUE))
            .withRequestBody(matchingJsonPath("$.artifactName", equalTo("service-TestSvc-csar.csar")))
            .withRequestBody(matchingJsonPath("$.artifactVersion", equalTo("2.0")))
            .withRequestBody(matchingJsonPath("$.csar", matching(".*")))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBodyFile("service-TestSvc-csar-babel-response.json")));

    ArtifactInfoImpl artifactInfo = new ArtifactInfoImpl();
    artifactInfo.setArtifactName("service-TestSvc-csar.csar");
    artifactInfo.setArtifactVersion("1.0");
    artifactInfo.setArtifactURL("/sdc/v1/catalog/services/DemovlbCds/1.0/artifacts/service-TestSvc-csar.csar");
    artifactInfo.setArtifactType("TOSCA_CSAR");
    artifactInfo.setArtifactChecksum("ZmI5NzQ1MWViZGFkMjRjZWEwNTQzY2U0OWQwYjlmYjQ=");
    artifactInfo.setArtifactUUID("f6f907f1-3f45-4fb4-8cbe-15a4c6ee16db");
    List<IArtifactInfo> artifacts = new ArrayList<>();
    artifacts.add(artifactInfo);
    List<Artifact> result = artifactDownloadManager.downloadArtifacts(notificationData, artifacts);
    
    assertEquals(1, result.size());
    ModelArtifact modelArtifact = (ModelArtifact) result.get(0);
    assertEquals(ArtifactType.MODEL, modelArtifact.getType());
    assertEquals("3c8bc8e7-e387-46ed-8616-70e99e2206dc", modelArtifact.getModelInvariantId());
    assertEquals("http://org.onap.aai.inventory/v28", modelArtifact.getModelNamespace());
    assertEquals("v28", modelArtifact.getModelNamespaceVersion());
    assertEquals("71f47717-b100-4eac-940b-7d4e86a4cbb7", modelArtifact.getModelVerId());
    assertEquals(Set.of("82194af1-3c2c-485a-8f44-420e22a9eaa4|46b92144-923a-4d20-b85a-3cbd847668a9"), modelArtifact.getDependentModelIds());
  }

}
