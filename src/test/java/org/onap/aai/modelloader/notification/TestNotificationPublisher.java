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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.consumer.IConfiguration;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.results.IDistributionClientResult;
import org.onap.sdc.utils.DistributionActionResultEnum;

/**
 * Test the Notification Publisher using Mocks
 *
 */
public class TestNotificationPublisher {

    @Mock
    private IDistributionClient client;

    @Mock
    private INotificationData data;

    @Mock
    private IArtifactInfo artifact;

    @Mock
    private IConfiguration config;

    @Mock
    private IDistributionClientResult clientResult;

    static {
        System.setProperty("CONFIG_HOME", "src/test/resources");
    }

    @BeforeEach
    public void setupMocks() {
        MockitoAnnotations.initMocks(this);
        when(client.getConfiguration()).thenReturn(config);
        when(client.sendDownloadStatus(any())).thenReturn(clientResult);
        when(client.sendComponentDoneStatus(any())).thenReturn(clientResult);
        when(client.sendComponentDoneStatus(any(), anyString())).thenReturn(clientResult);
        when(client.sendDeploymentStatus(any())).thenReturn(clientResult);
        when(clientResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.SUCCESS);
    }

    @Test
    public void testPublisher() {
        NotificationPublisher publisher = new NotificationPublisher();
        publisher.publishDownloadSuccess(client, data, artifact);
        publisher.publishDownloadFailure(client, data, artifact, "");
        publisher.publishComponentSuccess(client, data);
        publisher.publishComponentFailure(client, data, "");
        publisher.publishDeploySuccess(client, data, artifact);
        publisher.publishDeployFailure(client, data, artifact);
        assertTrue(true);
    }

}

