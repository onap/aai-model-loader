/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.modelloader.notification;

import org.junit.jupiter.api.Test;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.consumer.IConfiguration;
import org.onap.sdc.api.consumer.IDistributionStatusMessage;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.onap.sdc.utils.DistributionStatusEnum;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"CONFIG_HOME=src/test/resources"})
public class DistributionStatusMessageBuilderTest {

    @Test
    public void testBuild() {
        IDistributionClient mockClient = mock(IDistributionClient.class);
        IConfiguration mockConfig = mock(IConfiguration.class);
        when(mockClient.getConfiguration()).thenReturn(mockConfig);
        when(mockConfig.getConsumerID()).thenReturn("testConsumerID");

        INotificationData mockData = mock(INotificationData.class);
        when(mockData.getDistributionID()).thenReturn("testDistributionID");

        IArtifactInfo mockArtifact = mock(IArtifactInfo.class);
        when(mockArtifact.getArtifactURL()).thenReturn("http://example.com/artifact");

        DistributionStatusEnum status = DistributionStatusEnum.DEPLOY_OK;
        IDistributionStatusMessage result = DistributionStatusMessageBuilder.build(mockClient, mockData, mockArtifact, status);

        assertNotNull(result);
        assertEquals("testDistributionID", result.getDistributionID());
        assertEquals("testConsumerID", result.getConsumerID());
        assertEquals("http://example.com/artifact", result.getArtifactURL());
        assertEquals(DistributionStatusEnum.DEPLOY_OK, result.getStatus());
    }

    @Test
    public void testBuildWithoutArtifactInfo() {
        IDistributionClient mockClient = mock(IDistributionClient.class);
        IConfiguration mockConfig = mock(IConfiguration.class);
        when(mockClient.getConfiguration()).thenReturn(mockConfig);
        when(mockConfig.getConsumerID()).thenReturn("testConsumerID");

        INotificationData mockData = mock(INotificationData.class);
        when(mockData.getDistributionID()).thenReturn("testDistributionID");

        DistributionStatusEnum status = DistributionStatusEnum.DEPLOY_OK;
        IDistributionStatusMessage result = DistributionStatusMessageBuilder.build(mockClient, mockData, status);

        assertNotNull(result);
        assertEquals("testDistributionID", result.getDistributionID());
        assertEquals("testConsumerID", result.getConsumerID());
        assertEquals("", result.getArtifactURL());
        assertEquals(DistributionStatusEnum.DEPLOY_OK, result.getStatus());
    }
}


