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
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithInvalidType;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithModelQuerySpec;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneOfEach;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneService;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.babel.BabelArtifactService;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.NamedQueryArtifact;
import org.onap.aai.modelloader.extraction.InvalidArchiveException;
import org.onap.aai.modelloader.extraction.VnfCatalogExtractor;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.restclient.BabelServiceClientException;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.results.IDistributionClientDownloadResult;
import org.onap.sdc.impl.DistributionClientDownloadResultImpl;
import org.onap.sdc.utils.DistributionActionResultEnum;

/**
 * Tests {@link ArtifactDownloadManager}.
 */
public class TestArtifactDownloadManager {

    private ArtifactDownloadManager downloadManager;
    @Mock private BabelServiceClient mockBabelClient;
    @Mock private IDistributionClient mockDistributionClient;
    @Mock private NotificationPublisher mockNotificationPublisher;
    @Mock private BabelArtifactConverter mockBabelArtifactConverter;
    @InjectMocks BabelArtifactService babelArtifactService;
    private VnfCatalogExtractor vnfCatalogExtractor;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        vnfCatalogExtractor = new VnfCatalogExtractor();

        Properties configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream("model-loader.properties"));
        downloadManager = new ArtifactDownloadManager(mockDistributionClient,
                mockNotificationPublisher, vnfCatalogExtractor, babelArtifactService);
    }

    @AfterEach
    public void tearDown() {
        downloadManager = null;
        mockDistributionClient = null;
        mockNotificationPublisher = null;
    }

    /**
     * Test downloading zero artifacts from SDC.
 * @throws Exception 
     */
    @Test
    public void testDownloadWithZeroArtifacts() throws Exception {
        List<Artifact> artifacts = downloadManager.downloadArtifacts(getNotificationDataWithOneService(), new ArrayList<>());
        assertThat(artifacts, is(empty()));
        Mockito.verifyNoInteractions(mockBabelClient, mockDistributionClient, mockNotificationPublisher,
                mockBabelArtifactConverter);
    }

    @Test
    public void testArtifactDownloadFails() {
        INotificationData data = getNotificationDataWithOneService();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);
        String errorMessage = "error msg";
        when(mockDistributionClient.download(artifact)).thenReturn(
                createDistributionClientDownloadResult(DistributionActionResultEnum.FAIL, errorMessage, null));
        doNothing().when(mockNotificationPublisher).publishDownloadFailure(mockDistributionClient, data, artifact,
                errorMessage);

        assertThrows(DownloadFailureException.class,
                () -> downloadManager.downloadArtifacts(data, data.getServiceArtifacts()));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadFailure(mockDistributionClient, data, artifact,
                errorMessage);

        Mockito.verifyNoInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    @Test
    public void downloadArtifacts_invalidToscaCsarFile() throws IOException, BabelServiceClientException {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);
        when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This is not a valid Tosca CSAR File".getBytes()));
        doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        when(mockBabelClient.postArtifact(any(), any())).thenThrow(new BabelServiceClientException(""));
        doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        assertThrows(ProcessToscaArtifactsException.class,
                () -> downloadManager.downloadArtifacts(data, data.getServiceArtifacts()));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockBabelClient).postArtifact(any(), any());
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyNoInteractions(mockBabelArtifactConverter);

    }

    @Test
    public void downloadArtifacts_invalidModelQuerySpec() {
        INotificationData data = getNotificationDataWithModelQuerySpec();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);

        List<org.onap.aai.modelloader.entity.Artifact> modelArtifacts = new ArrayList<>();

        when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This is not a valid Model Query Spec".getBytes()));
        doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);

        assertThrows(BabelArtifactParsingException.class,
                () -> downloadManager.downloadArtifacts(data, data.getServiceArtifacts()));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyNoInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    @Test
    public void downloadArtifacts_validToscaCsarFile()
            throws Exception {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        setupValidDownloadCsarMocks(data, artifactInfo, new ArtifactTestUtils());

        List<Artifact> artifacts = downloadManager.downloadArtifacts(data, data.getServiceArtifacts());
        List<Artifact> modelArtifacts = artifacts.stream().filter(ModelArtifact.class::isInstance)
                .collect(Collectors.toList());
        List<Artifact> catalogArtifacts = artifacts.stream().filter(VnfCatalogArtifact.class::isInstance)
                .collect(Collectors.toList());
        assertThat(modelArtifacts.size(), is(1));
        assertThat(catalogArtifacts.size(), is(1));

        Mockito.verify(mockDistributionClient).download(artifactInfo);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifactInfo);
        Mockito.verify(mockBabelClient).postArtifact(any(), any());
        Mockito.verify(mockBabelArtifactConverter).convertToModel(any());
        Mockito.verify(mockBabelArtifactConverter).convertToCatalog(any());
    }

    private void setupValidDownloadCsarMocks(INotificationData data, IArtifactInfo artifactInfo,
            ArtifactTestUtils artifactTestUtils) throws IOException, BabelServiceClientException, BabelArtifactParsingException {
        when(mockDistributionClient.download(artifactInfo))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        artifactTestUtils.loadResource("compressedArtifacts/service-VscpaasTest-csar.csar")));
        when(mockBabelClient.postArtifact(any(), any())).thenReturn(createBabelArtifacts());

        when(mockBabelArtifactConverter.convertToModel(any(BabelArtifact.class))).thenReturn(List.of(new ModelArtifact()));
        when(mockBabelArtifactConverter.convertToCatalog(any(BabelArtifact.class))).thenReturn(new VnfCatalogArtifact(""));
    }

    private List<BabelArtifact> createBabelArtifacts() {
        List<BabelArtifact> artifactList = new ArrayList<>();
        artifactList.add(new BabelArtifact("ModelArtifact", BabelArtifact.ArtifactType.MODEL, "Some model payload"));
        artifactList.add(new BabelArtifact("VNFCArtifact", BabelArtifact.ArtifactType.VNFCATALOG, "Some VNFC payload"));
        return artifactList;
    }

    @Test
    public void downloadArtifactsWithValidModelQuerySpec()
            throws Exception {
        INotificationData data = getNotificationDataWithModelQuerySpec();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);
        setupValidModelQuerySpecMocks(new ArtifactTestUtils(), data, artifact);

        List<Artifact> artifacts = downloadManager.downloadArtifacts(data, data.getServiceArtifacts());
        List<Artifact> namedQueryArtifacts = artifacts.stream().filter(NamedQueryArtifact.class::isInstance)
                .collect(Collectors.toList());
        List<Artifact> catalogArtifacts = artifacts.stream().filter(VnfCatalogArtifact.class::isInstance)
                .collect(Collectors.toList());

        assertThat(namedQueryArtifacts.size(), is(1));
        assertThat(catalogArtifacts, is(empty()));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);

        Mockito.verifyNoInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    private void setupValidModelQuerySpecMocks(ArtifactTestUtils artifactTestUtils, INotificationData data,
            IArtifactInfo artifact) throws IOException {
        when(mockDistributionClient.download(artifact))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        artifactTestUtils.loadResource("models/named-query-wan-connector.xml")));
        doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
    }

    @Test
    public void downloadArtifacts_validCsarAndModelFiles()
            throws Exception {
        ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();
        INotificationData data = getNotificationDataWithOneOfEach();
        List<IArtifactInfo> artifactInfos = new ArrayList<>();

        IArtifactInfo serviceArtifact = data.getServiceArtifacts().get(0);
        IArtifactInfo modelSpecArtifact = data.getResources().get(1).getArtifacts().get(0);

        artifactInfos.add(serviceArtifact);
        artifactInfos.add(modelSpecArtifact);

        setupValidDownloadCsarMocks(data, serviceArtifact, artifactTestUtils);
        setupValidModelQuerySpecMocks(artifactTestUtils, data, modelSpecArtifact);

        List<Artifact> artifacts = downloadManager.downloadArtifacts(data, artifactInfos);
        List<Artifact> modelArtifacts = artifacts.stream().filter(artifact -> !(artifact instanceof VnfCatalogArtifact))
                .collect(Collectors.toList());
        List<Artifact> catalogArtifacts = artifacts.stream().filter(VnfCatalogArtifact.class::isInstance)
                .collect(Collectors.toList());
        assertThat(modelArtifacts.size(), is(2));
        assertThat(catalogArtifacts.size(), is(1));

        Mockito.verify(mockDistributionClient).download(serviceArtifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, serviceArtifact);
        Mockito.verify(mockBabelClient).postArtifact(any(), any());
        Mockito.verify(mockBabelArtifactConverter).convertToModel(any());
        Mockito.verify(mockBabelArtifactConverter).convertToCatalog(any());

        Mockito.verify(mockDistributionClient).download(modelSpecArtifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data,
                modelSpecArtifact);

        Mockito.verifyNoMoreInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    @Test
    public void activateCallback_toscaToModelConverterHasProcessToscaArtifactsException() throws Exception {
        ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        when(mockDistributionClient.download(artifactInfo))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        artifactTestUtils.loadResource("compressedArtifacts/service-VscpaasTest-csar.csar")));
        when(mockBabelArtifactConverter.convertToModel(any()))
                .thenThrow(BabelArtifactParsingException.class);
        doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);
        when(mockBabelClient.postArtifact(any(), any())).thenReturn(createBabelArtifacts());

        List<Artifact> modelArtifacts = new ArrayList<>();
        List<Artifact> catalogFiles = new ArrayList<>();
        assertThrows(ProcessToscaArtifactsException.class,
                () -> downloadManager.downloadArtifacts(data, data.getServiceArtifacts()));

        assertThat(modelArtifacts, is(empty()));
        assertThat(catalogFiles, is(empty()));

        Mockito.verify(mockDistributionClient).download(artifactInfo);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);
        Mockito.verify(mockBabelClient).postArtifact(any(), any());
        Mockito.verify(mockBabelArtifactConverter).convertToModel(any());
    }

    @Test
    public void downloadArtifactsWithInvalidType()
            throws IOException, BabelServiceClientException, BabelArtifactParsingException {
        INotificationData data = getNotificationDataWithInvalidType();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);

        when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This content does not matter.".getBytes()));
        doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);

        assertThrows(InvalidArchiveException.class,
                () -> downloadManager.downloadArtifacts(data, data.getServiceArtifacts()));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);
        Mockito.verifyNoInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    private IDistributionClientDownloadResult createDistributionClientDownloadResult(
            DistributionActionResultEnum status, String message, byte[] payload) {
        DistributionClientDownloadResultImpl downloadResult = new DistributionClientDownloadResultImpl(status, message);
        downloadResult.setArtifactPayload(payload);
        return downloadResult;
    }
}
