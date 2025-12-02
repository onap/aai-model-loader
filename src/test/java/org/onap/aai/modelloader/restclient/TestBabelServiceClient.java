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
package org.onap.aai.modelloader.restclient;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.modelloader.BabelClientTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Local testing of the Babel service client.
 *
 */
@SpringBootTest
@DirtiesContext
@AutoConfigureWireMock(port = 0)
@Import(BabelClientTestConfiguration.class)
public class TestBabelServiceClient {

    @Value("${wiremock.server.port}")
    private int wiremockPort;

    @Autowired
    private BabelServiceClient client;

    @BeforeEach
    public void setup() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<BabelArtifact> artifacts = List.of(
            new BabelArtifact("art1", null, ""),
            new BabelArtifact("art2", null, ""),
            new BabelArtifact("art3", null, "")
        );

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/services/babel-service/v1/app/generateArtifacts"))
                .withHeader("X-TransactionId", WireMock.equalTo("Test-Transaction-ID-BabelClient"))
                .withHeader("X-FromAppId", WireMock.equalTo("ModelLoader"))
                .withRequestBody(WireMock.matchingJsonPath("$.artifactName", WireMock.equalTo("service-Vscpass-Test")))
                .withRequestBody(WireMock.matchingJsonPath("$.artifactVersion", WireMock.equalTo("1.0")))
                .withRequestBody(WireMock.matchingJsonPath("$.csar", WireMock.matching(".*")))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(artifacts))
                )
        );
    }

    @Test
    public void testRestClient() throws BabelServiceClientException, IOException, URISyntaxException {
        BabelRequest babelRequest = new BabelRequest();
        babelRequest.setArtifactName("service-Vscpass-Test");
        babelRequest.setCsar(Base64.getEncoder().encodeToString(
            readBytesFromFile("compressedArtifacts/service-VscpaasTest-csar.csar")
        ));
        babelRequest.setArtifactVersion("1.0");

        List<BabelArtifact> result = client.postArtifact(babelRequest, "Test-Transaction-ID-BabelClient");
        assertThat(result.size(), is(equalTo(3)));
    }

    private byte[] readBytesFromFile(String resourceFile) throws IOException, URISyntaxException {
        return Files.readAllBytes(Path.of(ClassLoader.getSystemResource(resourceFile).toURI()));
    }
}
