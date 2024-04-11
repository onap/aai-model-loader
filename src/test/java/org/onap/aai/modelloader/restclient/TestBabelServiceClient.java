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
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.service.HttpsBabelServiceClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

/**
 * Local testing of the Babel service client.
 *
 */
@SpringBootTest
@AutoConfigureWireMock(port = 0)
public class TestBabelServiceClient {

    @Value("${wiremock.server.port}")
    private int wiremockPort;

    @BeforeAll
    public static void setup() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        List<BabelArtifact> artifacts = List.of(
            new BabelArtifact("art1", null, ""),
            new BabelArtifact("art2", null, ""),
            new BabelArtifact("art3", null, ""));
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/generate"))
                .withHeader("X-TransactionId", WireMock.equalTo("Test-Transaction-ID-BabelClient"))
                .withHeader("X-FromAppId", WireMock.equalTo("ModelLoader"))
                .withRequestBody(WireMock.matchingJsonPath("$.artifactName", WireMock.equalTo("service-Vscpass-Test")))
                .withRequestBody(WireMock.matchingJsonPath("$.artifactVersion", WireMock.equalTo("1.0")))
                .withRequestBody(WireMock.matchingJsonPath("$.csar", WireMock.matching(".*")))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(artifacts))));
    }

    @Test
    public void testRestClient() throws BabelServiceClientException, IOException, URISyntaxException {
        String url = "http://localhost:" + wiremockPort;
        Properties configProperties = new Properties();
        configProperties.put("ml.babel.KEYSTORE_PASSWORD", "OBF:1vn21ugu1saj1v9i1v941sar1ugw1vo0");
        configProperties.put("ml.babel.KEYSTORE_FILE", "src/test/resources/auth/aai-client-dummy.p12");
        configProperties.put("ml.babel.TRUSTSTORE_PASSWORD", "OBF:1vn21ugu1saj1v9i1v941sar1ugw1vo0");
        // In a real deployment this would be a different file (to the client keystore)
        configProperties.put("ml.babel.TRUSTSTORE_FILE", "src/test/resources/auth/aai-client-dummy.p12");
        configProperties.put("ml.babel.BASE_URL", url);
        configProperties.put("ml.babel.GENERATE_ARTIFACTS_URL", "/generate");
        configProperties.put("ml.aai.RESTCLIENT_CONNECT_TIMEOUT", "12000");
        configProperties.put("ml.aai.RESTCLIENT_READ_TIMEOUT", "12000");
        BabelServiceClient client =
                new HttpsBabelServiceClientFactory().create(new ModelLoaderConfig(configProperties, "."));
        List<BabelArtifact> result =
                client.postArtifact(readBytesFromFile("compressedArtifacts/service-VscpaasTest-csar.csar"),
                        "service-Vscpass-Test", "1.0", "Test-Transaction-ID-BabelClient");
        assertThat(result.size(), is(equalTo(3)));
    }

    @Test
    public void testRestClientHttp() throws BabelServiceClientException, IOException, URISyntaxException {
        String url = "http://localhost:" + wiremockPort;
        Properties configProperties = new Properties();
        configProperties.put("ml.babel.USE_HTTPS", "false");
        configProperties.put("ml.babel.BASE_URL", url);
        configProperties.put("ml.babel.GENERATE_ARTIFACTS_URL", "/generate");
        configProperties.put("ml.aai.RESTCLIENT_CONNECT_TIMEOUT", "3000");
        configProperties.put("ml.aai.RESTCLIENT_READ_TIMEOUT", "3000");
        BabelServiceClient client =
                new HttpsBabelServiceClientFactory().create(new ModelLoaderConfig(configProperties, "."));
        List<BabelArtifact> result =
                client.postArtifact(readBytesFromFile("compressedArtifacts/service-VscpaasTest-csar.csar"),
                        "service-Vscpass-Test", "1.0", "Test-Transaction-ID-BabelClient");
        assertThat(result.size(), is(equalTo(3)));
    }


    private byte[] readBytesFromFile(String resourceFile) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(resourceFile).toURI()));
    }
}
