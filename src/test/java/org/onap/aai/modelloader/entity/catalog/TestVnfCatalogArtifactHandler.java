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
package org.onap.aai.modelloader.entity.catalog;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

public class TestVnfCatalogArtifactHandler {

    protected static String CONFIG_FILE = "model-loader.properties";

    private AaiRestClient mockRestClient = mock(AaiRestClient.class);

    /**
     * Update A&AI with 4 images, 2 of which already exist.
     * 
     * @throws Exception
     */
    @Test
    public void testUpdateVnfImages() throws Exception {
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
        mockPutOperations();

        // Example VNF Catalog XML
        VnfCatalogArtifactHandler handler = new VnfCatalogArtifactHandler(createConfig());
        assertThat(handler.pushArtifacts(createVnfCatalogArtifact(), "test", new ArrayList<Artifact>(), mockRestClient),
                is(true));

        assertPutOperationsSucceeded();
    }

    private ModelLoaderConfig createConfig() {
        Properties configProperties = new Properties();
        try {
            configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
        } catch (IOException e) {
            fail();
        }
        ModelLoaderConfig config = new ModelLoaderConfig(configProperties, null);
        return config;
    }

    /**
     * Example VNF Catalog based on JSON data (returned by Babel)
     * 
     * @return test Artifacts
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    private List<Artifact> createVnfCatalogArtifact() throws IOException, UnsupportedEncodingException {
        String examplePath = "src/test/resources/imagedataexample.json";
        byte[] encoded = Files.readAllBytes(Paths.get(examplePath));
        List<Artifact> artifacts = new ArrayList<Artifact>();
        artifacts.add(new VnfCatalogArtifact(new String(encoded, "utf-8")));
        return artifacts;
    }

    /**
     * Always return CREATED (success) for a PUT operation.
     */
    private void mockPutOperations() {
        OperationResult mockPutResp = mock(OperationResult.class);
        when(mockPutResp.getResultCode()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(mockRestClient.putResource(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.any(MediaType.class))).thenReturn(mockPutResp);
    }

    private void assertPutOperationsSucceeded() {
        // Only two of the VNF images should be pushed
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        AaiRestClient mockedClient = Mockito.verify(mockRestClient, Mockito.times(2));
        mockedClient.putResource(Mockito.anyString(), argument.capture(), Mockito.anyString(),
                Mockito.any(MediaType.class));
        assertThat(argument.getAllValues().get(0), containsString("3.16.9"));
        assertThat(argument.getAllValues().get(0), containsString("VM00"));
        assertThat(argument.getAllValues().get(1), containsString("3.16.1"));
        assertThat(argument.getAllValues().get(1), containsString("VM01"));
    }
}
