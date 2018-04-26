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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithInvalidType;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithModelQuerySpec;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneOfEach;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneService;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.restclient.BabelServiceClient.BabelServiceException;
import org.onap.aai.modelloader.restclient.BabelServiceClientFactory;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.results.IDistributionClientDownloadResult;
import org.onap.sdc.impl.DistributionClientDownloadResultImpl;
import org.onap.sdc.utils.DistributionActionResultEnum;

/**
 * Tests {@link ArtifactDownloadManager}
 */
public class ArtifactDownloadManagerTest {

    private static final String FALSE_SHOULD_HAVE_BEEN_RETURNED = "A value of 'false' should have been returned";
    private static final String OOPS = "oops";
    private static final String TRUE_SHOULD_HAVE_BEEN_RETURNED = "A value of 'true' should have been returned";

    private ArtifactDownloadManager downloadManager;
    private BabelServiceClient mockBabelClient;
    private IDistributionClient mockDistributionClient;
    private NotificationPublisher mockNotificationPublisher;
    private BabelArtifactConverter mockBabelArtifactConverter;
    private BabelServiceClientFactory mockClientFactory;

    @Before
    public void setup() throws Exception {
        mockBabelClient = mock(BabelServiceClient.class);
        mockDistributionClient = mock(IDistributionClient.class);
        mockNotificationPublisher = mock(NotificationPublisher.class);
        mockBabelArtifactConverter = mock(BabelArtifactConverter.class);
        mockClientFactory = mock(BabelServiceClientFactory.class);
        when(mockClientFactory.create(Mockito.any())).thenReturn(mockBabelClient);

        Properties configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream("model-loader.properties"));
        downloadManager = new ArtifactDownloadManager(mockDistributionClient,
                new ModelLoaderConfig(configProperties, "."), mockClientFactory);

