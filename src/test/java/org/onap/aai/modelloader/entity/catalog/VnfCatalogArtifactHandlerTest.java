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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.restclient.client.OperationResult;

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
        config.setModelVersion("11");

        AaiRestClient mockRestClient = mock(AaiRestClient.class);

        // GET operation
        OperationResult mockGetResp = mock(OperationResult.class);

        // @formatter:off
        when(mockGetResp.getResultCode())
                .thenReturn(Response.Status.OK.getStatusCode())
                .thenReturn(Response.Status.NOT_FOUND.getStatusCode())
                .thenReturn(Response.Status.NOT_FOUND.getStatusCode())
                .thenReturn(Response.Status.OK.getStatusCode());
        // @formatter:on
        when(mockRestClient.getResource(Mockito.anyString(), Mockito.anyString(), Mockito.any(MediaType.class)))
                .thenReturn(mockGetResp);

        // PUT operation
        OperationResult mockPutResp = mock(OperationResult.class);

        when(mockPutResp.getResultCode()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(mockRestClient.putResource(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.any(MediaType.class))).thenReturn(mockPutResp);

        // Example VNF Catalog with
        VnfCatalogArtifactHandler vnfCAH = new VnfCatalogArtifactHandler(config);
        String examplePath = "src/test/resources/imagedataexample.json";
        byte[] encoded = Files.readAllBytes(Paths.get(examplePath));
        List<Artifact> artifacts = new ArrayList<Artifact>();
        artifacts.add(new VnfCatalogArtifact(new String(encoded, "utf-8")));

        assertTrue(vnfCAH.pushArtifacts(artifacts, "test", new ArrayList<Artifact>(), mockRestClient));

        // Only two of the VNF images should be pushed
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        AaiRestClient r = Mockito.verify(mockRestClient, Mockito.times(2));
        r.putResource(Mockito.anyString(), argument.capture(), Mockito.anyString(), Mockito.any(MediaType.class));
        assertTrue(argument.getAllValues().get(0).contains("3.16.9"));
        assertTrue(argument.getAllValues().get(0).contains("VM00"));
        assertTrue(argument.getAllValues().get(1).contains("3.16.1"));
        assertTrue(argument.getAllValues().get(1).contains("VM01"));
    }
}
