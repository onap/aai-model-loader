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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder;
import org.onap.aai.modelloader.service.ArtifactDeploymentManager;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.INotificationData;

/**
 * Tests {@link EventCallback}.
 */
public class TestEventCallback {

    private static final String CONFIG_FILE = "model-loader.properties";

    private ModelLoaderConfig config;
    private Properties configProperties;
    private EventCallback eventCallback;

    private ArtifactDeploymentManager mockArtifactDeploymentManager;
    private ArtifactDownloadManager mockArtifactDownloadManager;
    private IDistributionClient mockDistributionClient;
    private NotificationPublisher mockNotificationPublisher;

    @Before
    public void setup() throws IOException {
        configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
        config = new ModelLoaderConfig(configProperties, null);

        mockArtifactDeploymentManager = mock(ArtifactDeploymentManager.class);
        mockArtifactDownloadManager = mock(ArtifactDownloadManager.class);
        mockDistributionClient = mock(IDistributionClient.class);
        mockNotificationPublisher = mock(NotificationPublisher.class);

        eventCallback = new EventCallback(mockDistributionClient, config);

        Whitebox.setInternalState(eventCallback, "artifactDeploymentManager", mockArtifactDeploymentManager);
        Whitebox.setInternalState(eventCallback, "artifactDownloadManager", mockArtifactDownloadManager);
        Whitebox.setInternalState(eventCallback, "notificationPublisher", mockNotificationPublisher);
    }

    @After
    public void tearDown() {
        config = null;
        configProperties = null;
        eventCallback = null;
        mockArtifactDeploymentManager = null;
        mockArtifactDownloadManager = null;
        mockDistributionClient = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void activateCallback_downloadFails() {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

        when(mockArtifactDownloadManager.downloadArtifacts(any(INotificationData.class), any(List.class),
                any(List.class), any(List.class))).thenReturn(false);

        eventCallback.activateCallback(data);

        verify(mockArtifactDownloadManager).downloadArtifacts(any(INotificationData.class), any(List.class),
                any(List.class), any(List.class));
        Mockito.verifyZeroInteractions(mockArtifactDeploymentManager);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void activateCallback() throws BabelArtifactParsingException {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

        when(mockArtifactDownloadManager.downloadArtifacts(any(INotificationData.class), any(List.class),
                any(List.class), any(List.class))).thenReturn(true);

        when(mockArtifactDeploymentManager.deploy(any(INotificationData.class), any(List.class), any(List.class)))
                .thenReturn(true);

        eventCallback.activateCallback(data);

        verify(mockArtifactDownloadManager).downloadArtifacts(any(INotificationData.class), any(List.class),
                any(List.class), any(List.class));
        verify(mockArtifactDeploymentManager).deploy(any(INotificationData.class), any(List.class), any(List.class));
    }
}
