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
 import org.onap.sdc.api.consumer.IComponentDoneStatusMessage;
 import org.onap.sdc.api.consumer.IConfiguration;
 import org.onap.sdc.api.notification.INotificationData;
 import org.onap.sdc.utils.DistributionStatusEnum;
 import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.test.context.TestPropertySource;
 import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
 import static org.junit.jupiter.api.Assertions.*;

 @SpringBootTest
 @TestPropertySource(properties = {"CONFIG_HOME=src/test/resources"})
 public class CompDoneStatusMessageBuilderTest {

     @Test
     public void testBuild() {
         IDistributionClient mockClient = mock(IDistributionClient.class);
         INotificationData mockData = mock(INotificationData.class);

         IConfiguration mockConfig = mock(IConfiguration.class);
         when(mockClient.getConfiguration()).thenReturn(mockConfig);
         when(mockConfig.getConsumerID()).thenReturn("consumer123");

         when(mockData.getDistributionID()).thenReturn("distID456");

         DistributionStatusEnum status = DistributionStatusEnum.DEPLOY_OK;
         IComponentDoneStatusMessage result = CompDoneStatusMessageBuilder.build(mockClient, mockData, status);

         assertNotNull(result);
         assertTrue(result instanceof CompDoneStatusMsg);
         CompDoneStatusMsg statusMsg = (CompDoneStatusMsg) result;

         assertEquals("distID456", statusMsg.getDistributionID());
         assertEquals("consumer123", statusMsg.getConsumerID());
         assertEquals(DistributionStatusEnum.DEPLOY_OK, statusMsg.getStatus());
     }

     @Test
     public void testBuildWithFailureStatus() {
         IDistributionClient mockClient = mock(IDistributionClient.class);
         INotificationData mockData = mock(INotificationData.class);

         IConfiguration mockConfig = mock(IConfiguration.class);
         when(mockClient.getConfiguration()).thenReturn(mockConfig);
         when(mockConfig.getConsumerID()).thenReturn("consumer123");

         when(mockData.getDistributionID()).thenReturn("distID456");

         DistributionStatusEnum status = DistributionStatusEnum.DEPLOY_ERROR;
         IComponentDoneStatusMessage result = CompDoneStatusMessageBuilder.build(mockClient, mockData, status);

         assertNotNull(result);
         assertTrue(result instanceof CompDoneStatusMsg);
         CompDoneStatusMsg statusMsg = (CompDoneStatusMsg) result;

         assertEquals("distID456", statusMsg.getDistributionID());
         assertEquals("consumer123", statusMsg.getConsumerID());
         assertEquals(DistributionStatusEnum.DEPLOY_ERROR, statusMsg.getStatus());
     }

     @Test
     public void testBuildHandlesNullValues() {
         IDistributionClient mockClient = mock(IDistributionClient.class);
         INotificationData mockData = mock(INotificationData.class);

         IConfiguration mockConfig = mock(IConfiguration.class);
         when(mockClient.getConfiguration()).thenReturn(mockConfig);
         when(mockConfig.getConsumerID()).thenReturn(null);
         when(mockData.getDistributionID()).thenReturn(null);

         DistributionStatusEnum status = DistributionStatusEnum.DEPLOY_OK;
         IComponentDoneStatusMessage result = CompDoneStatusMessageBuilder.build(mockClient, mockData, status);

         assertNotNull(result);
         assertTrue(result instanceof CompDoneStatusMsg);
         CompDoneStatusMsg statusMsg = (CompDoneStatusMsg) result;

         assertNull(statusMsg.getDistributionID());
         assertNull(statusMsg.getConsumerID());
         assertEquals(DistributionStatusEnum.DEPLOY_OK, statusMsg.getStatus());
     }
}
