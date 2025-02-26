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
package org.onap.aai.modelloader.service;

import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.notification.EventCallback;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.results.IDistributionClientResult;
import org.onap.sdc.utils.DistributionActionResultEnum;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Timer;

@SpringBootTest
@TestPropertySource(properties = {"CONFIG_HOME=src/test/resources",})
public class TestSdcConnectionJob {

    @Mock
    private IDistributionClient client;

    @Mock
    private ModelLoaderConfig config;

    @Mock
    private EventCallback callback;

    @Mock
    private Timer timer;

    @Mock
    private IDistributionClientResult clientResult;

    @Mock
    private Logger logger;

    private SdcConnectionJob connectionJob;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        LoggerFactory loggerFactorySpy = mock(LoggerFactory.class);
        when(loggerFactorySpy.getLogger(SdcConnectionJob.class.getName())).thenReturn(logger);
        connectionJob = new SdcConnectionJob(client, config, callback, timer);
    }

    @Test
    public void testRunWhenASDCConnectionDisabled() {
        when(config.getASDCConnectionDisabled()).thenReturn(true);
        connectionJob.run();
        verify(client, never()).init(any(), any());
        verify(client, never()).start();
        verify(config, times(1)).getASDCConnectionDisabled();
    }

    @Test
    public void testRunInitializationFails() {
        when(config.getASDCConnectionDisabled()).thenReturn(false);
        when(client.init(config, callback)).thenReturn(clientResult);
        when(clientResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.FAIL);
        connectionJob.run();
        verify(client).init(config, callback);
        verify(client, never()).start();
        verify(config, times(1)).getASDCConnectionDisabled();
    }

    @Test
    public void testRunInitializationSucceedsButStartFails() {
        when(config.getASDCConnectionDisabled()).thenReturn(false);
        when(client.init(config, callback)).thenReturn(clientResult);
        when(clientResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.SUCCESS);

        IDistributionClientResult startResult = mock(IDistributionClientResult.class);
        when(client.start()).thenReturn(startResult);
        when(startResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.FAIL);

        connectionJob.run();
        verify(client,times(1)).start();
        verify(timer, never()).cancel();
        verify(config, times(1)).getASDCConnectionDisabled();
    }

    @Test
    public void testRunInitializationAndStartBothSucceed() {
        when(config.getASDCConnectionDisabled()).thenReturn(false);
        when(client.init(config, callback)).thenReturn(clientResult);
        when(clientResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.SUCCESS);

        IDistributionClientResult startResult = mock(IDistributionClientResult.class);
        when(client.start()).thenReturn(startResult);
        when(startResult.getDistributionActionResult()).thenReturn(DistributionActionResultEnum.SUCCESS);

        connectionJob.run();
        verify(client).init(config, callback);
        verify(client,times(1)).start();
        verify(timer).cancel();
    }

}
