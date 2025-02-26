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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder;
import org.onap.aai.modelloader.service.ArtifactDeploymentManager;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.INotificationData;

/**
 * Tests {@link EventCallback}.
 */
public class TestEventCallback {

    private static final String CONFIG_FILE = "model-loader.properties";

    private Properties configProperties;
    private EventCallback eventCallback;

    @Mock private ArtifactDeploymentManager mockArtifactDeploymentManager;
    @Mock private ArtifactDownloadManager mockArtifactDownloadManager;
    @Mock private IDistributionClient mockDistributionClient;
    @Mock private NotificationPublisher mockNotificationPublisher;

    @BeforeEach
    public void setup() throws IOException {
        MockitoAnnotations.openMocks(this);
        configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));

        eventCallback = new EventCallback(mockDistributionClient, mockArtifactDeploymentManager, mockArtifactDownloadManager, mockNotificationPublisher);
    }

    @AfterEach
    public void tearDown() {
        configProperties = null;
        eventCallback = null;
        mockArtifactDeploymentManager = null;
        mockArtifactDownloadManager = null;
        mockDistributionClient = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void activateCallback_downloadFails() throws Exception {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

        when(mockArtifactDownloadManager.downloadArtifacts(any(INotificationData.class), any(List.class)
        )).thenThrow(DownloadFailureException.class);

        eventCallback.activateCallback(data);

        verify(mockArtifactDownloadManager).downloadArtifacts(any(INotificationData.class), any(List.class));
        Mockito.verifyNoInteractions(mockArtifactDeploymentManager);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void activateCallback() throws Exception {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

        when(mockArtifactDownloadManager.downloadArtifacts(any(INotificationData.class), any(List.class)))
                .thenReturn(Collections.emptyList());

        when(mockArtifactDeploymentManager.deploy(any(String.class), any(List.class), any(List.class)))
                .thenReturn(true);

        eventCallback.activateCallback(data);

        verify(mockArtifactDownloadManager).downloadArtifacts(any(INotificationData.class), any(List.class));
        verify(mockArtifactDeploymentManager).deploy(any(String.class), any(List.class), any(List.class));
    }
    @Test
    public void testActivateCallback_withVnfCatalogArtifacts() throws Exception {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithCatalogFile();

        List<Artifact> downloadedArtifacts = new ArrayList<>();
        downloadedArtifacts.add(new Artifact(ArtifactType.MODEL));
        downloadedArtifacts.add(new Artifact(ArtifactType.VNF_CATALOG));

        when(mockArtifactDownloadManager.downloadArtifacts(any(INotificationData.class), any(List.class)))
                .thenReturn(downloadedArtifacts);

        eventCallback.activateCallback(data);

        verify(mockArtifactDeploymentManager).deploy(eq("ID"),
                argThat(list -> list.stream().anyMatch(a -> a.getType() == ArtifactType.MODEL)),
                anyList());
    }
}
