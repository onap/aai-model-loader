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
package org.onap.aai.modelloader.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithCatalogFile;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneOfEach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.config.AaiProperties;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifactHandler;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.ModelArtifactHandler;
import org.onap.aai.modelloader.entity.model.ModelArtifactParser;
import org.onap.aai.modelloader.extraction.InvalidArchiveException;
import org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ArtifactDeploymentManager;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.onap.sdc.api.notification.INotificationData;
import org.springframework.web.client.RestTemplate;

/**
 * Tests {@link ArtifactDeploymentManager}.
 */
public class TestArtifactDeploymentManager {

    private static final String SHOULD_HAVE_RETURNED_FALSE = "This should have returned false";

    private ArtifactDeploymentManager manager;

    @Mock private ModelArtifactHandler modelArtifactHandlerMock;
    @Mock private VnfCatalogArtifactHandler vnfCatalogArtifactHandlerMock;

    @BeforeEach
    public void setup() throws IOException {
        MockitoAnnotations.openMocks(this);
        AaiProperties aaiProperties = new AaiProperties();
        aaiProperties.setBaseUrl("http://aai.onap:80");
        aaiProperties.setModelUrl("/aai/%s/service-design-and-creation/models/model/");
        aaiProperties.setNamedQueryUrl("/aai/%s/service-design-and-creation/named-queries/named-query/");
        aaiProperties.setVnfImageUrl("/aai/%s/service-design-and-creation/vnf-images");

        AaiRestClient aaiRestClient = new AaiRestClient(aaiProperties, new RestTemplate());
        manager = new ArtifactDeploymentManager(modelArtifactHandlerMock, vnfCatalogArtifactHandlerMock, aaiRestClient);
    }

    @AfterEach
    public void tearDown() {
        modelArtifactHandlerMock = null;
        vnfCatalogArtifactHandlerMock = null;
        manager = null;
    }

    @Test
    public void deploy_csarDeploymentsFailed() throws IOException, BabelArtifactParsingException {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();
        byte[] xml = new ArtifactTestUtils().loadResource("convertedYmls/AAI-SCP-Test-VSP-resource-1.0.xml");
        BabelArtifact toscaArtifact = setupTest(xml, data);
        List<Artifact> modelArtifacts = new BabelArtifactConverter(new ModelArtifactParser()).convertToModel(toscaArtifact);

        when(modelArtifactHandlerMock.pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(), any()))
                .thenReturn(false);

        assertThat(SHOULD_HAVE_RETURNED_FALSE, manager.deploy(data.getDistributionID(), modelArtifacts, new ArrayList<>()), is(false));

