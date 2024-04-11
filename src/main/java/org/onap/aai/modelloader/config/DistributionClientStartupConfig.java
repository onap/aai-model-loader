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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.notification.EventCallback;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.modelloader.service.SdcConnectionJob;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.results.IDistributionClientResult;
import org.onap.sdc.utils.DistributionActionResultEnum;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@ConditionalOnProperty(value = "ml.distribution.connection.enabled", havingValue = "true", matchIfMissing = true)
public class DistributionClientStartupConfig {

    private static final Logger logger = LoggerFactory.getInstance().getLogger(DistributionClientStartupConfig.class);

    private final IDistributionClient client;
    private final ModelLoaderConfig config;
    private final EventCallback eventCallback;

    public DistributionClientStartupConfig(IDistributionClient client, ModelLoaderConfig config,
            EventCallback eventCallback) {
        this.client = client;
        this.config = config;
        this.eventCallback = eventCallback;
    }

    @EventListener(ApplicationReadyEvent.class)
    protected void initSdcClient() {
        // Initialize distribution client
        logger.debug(ModelLoaderMsgs.INITIALIZING, "Initializing distribution client...");
        IDistributionClientResult initResult = client.init(config, eventCallback);

        if (initResult.getDistributionActionResult() == DistributionActionResultEnum.SUCCESS) {
            // Start distribution client
            logger.debug(ModelLoaderMsgs.INITIALIZING, "Starting distribution client...");
            IDistributionClientResult startResult = client.start();
            if (startResult.getDistributionActionResult() == DistributionActionResultEnum.SUCCESS) {
                logger.info(ModelLoaderMsgs.INITIALIZING, "Connection to SDC established");
            } else {
                String errorMsg = "Failed to start distribution client: " + startResult.getDistributionMessageResult();
                logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);

                // Kick off a timer to retry the SDC connection
                Timer timer = new Timer();
                TimerTask task = new SdcConnectionJob(client, config, eventCallback, timer);
                timer.schedule(task, new Date(), 60000);
            }
        } else {
            String errorMsg = "Failed to initialize distribution client: " + initResult.getDistributionMessageResult();
            logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);

            // Kick off a timer to retry the SDC connection
            Timer timer = new Timer();
            TimerTask task = new SdcConnectionJob(client, config, eventCallback, timer);
            timer.schedule(task, new Date(), 60000);
        }
    }
}
