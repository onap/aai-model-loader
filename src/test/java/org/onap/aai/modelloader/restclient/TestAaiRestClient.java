/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
 * Copyright © 2023 Deutsche Telekom AG.
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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Testcontainers
@SpringBootTest
public class TestAaiRestClient {

    private static final String MODEL_FILE = "src/test/resources/models/l3-network-widget.xml";
    private static final String modelInvariantId = "3d560d81-57d0-438b-a2a1-5334dba0651a";
    private static final String modelVersionId = "9111f20f-e680-4001-b83f-19a2fc23bfc1";
    private ModelLoaderConfig config;
    @Autowired RestTemplate restTemplate;
    private int containerPort;
    AaiRestClient aaiClient;
    String modelUrl;

    private final String image = "nexus3.onap.org:10001/onap/aai-resources:1.12.3";

    @Container
    public GenericContainer<?> resourcesContainer = new GenericContainer<>(DockerImageName.parse(image))
            .withExposedPorts(8447)
            .waitingFor(Wait.forLogMessage(".*Resources MicroService Started.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    @BeforeEach
    public void setup() {
        containerPort = resourcesContainer.getFirstMappedPort();
        String baseUrl = "https://localhost:" + String.valueOf(containerPort);
        modelUrl = baseUrl + "/aai/v25/service-design-and-creation/models/model/" + modelInvariantId;
        Properties props = new Properties();
        props.setProperty("ml.distribution.ARTIFACT_TYPES", "MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG");
        props.setProperty("ml.aai.BASE_URL", baseUrl);
        props.setProperty("ml.aai.MODEL_URL", "aai/v*/service-design-and-creation/models/model/");
        props.setProperty("ml.aai.AUTH_USER", "ModelLoader");
        props.setProperty("ml.aai.AUTH_PASSWORD", "OBF:1qvu1v2h1sov1sar1wfw1j7j1wg21saj1sov1v1x1qxw");

        config = new ModelLoaderConfig(props, ".");
        aaiClient = new AaiRestClient(config, restTemplate);
    }

    @Test
    public void thatModelCanBeCreated() throws Exception {
        // Build the model artifact
        String payload = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));
        ModelArtifact model = new ModelArtifact();
        model.setModelInvariantId(modelInvariantId);
        model.setModelVerId(modelVersionId);
        model.setPayload(payload);
        model.setModelNamespace("http://org.onap.aai.inventory/v25");

        // GET model
        HttpClientErrorException thrown = assertThrows(HttpClientErrorException.class,
            () -> {
                aaiClient.getResource(modelUrl, "example-trans-id-0", MediaType.APPLICATION_XML, String.class);
            });
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());

        ResponseEntity<?> opResult;
        // PUT the model
        opResult = aaiClient.putResource(modelUrl, model.getPayload(), "example-trans-id-1",
                MediaType.APPLICATION_XML, String.class);
        assertEquals(opResult.getStatusCode(), HttpStatus.CREATED);

        // DELETE the model
        opResult = aaiClient.getAndDeleteResource(modelUrl, "example-trans-id-3");
        assertEquals(opResult.getStatusCode(), HttpStatus.NO_CONTENT);
    }
}
