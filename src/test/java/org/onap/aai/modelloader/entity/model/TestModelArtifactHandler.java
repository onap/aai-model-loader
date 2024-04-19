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
package org.onap.aai.modelloader.entity.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Test the Model Artifact Handler using Mocks
 *
 */
public class TestModelArtifactHandler {

    @Mock
    private ModelLoaderConfig config;

    @Mock
    private AaiRestClient aaiClient;

    @BeforeEach
    public void setupMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testEmptyLists() {
        ModelArtifactHandler handler = new ModelArtifactHandler(config);
        handler.pushArtifacts(Collections.emptyList(), "", Collections.emptyList(), aaiClient);
        handler.rollback(Collections.emptyList(), "", aaiClient);
        assertTrue(true);
    }

    @Test
    public void testPushExistingModelsWithRollback() {
        when(config.getAaiBaseUrl()).thenReturn("");
        when(config.getAaiModelUrl(any())).thenReturn("");

        ResponseEntity operationResult = mock(ResponseEntity.class);
        when(aaiClient.getResource(any(), any(), any(), any())).thenReturn(operationResult);
        when(operationResult.getStatusCode()).thenReturn(HttpStatus.OK);

        List<Artifact> artifacts = new ArrayList<>();
        Artifact artifact = new ModelArtifact();
        artifacts.add(artifact);

        ModelArtifactHandler handler = new ModelArtifactHandler(config);
        boolean pushed = handler.pushArtifacts(artifacts, "", Collections.emptyList(), aaiClient);
        assertTrue(pushed);
        handler.rollback(artifacts, "", aaiClient);
    }

    @Test
    public void testPushNewModelsWithRollback() {
        when(config.getAaiBaseUrl()).thenReturn("");
        when(config.getAaiModelUrl(any())).thenReturn("");
        when(config.getAaiNamedQueryUrl(any())).thenReturn("");

        ResponseEntity getResult = mock(ResponseEntity.class);
        when(aaiClient.getResource(any(), any(), any(), any())).thenReturn(getResult);
        when(getResult.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

        ResponseEntity putResult = mock(ResponseEntity.class);
        when(aaiClient.putResource(any(), any(), any(), any(), any())).thenReturn(putResult);
        when(putResult.getStatusCode()).thenReturn(HttpStatus.CREATED);

        List<Artifact> artifacts = new ArrayList<>();
        artifacts.add(new ModelArtifact());
        NamedQueryArtifact namedQueryArtifact = new NamedQueryArtifact();
        namedQueryArtifact.setNamedQueryUuid("fred");
        namedQueryArtifact.setModelNamespace("http://org.onap.aai.inventory/v13");
        artifacts.add(namedQueryArtifact);

        List<Artifact> completedArtifacts = new ArrayList<>();
        ModelArtifactHandler handler = new ModelArtifactHandler(config);
        boolean pushed = handler.pushArtifacts(artifacts, "", completedArtifacts, aaiClient);
        assertThat(pushed, is(true));
        handler.rollback(artifacts, "", aaiClient);
    }

    @Test
    public void testPushNewModelsBadRequest() {
        when(config.getAaiBaseUrl()).thenReturn("");
        when(config.getAaiModelUrl(any())).thenReturn("");
        when(config.getAaiNamedQueryUrl(any())).thenReturn("");

        ResponseEntity getResult = mock(ResponseEntity.class);
        when(aaiClient.getResource(any(), any(), any(), any())).thenReturn(getResult);
        when(getResult.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

        ResponseEntity putResult = mock(ResponseEntity.class);
        when(aaiClient.putResource(any(), any(), any(), any(), any())).thenReturn(putResult);
        when(putResult.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        checkRollback(Collections.singletonList(new ModelArtifact()));
    }

    @Test
    public void testBadRequestResourceModelResult() {
        when(config.getAaiBaseUrl()).thenReturn("");
        when(config.getAaiModelUrl(any())).thenReturn("");

        ResponseEntity operationResult = mock(ResponseEntity.class);
        when(aaiClient.getResource(any(), any(), any(), any())).thenReturn(operationResult);
        when(operationResult.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        checkRollback(Collections.singletonList(new ModelArtifact()));
    }

    private void checkRollback(List<Artifact> artifacts) {
        ModelArtifactHandler handler = new ModelArtifactHandler(config);
        boolean pushed = handler.pushArtifacts(artifacts, "", Collections.emptyList(), aaiClient);
        assertThat(pushed, is(false));
        handler.rollback(artifacts, "", aaiClient);
    }
}

