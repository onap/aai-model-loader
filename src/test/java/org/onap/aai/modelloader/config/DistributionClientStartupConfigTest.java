/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom AG Intellectual Property. All rights reserved.
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
package org.onap.aai.modelloader.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.aai.modelloader.notification.EventCallback;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.results.IDistributionClientResult;
import org.onap.sdc.utils.DistributionActionResultEnum;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DistributionClientStartupConfigTest {

  @Mock IDistributionClient distributionClient;
  @Mock ModelLoaderConfig config;
  @Mock EventCallback eventCallback;
  @InjectMocks DistributionClientStartupConfig startupConfig;

  @Test
  public void thatClientIsInitialized() {
    IDistributionClientResult initResult = mock(IDistributionClientResult.class);
    when(distributionClient.init(any(), any())).thenReturn(initResult);
    when(distributionClient.start()).thenReturn(initResult);
    when(initResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.SUCCESS);
    startupConfig.initSdcClient();
    verify(distributionClient, times(1)).init(any(), any());
    verify(distributionClient, times(1)).start();
  }

  @Test
  public void thatClientIsStoppedOnPreDestroy() {
    startupConfig.destroy();
    verify(distributionClient, times(1)).stop();
  }
  
}