        Whitebox.setInternalState(downloadManager, "notificationPublisher", mockNotificationPublisher);
        Whitebox.setInternalState(downloadManager, "babelArtifactConverter", mockBabelArtifactConverter);
    }

    @After
    public void tearDown() {
        downloadManager = null;
        mockDistributionClient = null;
        mockNotificationPublisher = null;
    }

    @Test
    public void downloadArtifacts_emptyListSupplied() {
        List<org.onap.aai.modelloader.entity.Artifact> modelFiles = new ArrayList<>();
        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();

        assertTrue(TRUE_SHOULD_HAVE_BEEN_RETURNED, downloadManager
                .downloadArtifacts(getNotificationDataWithOneService(), new ArrayList<>(), modelFiles, catalogFiles));

        Mockito.verifyZeroInteractions(mockBabelClient, mockDistributionClient, mockNotificationPublisher,
                mockBabelArtifactConverter);
    }

    @Test
    public void downloadArtifacts_artifactDownloadFails() {
        INotificationData data = getNotificationDataWithOneService();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);
        when(mockDistributionClient.download(artifact))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.FAIL, OOPS, null));
        doNothing().when(mockNotificationPublisher).publishDownloadFailure(mockDistributionClient, data, artifact,
                OOPS);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), null, null));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadFailure(mockDistributionClient, data, artifact, OOPS);

        Mockito.verifyZeroInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    private IDistributionClientDownloadResult createDistributionClientDownloadResult(
            DistributionActionResultEnum status, String message, byte[] payload) {
        IDistributionClientDownloadResult downloadResult = new DistributionClientDownloadResultImpl(status, message);

        ((DistributionClientDownloadResultImpl) downloadResult).setArtifactPayload(payload);

        return downloadResult;
    }

    @Test
    public void downloadArtifacts_noSuchAlgorithmExceptionFromCreatingBabelClient() throws Exception {
        doCreateBabelClientFailureTest(NoSuchAlgorithmException.class);
    }

    @SuppressWarnings("unchecked")
    private void doCreateBabelClientFailureTest(Class<? extends Throwable> exception) throws Exception {
        when(mockClientFactory.create(Mockito.any())).thenThrow(exception);

        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);
        setupValidDownloadCsarMocks(data, artifactInfo, new ArtifactTestUtils());
        doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), null, null));

        Mockito.verify(mockDistributionClient).download(artifactInfo);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifactInfo);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);

        Mockito.verifyZeroInteractions(mockBabelArtifactConverter);
    }

    @Test
    public void downloadArtifacts_keyStoreExceptionFromCreatingBabelClient() throws Exception {
        doCreateBabelClientFailureTest(KeyStoreException.class);
    }

    @Test
    public void downloadArtifacts_certificateExceptionFromCreatingBabelClient() throws Exception {
        doCreateBabelClientFailureTest(CertificateException.class);
    }

    @Test
    public void downloadArtifacts_iOExceptionFromCreatingBabelClient() throws Exception {
        doCreateBabelClientFailureTest(IOException.class);
    }

    @Test
    public void downloadArtifacts_unrecoverableKeyExceptionFromCreatingBabelClient() throws Exception {
        doCreateBabelClientFailureTest(UnrecoverableKeyException.class);
    }

    @Test
    public void downloadArtifacts_keyManagementExceptionFromCreatingBabelClient() throws Exception {
        doCreateBabelClientFailureTest(KeyManagementException.class);
    }

    /**
     * Test disabled as exception handling needs to be reworked
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void downloadArtifacts_invalidToscaCsarFile() throws IOException, BabelServiceException {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);
        when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This is not a valid Tosca CSAR File".getBytes()));
        doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        when(mockBabelClient.postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString())).thenThrow(BabelServiceException.class);
        doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), null, null));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockBabelClient).postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString());
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyZeroInteractions(mockBabelArtifactConverter);

    }

    @Test
    public void downloadArtifacts_invalidModelQuerySpec() {
        INotificationData data = getNotificationDataWithModelQuerySpec();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);

        List<org.onap.aai.modelloader.entity.Artifact> modelArtifacts = new ArrayList<>();

        when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This is not a valid Model Query Spec".getBytes()));
        doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, null));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyZeroInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    @Test
    public void downloadArtifacts_validToscaCsarFile()
            throws IOException, BabelServiceException, BabelArtifactParsingException {
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        setupValidDownloadCsarMocks(data, artifactInfo, new ArtifactTestUtils());

        List<org.onap.aai.modelloader.entity.Artifact> modelArtifacts = new ArrayList<>();
        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        assertTrue(TRUE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, catalogFiles));

        assertTrue("There should not have been any catalog files", catalogFiles.size() == 0);

        Mockito.verify(mockDistributionClient).download(artifactInfo);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifactInfo);
        Mockito.verify(mockBabelClient).postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString());
        Mockito.verify(mockBabelArtifactConverter).convertToModel(Matchers.any());
        Mockito.verify(mockBabelArtifactConverter).convertToCatalog(Matchers.any());
    }

    private void setupValidDownloadCsarMocks(INotificationData data, IArtifactInfo artifactInfo,
            ArtifactTestUtils artifactTestUtils) throws IOException, BabelServiceException {
        when(mockDistributionClient.download(artifactInfo))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        artifactTestUtils.loadResource("compressedArtifacts/service-VscpaasTest-csar.csar")));
        when(mockBabelClient.postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString())).thenReturn(createBabelArtifacts());
    }

    private List<BabelArtifact> createBabelArtifacts() {
        List<BabelArtifact> artifactList = new ArrayList<>();
        artifactList.add(new BabelArtifact("ModelArtifact", BabelArtifact.ArtifactType.MODEL, "Some model payload"));
        artifactList.add(new BabelArtifact("VNFCArtifact", BabelArtifact.ArtifactType.VNFCATALOG, "Some VNFC payload"));
        return artifactList;
    }

    @Test
    public void downloadArtifacts_validModelQuerySpec()
            throws IOException, BabelServiceException, BabelArtifactParsingException {
        ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();
        INotificationData data = getNotificationDataWithModelQuerySpec();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);
        setupValidModelQuerySpecMocks(artifactTestUtils, data, artifact);

        List<org.onap.aai.modelloader.entity.Artifact> modelFiles = new ArrayList<>();
        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        assertTrue(TRUE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelFiles, catalogFiles));

        assertTrue("There should have been some model artifacts", !modelFiles.isEmpty());
        assertTrue("There should not have been any catalog artifacts", catalogFiles.isEmpty());

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);

        Mockito.verifyZeroInteractions(mockBabelClient, mockBabelArtifactConverter);
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
            throws IOException, BabelServiceException, BabelArtifactParsingException {
        ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();
        INotificationData data = getNotificationDataWithOneOfEach();
        List<IArtifactInfo> artifacts = new ArrayList<>();

        IArtifactInfo serviceArtifact = data.getServiceArtifacts().get(0);
        IArtifactInfo modelSpecArtifact = data.getResources().get(1).getArtifacts().get(0);

        artifacts.add(serviceArtifact);
        artifacts.add(modelSpecArtifact);

        setupValidDownloadCsarMocks(data, serviceArtifact, artifactTestUtils);
        setupValidModelQuerySpecMocks(artifactTestUtils, data, modelSpecArtifact);

        List<org.onap.aai.modelloader.entity.Artifact> modelFiles = new ArrayList<>();
        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        assertTrue(TRUE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, artifacts, modelFiles, catalogFiles));

        Mockito.verify(mockDistributionClient).download(serviceArtifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, serviceArtifact);
        Mockito.verify(mockBabelClient).postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString());
        Mockito.verify(mockBabelArtifactConverter).convertToModel(Matchers.any());
        Mockito.verify(mockBabelArtifactConverter).convertToCatalog(Matchers.any());

        Mockito.verify(mockDistributionClient).download(modelSpecArtifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data,
                modelSpecArtifact);

        Mockito.verifyNoMoreInteractions(mockBabelClient, mockBabelArtifactConverter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void activateCallback_toscaToModelConverterHasProcessToscaArtifactsException() throws Exception {
        ArtifactTestUtils artifactTestUtils = new ArtifactTestUtils();
        INotificationData data = getNotificationDataWithToscaCsarFile();
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        when(mockDistributionClient.download(artifactInfo))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        artifactTestUtils.loadResource("compressedArtifacts/service-VscpaasTest-csar.csar")));
        when(mockBabelArtifactConverter.convertToModel(Mockito.anyList()))
                .thenThrow(BabelArtifactParsingException.class);
        doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);
        when(mockBabelClient.postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString())).thenReturn(createBabelArtifacts());

        List<org.onap.aai.modelloader.entity.Artifact> modelArtifacts = new ArrayList<>();
        List<org.onap.aai.modelloader.entity.Artifact> catalogFiles = new ArrayList<>();
        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, catalogFiles));

        assertTrue("There should not have been any catalog files", catalogFiles.size() == 0);

        Mockito.verify(mockDistributionClient).download(artifactInfo);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifactInfo);
        Mockito.verify(mockBabelClient).postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString());
        Mockito.verify(mockBabelArtifactConverter).convertToModel(Matchers.any());
    }

    @Test
    public void downloadArtifacts_invalidType()
            throws IOException, BabelServiceException, BabelArtifactParsingException {
        INotificationData data = getNotificationDataWithInvalidType();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);

        List<org.onap.aai.modelloader.entity.Artifact> catalogArtifacts = new ArrayList<>();

        when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This content does not matter.".getBytes()));
        doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), null, catalogArtifacts));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyZeroInteractions(mockBabelClient, mockBabelArtifactConverter);
    }
}