        Mockito.verify(modelArtifactHandlerMock).pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(),
                any());
        Mockito.verify(vnfCatalogArtifactHandlerMock, Mockito.never()).pushArtifacts(eq(modelArtifacts),
                eq(data.getDistributionID()), any(), any());
        Mockito.verify(modelArtifactHandlerMock).rollback(eq(new ArrayList<Artifact>()), eq(data.getDistributionID()),
                any());
        Mockito.verify(vnfCatalogArtifactHandlerMock, Mockito.never()).rollback(eq(new ArrayList<Artifact>()),
                eq(data.getDistributionID()), any());
    }

    private BabelArtifact setupTest(byte[] xml, INotificationData data) throws IOException {
        org.onap.sdc.api.notification.IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        BabelArtifact xmlArtifact =
                new BabelArtifact(artifactInfo.getArtifactName(), BabelArtifact.ArtifactType.MODEL, new String(xml));

        return xmlArtifact;
    }

    @Test
    public void deploy_catalogDeploymentsFailed()
            throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithCatalogFile();

        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        catalogFiles.add(new VnfCatalogArtifact("Some catalog content"));

        when(modelArtifactHandlerMock.pushArtifacts(any(), any(), any(), any())).thenReturn(true);
        when(vnfCatalogArtifactHandlerMock.pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()), any(), any()))
                .thenReturn(false);

        assertThat(SHOULD_HAVE_RETURNED_FALSE, manager.deploy(data.getDistributionID(), new ArrayList<>(), catalogFiles), is(false));

        Mockito.verify(modelArtifactHandlerMock).pushArtifacts(eq(new ArrayList<Artifact>()),
                eq(data.getDistributionID()), any(), any());
        Mockito.verify(vnfCatalogArtifactHandlerMock).pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()),
                any(), any());
        Mockito.verify(modelArtifactHandlerMock).rollback(eq(new ArrayList<Artifact>()), eq(data.getDistributionID()),
                any());
        Mockito.verify(vnfCatalogArtifactHandlerMock).rollback(eq(new ArrayList<Artifact>()),
                eq(data.getDistributionID()), any());
    }

    @Test
    public void testNoArtifactsDeployed() throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        doFailedCombinedTests(false, false);
    }

    @Test
    public void testModelsNotDeployed() throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        doFailedCombinedTests(false, true);
    }

    @Test
    public void testCatalogsNotDeployed() throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        doFailedCombinedTests(true, false);
    }

    private void doFailedCombinedTests(boolean modelsDeployed, boolean catalogsDeployed)
            throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithOneOfEach();
        byte[] xml = new ArtifactTestUtils().loadResource("convertedYmls/AAI-SCP-Test-VSP-resource-1.0.xml");
        BabelArtifact toscaArtifact = setupTest(xml, data);
        List<Artifact> modelArtifacts = new BabelArtifactConverter(new ModelArtifactParser()).convertToModel(toscaArtifact);

        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        catalogFiles.add(new VnfCatalogArtifact("Some catalog content"));

        when(vnfCatalogArtifactHandlerMock.pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()), any(), any()))
                .thenReturn(catalogsDeployed);
        when(modelArtifactHandlerMock.pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(), any()))
                .thenReturn(modelsDeployed);

        assertThat(SHOULD_HAVE_RETURNED_FALSE, manager.deploy(data.getDistributionID(), modelArtifacts, catalogFiles), is(false));

        // Catalog artifacts are only pushed if models are successful.
        Mockito.verify(modelArtifactHandlerMock).pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(),
                any());
        if (modelsDeployed) {
            Mockito.verify(vnfCatalogArtifactHandlerMock).pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()),
                    any(), any());
        }

        if (modelsDeployed && catalogsDeployed) {
            Mockito.verify(modelArtifactHandlerMock, Mockito.never()).rollback(any(), any(), any());
            Mockito.verify(vnfCatalogArtifactHandlerMock, Mockito.never()).rollback(any(), any(), any());
        } else {
            if (modelsDeployed) {
                Mockito.verify(modelArtifactHandlerMock).rollback(eq(new ArrayList<Artifact>()),
                        eq(data.getDistributionID()), any());
                Mockito.verify(vnfCatalogArtifactHandlerMock).rollback(eq(new ArrayList<Artifact>()),
                        eq(data.getDistributionID()), any());
            } else {
                Mockito.verify(modelArtifactHandlerMock).rollback(eq(new ArrayList<Artifact>()),
                        eq(data.getDistributionID()), any());
                Mockito.verify(vnfCatalogArtifactHandlerMock, Mockito.never()).rollback(any(), any(), any());
            }
        }
    }

    /**
     * Deploy both models and VNF images.
     *
     * @throws IOException
     * @throws BabelArtifactParsingException
     * @throws InvalidArchiveException
     */
    @Test
    public void testDeploySuccess() throws IOException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithOneOfEach();
        byte[] xml = new ArtifactTestUtils().loadResource("convertedYmls/AAI-SCP-Test-VSP-resource-1.0.xml");
        BabelArtifact toscaArtifact = setupTest(xml, data);
        List<Artifact> modelArtifacts = new BabelArtifactConverter(new ModelArtifactParser()).convertToModel(toscaArtifact);

        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        catalogFiles.add(new VnfCatalogArtifact("Some catalog content"));

        when(vnfCatalogArtifactHandlerMock.pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()), any(), any()))
                .thenReturn(true);
        when(modelArtifactHandlerMock.pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(), any()))
                .thenReturn(true);

        assertThat(manager.deploy(data.getDistributionID(), modelArtifacts, catalogFiles), is(true));

        Mockito.verify(vnfCatalogArtifactHandlerMock).pushArtifacts(eq(catalogFiles), eq(data.getDistributionID()),
                any(), any());
        Mockito.verify(modelArtifactHandlerMock).pushArtifacts(eq(modelArtifacts), eq(data.getDistributionID()), any(),
                any());
        Mockito.verify(modelArtifactHandlerMock, Mockito.never()).rollback(any(), any(), any());
        Mockito.verify(vnfCatalogArtifactHandlerMock, Mockito.never()).rollback(any(), any(), any());
    }
}
