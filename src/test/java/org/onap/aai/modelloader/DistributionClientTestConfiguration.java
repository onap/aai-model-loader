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
package org.onap.aai.modelloader;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;

@TestConfiguration
public class DistributionClientTestConfiguration {

  @Value("${CONFIG_HOME}")
  private String configDir;

  @Value("${wiremock.server.port}")
  private int wiremockPort;

  @Primary
  @Bean(name = "testProperties")
  public Properties configProperties() throws IOException {
    // Load model loader system configuration
    InputStream configInputStream = Files.newInputStream(Paths.get(configDir, "model-loader.properties"));
    Properties configProperties = new Properties();
    configProperties.load(configInputStream);

    setOverrides(configProperties);

    return configProperties;
  }

  private void setOverrides(Properties configProperties) {
    configProperties.setProperty("ml.distribution.ASDC_ADDRESS", "localhost:" + wiremockPort);
    configProperties.setProperty("ml.babel.BASE_URL", "http://localhost:" + wiremockPort);
  }

  @EventListener(ApplicationStartedEvent.class)
  public void mockSdcInit() {
    stubFor(get(urlEqualTo("/sdc/v1/artifactTypes"))
        .withHeader("X-ECOMP-RequestID", matching(".+"))
        .withHeader("X-ECOMP-InstanceID", equalTo("aai-ml-id-test"))
        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("artifactTypes.json")));

    stubFor(get(urlEqualTo("/sdc/v1/distributionKafkaData"))
        .withHeader("X-ECOMP-RequestID", matching(".+"))
        .withHeader("X-ECOMP-InstanceID", equalTo("aai-ml-id-test"))
        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBodyFile("kafkaBootstrap.json")));
  }
}