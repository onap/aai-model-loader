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
package org.onap.aai.modelloader.notification;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithCatalogFile;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneOfEach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifactHandler;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.ModelArtifactHandler;
import org.onap.aai.modelloader.extraction.InvalidArchiveException;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.INotificationData;

/**
 * Tests {@link ArtifactDeploymentManager }
 */
public class ArtifactDeploymentManagerTest {

    private static final String CONFIG_FILE = "model-loader.properties";
    private static final String SHOULD_HAVE_RETURNED_FALSE = "This should have returned false";

    private Properties configProperties;
    private ArtifactDeploymentManager manager;

    private IDistributionClient mockDistributionClient;
    private ModelArtifactHandler mockModelArtifactHandler;
    private NotificationPublisher mockNotificationPublisher;
    private VnfCatalogArtifactHandler mockVnfCatalogArtifactHandler;

    @Before
    public void setup() throws IOException {
        configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
        ModelLoaderConfig config = new ModelLoaderConfig(configProperties, null);

        mockDistributionClient = mock(IDistributionClient.class);
        mockModelArtifactHandler = mock(ModelArtifactHandler.class);
        mockNotificationPublisher = mock(NotificationPublisher.class);
        mockVnfCatalogArtifactHandler = mock(VnfCatalogArtifactHandler.class);

        manager = new ArtifactDeploymentManager(mockDistributionClient, config);

        Whitebox.setInternalState(manager, "modelArtifactHandler", mockModelArtifactHandler);
        Whitebox.setInternalState(manager, "notificationPublisher", mockNotificationPublisher);
        Whitebox.setInternalState(manager, "vnfCatalogArtifactHandler", mockVnfCatalogArtifactHandler);
    }

    @After
    public void tearDown() {
        configProperties = null;
        mockDistributionClient = null;
        mockModelArtifactHandler = null;
        mockNotificationPublisher = null;
        mockVnfCatalogArtifactHandler = null;
        manager = null;
    }

    private List<BabelArtifact> setupTest(byte[] xml, INotificationData data) throws IOException {
        List<BabelArtifact> toscaArtifacts = new ArrayList<>();
        org.onap.sdc.api.notification.IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        BabelArtifact xmlArtifact =
                new BabelArtifact(artifactInfo.getArtifactName(), BabelArtifact.ArtifactType.MODEL, new String(xml));
        toscaArtifacts.add(xmlArtifact);

        return toscaArtifacts;
    }

    @Test
    public void deploy_catalogDeploymentsFailed()
            throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithCatalogFile();

        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        catalogFiles.add(new VnfCatalogArtifact("Some catalog content"));

        when(mockModelArtifactHandler.pushArtifacts(any(), any(), any(), any())).thenReturn(true);
        when(mockVnfCatalogArtifactHandler.pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()), any(), any()))
                .thenReturn(false);
        Mockito.doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data,
                data.getServiceArtifacts().get(0));

        assertFalse(SHOULD_HAVE_RETURNED_FALSE,
                manager.deploy(data, data.getServiceArtifacts(), new ArrayList<>(), catalogFiles));

        Mockito.verify(mockModelArtifactHandler).pushArtifacts(eq(new ArrayList<Artifact>()),
                eq(data.getDistributionID()), any(), any());
        Mockito.verify(mockVnfCatalogArtifactHandler).pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()),
                any(), any());
        Mockito.verify(mockModelArtifactHandler).rollback(eq(new ArrayList<Artifact>()), eq(data.getDistributionID()),
                any());
        Mockito.verify(mockVnfCatalogArtifactHandler).rollback(eq(new ArrayList<Artifact>()),
                eq(data.getDistributionID()), any());
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data,
                data.getServiceArtifacts().get(0));
    }

    private void doFailedCombinedTests(boolean modelsOK, boolean catalogsOK)
            throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithOneOfEach();
        ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();
        byte[] xml = artifactTestUtils.loadResource("convertedYmls/AAI-SCP-Test-VSP-resource-1.0.xml");
        List<BabelArtifact> toscaArtifacts = setupTest(xml, data);
        List<Artifact> modelArtifacts = new BabelArtifactConverter().convertToModel(toscaArtifacts);

        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        catalogFiles.add(new VnfCatalogArtifact("Some catalog content"));

        when(mockVnfCatalogArtifactHandler.pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()), any(), any()))
                .thenReturn(catalogsOK);
        when(mockModelArtifactHandler.pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(), any()))
                .thenReturn(modelsOK);

        Mockito.doNothing().when(mockNotificationPublisher).publishDeploySuccess(mockDistributionClient, data,
                data.getServiceArtifacts().get(0));
        Mockito.doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data,
                data.getServiceArtifacts().get(0));

        assertFalse(SHOULD_HAVE_RETURNED_FALSE,
                manager.deploy(data, data.getServiceArtifacts(), modelArtifacts, catalogFiles));

        // Catalog artifacts are only pushed if models are successful.
        Mockito.verify(mockModelArtifactHandler).pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(),
                any());
        if (modelsOK) {
            Mockito.verify(mockVnfCatalogArtifactHandler).pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()),
                    any(), any());
        }

        if (modelsOK && catalogsOK) {
            Mockito.verify(mockNotificationPublisher).publishDeploySuccess(mockDistributionClient, data,
                    data.getServiceArtifacts().get(0));
            Mockito.verify(mockModelArtifactHandler, Mockito.never()).rollback(any(), any(), any());
            Mockito.verify(mockVnfCatalogArtifactHandler, Mockito.never()).rollback(any(), any(), any());
        } else {
            Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data,
                    data.getServiceArtifacts().get(0));
            if (modelsOK) {
                Mockito.verify(mockModelArtifactHandler).rollback(eq(new ArrayList<Artifact>()),
                        eq(data.getDistributionID()), any());
                Mockito.verify(mockVnfCatalogArtifactHandler).rollback(eq(new ArrayList<Artifact>()),
                        eq(data.getDistributionID()), any());
            } else {
                Mockito.verify(mockModelArtifactHandler).rollback(eq(new ArrayList<Artifact>()),
                        eq(data.getDistributionID()), any());
                Mockito.verify(mockVnfCatalogArtifactHandler, Mockito.never()).rollback(any(), any(), any());
            }
        }
    }

}
