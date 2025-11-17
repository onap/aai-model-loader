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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.onap.aai.modelloader.config.AaiProperties;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class TestVnfCatalogArtifactHandler {

    protected static AaiProperties aaiProperties = new AaiProperties();

    private AaiRestClient mockRestClient = mock(AaiRestClient.class);

    @BeforeAll
    public static void setup() {
        aaiProperties.setBaseUrl("http://aai.onap:80");
        aaiProperties.setModelUrl("/aai/%s/service-design-and-creation/models/model/");
        aaiProperties.setNamedQueryUrl("/aai/%s/service-design-and-creation/named-queries/named-query/");
        aaiProperties.setVnfImageUrl("/aai/v*/service-design-and-creation/vnf-images");
    }

    /**
     * Update A&AI with 4 images, 2 of which already exist.
     *
     * @throws Exception
     */
    @Test
    public void testUpdateVnfImages() throws Exception {
        // GET operation
        ResponseEntity mockGetResp = mock(ResponseEntity.class);

        // @formatter:off
        when(mockGetResp.getStatusCodeValue())
                .thenReturn(HttpStatus.OK.value())
                .thenReturn(HttpStatus.NOT_FOUND.value())
                .thenReturn(HttpStatus.NOT_FOUND.value())
                .thenReturn(HttpStatus.OK.value());
        // @formatter:on

        when(mockRestClient.getResource(Mockito.anyString(), Mockito.anyString(), Mockito.any(MediaType.class), Mockito.any()))
                .thenReturn(mockGetResp);
        mockPutOperations();

        // Example VNF Catalog XML
        VnfCatalogArtifactHandler handler = new VnfCatalogArtifactHandler(aaiProperties);
        assertTrue(handler.pushArtifacts(createVnfCatalogArtifact(), "test", new ArrayList<Artifact>(), mockRestClient));

        assertPutOperationsSucceeded();
    }

    @Test
    public void testUpdateVnfImagesFromXml() throws Exception {
        // GET operation
        ResponseEntity mockGetResp = mock(ResponseEntity.class);

        // @formatter:off
        when(mockGetResp.getStatusCodeValue())
                .thenReturn(HttpStatus.OK.value())
                .thenReturn(HttpStatus.NOT_FOUND.value())
                .thenReturn(HttpStatus.NOT_FOUND.value())
                .thenReturn(HttpStatus.OK.value());
        // @formatter:on

        when(mockRestClient.getResource(Mockito.anyString(), Mockito.anyString(), Mockito.any(MediaType.class), Mockito.any()))
                .thenReturn(mockGetResp);
        mockPutOperations();

        // Example VNF Catalog XML
        VnfCatalogArtifactHandler handler = new VnfCatalogArtifactHandler(aaiProperties);
        assertThat(
                handler.pushArtifacts(createVnfCatalogXmlArtifact(), "test", new ArrayList<Artifact>(), mockRestClient),
                is(true));

        // Only two of the VNF images should be pushed
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        AaiRestClient client = Mockito.verify(mockRestClient, Mockito.times(2));
        client.putResource(Mockito.anyString(), argument.capture(), Mockito.anyString(), Mockito.any(MediaType.class), Mockito.any());
        assertThat(argument.getAllValues().size(), is(2));
        assertThat(argument.getAllValues().get(0), containsString("5.2.5"));
        assertThat(argument.getAllValues().get(0), containsString("VM00"));
        assertThat(argument.getAllValues().get(1), containsString("5.2.4"));
        assertThat(argument.getAllValues().get(1), containsString("VM00"));
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
        byte[] encoded = Files.readAllBytes(Path.of(examplePath));
        List<Artifact> artifacts = new ArrayList<Artifact>();
        artifacts.add(new VnfCatalogArtifact(new String(encoded, "utf-8")));
        return artifacts;
    }

    /**
     * Example VNF Catalog based on VNF_CATALOG XML
     *
     * @return test Artifacts
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    private List<Artifact> createVnfCatalogXmlArtifact() throws IOException, UnsupportedEncodingException {
        byte[] encoded = Files.readAllBytes(Path.of("src/test/resources/xmlFiles/fortigate.xml"));
        List<Artifact> artifacts = new ArrayList<Artifact>();
        artifacts.add(new VnfCatalogArtifact(ArtifactType.VNF_CATALOG_XML, new String(encoded, "utf-8")));
        return artifacts;
    }

    /**
     * Always return CREATED (success) for a PUT operation.
     */
    private void mockPutOperations() {
        ResponseEntity mockPutResp = mock(ResponseEntity.class);
        when(mockPutResp.getStatusCode()).thenReturn(HttpStatus.CREATED);
        when(mockRestClient.putResource(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.any(MediaType.class), Mockito.any())).thenReturn(mockPutResp);
    }

    private void assertPutOperationsSucceeded() {
        // Only two of the VNF images should be pushed
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        AaiRestClient mockedClient = Mockito.verify(mockRestClient, Mockito.times(2));
        mockedClient.putResource(Mockito.anyString(), argument.capture(), Mockito.anyString(),
                Mockito.any(MediaType.class), Mockito.any());
        assertThat(argument.getAllValues().get(0), containsString("3.16.9"));
        assertThat(argument.getAllValues().get(0), containsString("VM00"));
        assertThat(argument.getAllValues().get(1), containsString("3.16.1"));
        assertThat(argument.getAllValues().get(1), containsString("VM01"));
    }
}
