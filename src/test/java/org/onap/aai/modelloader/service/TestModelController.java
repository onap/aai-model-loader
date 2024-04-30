/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
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
package org.onap.aai.modelloader.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.aai.modelloader.babel.BabelArtifactService;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.extraction.VnfCatalogExtractor;
import org.onap.aai.modelloader.notification.ArtifactDownloadManager;
import org.onap.aai.modelloader.notification.BabelArtifactConverter;
import org.onap.aai.modelloader.notification.NotificationPublisher;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.restclient.BabelServiceClientException;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.onap.sdc.api.IDistributionClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for the ModelLoaderService class.
 *
 */
@SpringBootTest
@TestPropertySource(properties = {"CONFIG_HOME=src/test/resources",})
public class TestModelController {

    @Autowired IDistributionClient iDistributionClient;
    @Autowired ModelLoaderConfig modelLoaderConfig;
    @Autowired ArtifactDeploymentManager artifactDeploymentManager;
    @Autowired BabelArtifactConverter babelArtifactConverter;
    @Autowired NotificationPublisher notificationPublisher;
    @Autowired VnfCatalogExtractor vnfCatalogExtractor;

    @Mock BabelServiceClient babelServiceClient;
    @InjectMocks BabelArtifactService babelArtifactService;
    
    private ModelController modelController;

    @BeforeEach
    public void init() throws BabelServiceClientException {
        when(babelServiceClient.postArtifact(any(), any())).thenReturn(Collections.emptyList());
        ArtifactDownloadManager artifactDownloadManager = new ArtifactDownloadManager(iDistributionClient, notificationPublisher, vnfCatalogExtractor, babelArtifactService);
        this.modelController = new ModelController(iDistributionClient, modelLoaderConfig, artifactDeploymentManager, artifactDownloadManager);
    }

    @Test
    public void testLoadModel() {
        ResponseEntity<String> response = modelController.loadModel("");
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testSaveModel() {
        ResponseEntity<String> response = modelController.saveModel("", "");
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testIngestModel() throws IOException {
        byte[] csarPayload = new ArtifactTestUtils().loadResource("compressedArtifacts/service-VscpaasTest-csar.csar");
        ResponseEntity<String> response = modelController.ingestModel("model-name", "", Base64.getEncoder().encodeToString(csarPayload));
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testIngestModelMissingName() throws IOException {
        byte[] csarPayload = new ArtifactTestUtils().loadResource("compressedArtifacts/service-VscpaasTest-csar.csar");
        ResponseEntity<String> response = modelController.ingestModel("", "", Base64.getEncoder().encodeToString(csarPayload));
        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

}
