/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.aai.modelloader.entity.catalog;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.restclient.AaiRestClient.MimeType;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.jersey.api.client.ClientResponse;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VnfCatalogArtifactHandler.class, ClientResponse.class, AaiRestClient.class })
public class VnfCatalogArtifactHandlerTest {

  protected static String CONFIG_FILE = "model-loader.properties";

  @Test
  public void testWithMocks() throws Exception {

    Properties configProperties = new Properties();
    try {
      configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
    } catch (IOException e) {
      fail();
    }
    ModelLoaderConfig config = new ModelLoaderConfig(configProperties, null);

    ClientResponse mockGetResp = PowerMockito.mock(ClientResponse.class);
    PowerMockito.when(mockGetResp.getStatus()).thenReturn(200).thenReturn(200).thenReturn(404)
        .thenReturn(404).thenReturn(200); // only second two will be PUT
    ClientResponse mockPutResp = PowerMockito.mock(ClientResponse.class);
    PowerMockito.when(mockPutResp.getStatus()).thenReturn(201);

    AaiRestClient mockRestClient = PowerMockito.mock(AaiRestClient.class);
    PowerMockito.whenNew(AaiRestClient.class).withAnyArguments().thenReturn(mockRestClient);
    PowerMockito.when(mockRestClient.getResource(Mockito.anyString(), Mockito.anyString(),
        Mockito.any(MimeType.class))).thenReturn(mockGetResp);
    PowerMockito.when(mockRestClient.putResource(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.any(MimeType.class))).thenReturn(mockPutResp);

    VnfCatalogArtifactHandler vnfCAH = new VnfCatalogArtifactHandler(config);

    String examplePath = "src/test/resources/vnfcatalogexample.xml";

    byte[] encoded = Files.readAllBytes(Paths.get(examplePath));
    String payload = new String(encoded, "utf-8");

    VnfCatalogArtifact artifact = new VnfCatalogArtifact(payload);
    List<Artifact> artifacts = new ArrayList<Artifact>();
    artifacts.add(artifact);

    String distributionID = "test";

    assertTrue(vnfCAH.pushArtifacts(artifacts, distributionID));
    // times(2) bc with above get returns should only get to this part twice
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    Mockito.verify(mockRestClient, Mockito.times(2)).putResource(Mockito.anyString(),
        argument.capture(), Mockito.anyString(), Mockito.any(MimeType.class));
    assertTrue(argument.getAllValues().get(0).contains("5.2.5"));
    assertTrue(argument.getAllValues().get(1).contains("5.2.4"));
  }
}
