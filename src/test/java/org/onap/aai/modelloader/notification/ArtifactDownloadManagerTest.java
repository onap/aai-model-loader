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
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.DistributionClientTestConfiguration;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.service.ArtifactInfoImpl;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import com.github.tomakehurst.wiremock.client.WireMock;

@DirtiesContext
@AutoConfigureWireMock(port = 0)
@SpringBootTest(properties = { "ml.distribution.connection.enabled=true" })
@Import(DistributionClientTestConfiguration.class)
public class ArtifactDownloadManagerTest {

  @Autowired ArtifactDownloadManager artifactDownloadManager;

  @BeforeEach
  // TODO: These stubs are currently not being registered before spring application startup
  public void setup() {
    stubFor(get(urlEqualTo("/sdc/v1/artifactTypes"))
      .withHeader("X-ECOMP-RequestID", WireMock.matching(".+"))
      .withHeader("X-ECOMP-InstanceID", equalTo("aai-ml-id-test"))
        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("artifactTypes.json")));
    
    stubFor(get(urlEqualTo("/sdc/v1/distributionKafkaData"))
      .withHeader("X-ECOMP-RequestID", WireMock.matching(".+"))
      .withHeader("X-ECOMP-InstanceID", equalTo("aai-ml-id-test"))
        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("artifactTypes.json")));
  }

  @Test
  @Disabled
  public void downloadArtifacts() {

    // requestHeaders.put("X-ECOMP-RequestID", requestId);
    // requestHeaders.put("X-ECOMP-InstanceID", this.configuration.getConsumerID());
    // requestHeaders.put("Accept", ContentType.APPLICATION_OCTET_STREAM.toString());

    NotificationDataImpl notificationData = new NotificationDataImpl();
    notificationData.setDistributionID("distributionID");
    ArtifactInfoImpl artifactInfo = new ArtifactInfoImpl();
    artifactInfo.setArtifactVersion("1.0");
    List<IArtifactInfo> artifacts = new ArrayList<>();
    artifacts.add(artifactInfo);
    List<Artifact> modelArtifacts = new ArrayList<>(); // processed artifacts will be written to this list
    boolean result = artifactDownloadManager.downloadArtifacts(notificationData, artifacts, modelArtifacts, null);
    assertTrue(result);
  }

}
