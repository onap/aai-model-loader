/**
 * ﻿============LICENSE_START=======================================================
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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.restclient.client.OperationResult;

/**
 * Test the Model Artifact Handler using Mocks
 *
 */
public class TestModelArtifactHandler {

    @Mock
    private ModelLoaderConfig config;

    @Mock
    private AaiRestClient aaiClient;

    @Before
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEmptyLists() {
        ModelArtifactHandler handler = new ModelArtifactHandler(config);
        handler.pushArtifacts(Collections.emptyList(), "", Collections.emptyList(), aaiClient);
        handler.rollback(Collections.emptyList(), "", aaiClient);
    }

    @Test
    public void testSingleItemList() {
        when(config.getAaiBaseUrl()).thenReturn("");
        when(config.getAaiModelUrl(any())).thenReturn("");

        ModelArtifactHandler handler = new ModelArtifactHandler(config);
        List<Artifact> artifacts = Collections.singletonList(new ModelArtifact());
        handler.pushArtifacts(artifacts, "", Collections.emptyList(), aaiClient);
        handler.rollback(Collections.emptyList(), "", aaiClient);
    }

    @Test
    public void testPushExistingModelsWithRollback() {
        when(config.getAaiBaseUrl()).thenReturn("");
        when(config.getAaiModelUrl(any())).thenReturn("");

        OperationResult operationResult = mock(OperationResult.class);
        when(aaiClient.getResource(any(), any(), any())).thenReturn(operationResult);
        when(operationResult.getResultCode()).thenReturn(Response.Status.OK.getStatusCode());

        List<Artifact> artifacts = new ArrayList<>();
        Artifact artifact = new ModelArtifact();
        artifacts.add(artifact);

        ModelArtifactHandler handler = new ModelArtifactHandler(config);
        boolean pushed = handler.pushArtifacts(artifacts, "", Collections.emptyList(), aaiClient);
        assertThat(pushed, is(true));
        handler.rollback(artifacts, "", aaiClient);
    }

    @Test
    public void testPushNewModelsWithRollback() {
        when(config.getAaiBaseUrl()).thenReturn("");
        when(config.getAaiModelUrl(any())).thenReturn("");
        when(config.getAaiNamedQueryUrl(any())).thenReturn("");

        OperationResult getResult = mock(OperationResult.class);
        when(aaiClient.getResource(any(), any(), any())).thenReturn(getResult);
        when(getResult.getResultCode()).thenReturn(Response.Status.NOT_FOUND.getStatusCode());

        OperationResult putResult = mock(OperationResult.class);
        when(aaiClient.putResource(any(), any(), any(), any())).thenReturn(putResult);
        when(putResult.getResultCode()).thenReturn(Response.Status.CREATED.getStatusCode());

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
}

