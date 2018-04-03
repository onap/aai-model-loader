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

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder;
import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.notification.INotificationData;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Tests {@link EventCallback}
 */
@RunWith(PowerMockRunner.class)
public class EventCallbackTest {

    private static final String CONFIG_FILE = "model-loader.properties";

    private ModelLoaderConfig config;
    private Properties configProperties;
    private EventCallback eventCallback;

    private ArtifactDeploymentManager mockArtifactDeploymentManager;
    private ArtifactDownloadManager mockArtifactDownloadManager;
    private IDistributionClient mockDistributionClient;

    @Before
    public void setup() throws IOException {
        configProperties = new Properties();
        configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
        config = new ModelLoaderConfig(configProperties, null);

        mockArtifactDeploymentManager = PowerMockito.mock(ArtifactDeploymentManager.class);
        mockArtifactDownloadManager = PowerMockito.mock(ArtifactDownloadManager.class);
        mockDistributionClient = PowerMockito.mock(IDistributionClient.class);

        eventCallback = new EventCallback(mockDistributionClient, config);

        Whitebox.setInternalState(eventCallback, mockArtifactDeploymentManager);
        Whitebox.setInternalState(eventCallback, mockArtifactDownloadManager);
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

        PowerMockito.when(mockArtifactDownloadManager.downloadArtifacts(Mockito.any(INotificationData.class),
                Mockito.any(List.class), Mockito.any(List.class), Mockito.any(List.class)))
                .thenReturn(false);

        eventCallback.activateCallback(data);

        Mockito.verify(mockArtifactDownloadManager).downloadArtifacts(Mockito.any(INotificationData.class),
                Mockito.any(List.class), Mockito.any(List.class), Mockito.any(List.class));
        Mockito.verifyZeroInteractions(mockArtifactDeploymentManager);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void activateCallback() throws BabelArtifactParsingException {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

        PowerMockito.when(mockArtifactDownloadManager.downloadArtifacts(Mockito.any(INotificationData.class),
                Mockito.any(List.class), Mockito.any(List.class), Mockito.any(List.class)))
                .thenReturn(true);

        PowerMockito
                .when(mockArtifactDeploymentManager.deploy(Mockito.any(INotificationData.class),
                        Mockito.any(List.class), Mockito.any(List.class), Mockito.any(List.class)))
                .thenReturn(true);

        eventCallback.activateCallback(data);

        Mockito.verify(mockArtifactDownloadManager).downloadArtifacts(Mockito.any(INotificationData.class),
                Mockito.any(List.class), Mockito.any(List.class), Mockito.any(List.class));
        Mockito.verify(mockArtifactDeploymentManager).deploy(Mockito.any(INotificationData.class),
                Mockito.any(List.class), Mockito.any(List.class), Mockito.any(List.class));
    }
}
