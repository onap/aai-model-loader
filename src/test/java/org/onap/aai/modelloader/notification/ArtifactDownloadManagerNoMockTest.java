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
package org.onap.aai.modelloader.notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithInvalidType;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithModelQuerySpec;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneService;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.restclient.BabelServiceClient.BabelServiceException;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.INotificationData;
import org.openecomp.sdc.api.results.IDistributionClientDownloadResult;
import org.openecomp.sdc.impl.DistributionClientDownloadResultImpl;
import org.openecomp.sdc.utils.DistributionActionResultEnum;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

/**
 * No-Mock tests
 * 
 * Because Jacoco (and other coverage tools) can't cope with mocked classes under some circumstances, coverage is/was
 * falsely reported as < 50%. Hence these duplicated but non-mock tests to address this, for ONAP reasons.
 * 
 * @author andrewdo
 *
 */

/**
 * Tests {@link ArtifactDownloadManager}
 */
@PowerMockIgnore({"sun.security.ssl.*", "javax.net.ssl.*"})
@PrepareForTest({ArtifactDownloadManager.class})
public class ArtifactDownloadManagerNoMockTest {

    private static final String FALSE_SHOULD_HAVE_BEEN_RETURNED = "A value of 'false' should have been returned";
    private static final String OOPS = "oops";
    private static final String TRUE_SHOULD_HAVE_BEEN_RETURNED = "A value of 'true' should have been returned";

    private ArtifactDownloadManager downloadManager;
    private BabelServiceClient mockBabelClient;
    private IDistributionClient mockDistributionClient;
    private NotificationPublisher mockNotificationPublisher;
    private BabelArtifactConverter mockBabelArtifactConverter;

    @Before
    public void setup() throws Exception {
        mockBabelClient = PowerMockito.mock(BabelServiceClient.class);
        mockDistributionClient = PowerMockito.mock(IDistributionClient.class);
        mockNotificationPublisher = PowerMockito.mock(NotificationPublisher.class);
        mockBabelArtifactConverter = PowerMockito.mock(BabelArtifactConverter.class);

        Properties configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream("model-loader.properties"));
        downloadManager =
                new ArtifactDownloadManager(mockDistributionClient, new ModelLoaderConfig(configProperties, "."));

        PowerMockito.whenNew(BabelServiceClient.class).withAnyArguments().thenReturn(mockBabelClient);

        Whitebox.setInternalState(downloadManager, mockNotificationPublisher);
        Whitebox.setInternalState(downloadManager, mockBabelArtifactConverter);
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
        PowerMockito.when(mockDistributionClient.download(artifact))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.FAIL, OOPS, null));
        PowerMockito.doNothing().when(mockNotificationPublisher).publishDownloadFailure(mockDistributionClient, data,
                artifact, OOPS);

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
        PowerMockito.when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This is not a valid Tosca CSAR File".getBytes()));
        PowerMockito.doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data,
                artifact);
        PowerMockito.when(mockBabelClient.postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
                Matchers.anyString())).thenThrow(BabelServiceException.class);
        PowerMockito.doNothing().when(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data,
                artifact);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), null, null));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);

        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyZeroInteractions(mockBabelArtifactConverter);

    }

    @Test
    public void downloadArtifacts_invalidModelQuerySpec() {
        INotificationData data = getNotificationDataWithModelQuerySpec();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);

        List<org.onap.aai.modelloader.entity.Artifact> modelArtifacts = new ArrayList<>();

        PowerMockito.when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This is not a valid Model Query Spec".getBytes()));
        PowerMockito.doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data,
                artifact);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), modelArtifacts, null));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyZeroInteractions(mockBabelClient, mockBabelArtifactConverter);
    }


    private void setupValidDownloadCsarMocks(INotificationData data, IArtifactInfo artifactInfo,
            ArtifactTestUtils artifactTestUtils) throws IOException, BabelServiceException {
        PowerMockito.when(mockDistributionClient.download(artifactInfo))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        artifactTestUtils.loadResource("compressedArtifacts/service-VscpaasTest-csar.csar")));
        PowerMockito.doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data,
                artifactInfo);
        PowerMockito.when(mockBabelClient.postArtifact(Matchers.any(), Matchers.anyString(), Matchers.anyString(),
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
        PowerMockito.when(mockDistributionClient.download(artifact))
                .thenReturn(createDistributionClientDownloadResult(DistributionActionResultEnum.SUCCESS, null,
                        artifactTestUtils.loadResource("models/named-query-wan-connector.xml")));
        PowerMockito.doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data,
                artifact);
    }



    @Test
    public void downloadArtifacts_invalidType()
            throws IOException, BabelServiceException, BabelArtifactParsingException {
        INotificationData data = getNotificationDataWithInvalidType();
        IArtifactInfo artifact = data.getServiceArtifacts().get(0);

        List<org.onap.aai.modelloader.entity.Artifact> catalogArtifacts = new ArrayList<>();

        PowerMockito.when(mockDistributionClient.download(artifact)).thenReturn(createDistributionClientDownloadResult(
                DistributionActionResultEnum.SUCCESS, null, "This content does not matter.".getBytes()));
        PowerMockito.doNothing().when(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data,
                artifact);

        assertFalse(FALSE_SHOULD_HAVE_BEEN_RETURNED,
                downloadManager.downloadArtifacts(data, data.getServiceArtifacts(), null, catalogArtifacts));

        Mockito.verify(mockDistributionClient).download(artifact);
        Mockito.verify(mockNotificationPublisher).publishDownloadSuccess(mockDistributionClient, data, artifact);
        Mockito.verify(mockNotificationPublisher).publishDeployFailure(mockDistributionClient, data, artifact);

        Mockito.verifyZeroInteractions(mockBabelClient, mockBabelArtifactConverter);
    }
}
