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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.babel.BabelArtifactService;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.extraction.InvalidArchiveException;
import org.onap.aai.modelloader.extraction.VnfCatalogExtractor;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.restclient.BabelServiceClientException;
import org.onap.aai.modelloader.service.BabelServiceClientFactory;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.results.IDistributionClientDownloadResult;
import org.onap.sdc.impl.DistributionClientDownloadResultImpl;
import org.onap.sdc.utils.DistributionActionResultEnum;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests {@link ArtifactDownloadManager} with VNF Catalog Artifacts.
 */
public class ArtifactDownloadManagerVnfcTest {

    @Autowired private ModelLoaderConfig config;
    @Mock private ArtifactDownloadManager downloadManager;
    @Mock private BabelServiceClient mockBabelClient;
    @Mock private IDistributionClient mockDistributionClient;
    @Mock private NotificationPublisher mockNotificationPublisher;
    @Mock private BabelArtifactConverter mockBabelArtifactConverter;
    @Mock private BabelServiceClientFactory mockClientFactory;
    @Mock private VnfCatalogExtractor mockVnfCatalogExtractor;
    @InjectMocks private BabelArtifactService babelArtifactService;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(mockClientFactory.create(Mockito.any())).thenReturn(mockBabelClient);

        Properties configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream("model-loader.properties"));
        downloadManager = new ArtifactDownloadManager(mockDistributionClient,
                mockNotificationPublisher, mockVnfCatalogExtractor, babelArtifactService);
    }

    @Test
    public void downloadArtifacts_validToscaVnfcCsarFile()
            throws IOException, BabelServiceClientException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        setupValidDownloadCsarMocks(data, artifactInfo);
        when(mockBabelClient.postArtifact(any(), any())).thenReturn(createBabelArtifacts());
        when(mockVnfCatalogExtractor.extract(any(), any())).thenReturn(new ArrayList<>());

        List<Artifact> modelArtifacts = new ArrayList<>();
        List<Artifact> catalogFiles = new ArrayList<>();
        assertThat(downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, catalogFiles),
                is(true));

        assertEquals(2, catalogFiles.size(), "There should have been some catalog files");
    }

    @Test
    public void downloadArtifacts_validXmlVnfcCsarFile()
            throws IOException, BabelServiceClientException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        setupValidDownloadCsarMocks(data, artifactInfo);
        when(mockBabelClient.postArtifact(any(), any())).thenReturn(createBabelArtifactsNoVnfc());
        when(mockVnfCatalogExtractor.extract(any(), any())).thenReturn(createXmlVnfcArtifacts());

        List<Artifact> modelArtifacts = new ArrayList<>();
        List<Artifact> catalogFiles = new ArrayList<>();
        assertThat(downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, catalogFiles),
                is(true));

        assertEquals(3, catalogFiles.size(), "There should have been some catalog files");
    }

    @Test
    public void downloadArtifacts_validNoVnfcCsarFile()
            throws IOException, BabelServiceClientException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        setupValidDownloadCsarMocks(data, artifactInfo);
        when(mockBabelClient.postArtifact(any(), any())).thenReturn(createBabelArtifactsNoVnfc());
        when(mockVnfCatalogExtractor.extract(any(), any())).thenReturn(new ArrayList<>());

        List<Artifact> modelArtifacts = new ArrayList<>();
        List<Artifact> catalogFiles = new ArrayList<>();
        assertThat(downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, catalogFiles),
                is(true));

        assertEquals(0, catalogFiles.size(), "There should not have been any catalog files");
    }

    @Test
    public void downloadArtifacts_invalidXmlAndToscaVnfcCsarFile()
            throws IOException, BabelServiceClientException, BabelArtifactParsingException, InvalidArchiveException {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        setupValidDownloadCsarMocks(data, artifactInfo);
        when(mockBabelClient.postArtifact(any(), any())).thenReturn(createBabelArtifacts());
        when(mockVnfCatalogExtractor.extract(any(), any())).thenReturn(createXmlVnfcArtifacts());
        doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);

        List<Artifact> modelArtifacts = new ArrayList<>();
        List<Artifact> catalogFiles = new ArrayList<>();
        assertThat(downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, catalogFiles),
                is(false));

        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);
    }

    private IDistributionClientDownloadResult createDistributionClientDownloadResult(
            DistributionActionResultEnum status, String message, byte[] payload) {
        IDistributionClientDownloadResult downloadResult = new DistributionClientDownloadResultImpl(status, message);

        ((DistributionClientDownloadResultImpl) downloadResult).setArtifactPayload(payload);

        return downloadResult;
    }

    private void setupValidDownloadCsarMocks(INotificationData data, IArtifactInfo artifactInfo)
            throws IOException, BabelServiceClientException, BabelArtifactParsingException {
        when(mockDistributionClient.download(artifactInfo))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        new ArtifactTestUtils().loadResource("compressedArtifacts/service-VscpaasTest-csar.csar")));
        when(mockBabelArtifactConverter.convertToModel(Mockito.anyList())).thenReturn(createModelArtifacts());
        when(mockBabelArtifactConverter.convertToCatalog(Mockito.anyList())).thenReturn(createToscaVnfcArtifacts());
    }

    private List<BabelArtifact> createBabelArtifacts() {
        List<BabelArtifact> artifactList = new ArrayList<>();
        artifactList.add(new BabelArtifact("ModelArtifact", BabelArtifact.ArtifactType.MODEL, "Some model payload"));
        artifactList.add(new BabelArtifact("VNFCArtifact", BabelArtifact.ArtifactType.VNFCATALOG, "Some VNFC payload"));
        return artifactList;
    }

    private List<BabelArtifact> createBabelArtifactsNoVnfc() {
        List<BabelArtifact> artifactList = new ArrayList<>();
        artifactList.add(new BabelArtifact("ModelArtifact", BabelArtifact.ArtifactType.MODEL, "Some model payload"));
        return artifactList;
    }

    private List<Artifact> createModelArtifacts() {
        List<Artifact> modelArtifacts = new ArrayList<>();
        modelArtifacts.add(new ModelArtifact());
        modelArtifacts.add(new ModelArtifact());
        return modelArtifacts;
    }

    private List<Artifact> createToscaVnfcArtifacts() {
        List<Artifact> vnfcArtifacts = new ArrayList<>();
        vnfcArtifacts.add(new VnfCatalogArtifact("Some VNFC payload"));
        vnfcArtifacts.add(new VnfCatalogArtifact("Some VNFC payload"));
        return vnfcArtifacts;
    }

    private List<Artifact> createXmlVnfcArtifacts() {
        List<Artifact> vnfcArtifacts = new ArrayList<>();
        vnfcArtifacts.add(new VnfCatalogArtifact(ArtifactType.VNF_CATALOG_XML, "Some VNFC payload"));
        vnfcArtifacts.add(new VnfCatalogArtifact(ArtifactType.VNF_CATALOG_XML, "Some VNFC payload"));
        vnfcArtifacts.add(new VnfCatalogArtifact(ArtifactType.VNF_CATALOG_XML, "Some VNFC payload"));
        return vnfcArtifacts;
    }
}
