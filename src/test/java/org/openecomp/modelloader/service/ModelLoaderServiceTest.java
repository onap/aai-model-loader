/*-
 * ============LICENSE_START=======================================================
 * MODEL LOADER SERVICE
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.modelloader.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.openecomp.modelloader.config.ModelLoaderConfig;
import org.openecomp.modelloader.notification.EventCallback;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.results.IDistributionClientResult;
import org.openecomp.sdc.impl.DistributionClientFactory;
import org.openecomp.sdc.utils.DistributionActionResultEnum;

@PrepareForTest({ DistributionClientFactory.class })
@RunWith(PowerMockRunner.class)
public class ModelLoaderServiceTest {

  /*
   * //TODO this should be re-added once we come up with a strategy to fail
   * gracefully
   * 
   * @Test public void testNonExistentConfiguration(){
   * ModelLoaderService.CONFIG_LOCATION = "FAKELOCATION";
   * 
   * try{ new ModelLoaderService().start(); }catch(RuntimeException e){
   * assertTrue("Got unexpected message from error log",
   * e.getMessage().contains("Failed to load configuration")); return; }
   * 
   * fail("Expecting runtime exception"); }
   */

  @Test
  public void testConfigureStartDistributionClient() {
    PowerMockito.mockStatic(DistributionClientFactory.class);

    IDistributionClient mockClient = mock(IDistributionClient.class);
    ModelLoaderConfig mockConfig = mock(ModelLoaderConfig.class);

    when(DistributionClientFactory.createDistributionClient()).thenReturn(mockClient);

    IDistributionClientResult result = mock(IDistributionClientResult.class);

    when(result.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.SUCCESS);
    when(mockClient.init(Matchers.<ModelLoaderConfig> any(), Matchers.<EventCallback> any()))
        .thenReturn(result);
    when(mockClient.start()).thenReturn(result);

    new ModelLoaderService().init();

    // Validate that the client was initialized and started
    verify(mockClient, times(1)).init(Matchers.<ModelLoaderConfig> any(),
        Matchers.<EventCallback> any());
    verify(mockClient, times(1)).start();
  }

  @Test
  public void testInitializeButNotStarted() {
    PowerMockito.mockStatic(DistributionClientFactory.class);

    IDistributionClient mockClient = mock(IDistributionClient.class);
    ModelLoaderConfig mockConfig = mock(ModelLoaderConfig.class);

    DistributionActionResultEnum failureReason = DistributionActionResultEnum.ASDC_CONNECTION_FAILED;

    when(DistributionClientFactory.createDistributionClient()).thenReturn(mockClient);

    IDistributionClientResult initResult = mock(IDistributionClientResult.class);
    when(initResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.SUCCESS);
    IDistributionClientResult startResult = mock(IDistributionClientResult.class);
    when(startResult.getDistributionActionResult()).thenReturn(failureReason);

    when(mockClient.init(Matchers.<ModelLoaderConfig> any(), Matchers.<EventCallback> any()))
        .thenReturn(initResult);
    when(mockClient.start()).thenReturn(startResult);

    // TODO this should be re-added once we come up with a strategy to fail
    // gracefully
    /*
     * try{ new ModelLoaderService().init(mockConfig); }catch(RuntimeException
     * e){ assertTrue(e.getMessage().contains(failureReason.toString()));
     * return; }
     * 
     * fail("Expecting runtime exception with failure: " +
     * failureReason.toString());
     */
  }
}
