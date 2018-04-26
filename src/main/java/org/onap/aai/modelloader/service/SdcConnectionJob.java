/**
 * ﻿============LICENSE_START=======================================================
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
package org.onap.aai.modelloader.service;

import java.util.Timer;
import java.util.TimerTask;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.notification.EventCallback;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.results.IDistributionClientResult;
import org.onap.sdc.utils.DistributionActionResultEnum;

public class SdcConnectionJob extends TimerTask {
    static Logger logger = LoggerFactory.getInstance().getLogger(SdcConnectionJob.class.getName());

    private IDistributionClient client;
    private ModelLoaderConfig config;
    private EventCallback callback;
    private Timer timer;

    public SdcConnectionJob(IDistributionClient client, ModelLoaderConfig config, EventCallback callback, Timer timer) {
        this.client = client;
        this.timer = timer;
        this.callback = callback;
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.getASDCConnectionDisabled()) {

            IDistributionClientResult initResult = client.init(config, callback);

            if (initResult.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
                String errorMsg =
                        "Failed to initialize distribution client: " + initResult.getDistributionMessageResult();
                logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);
                return;
            }

            IDistributionClientResult startResult = client.start();
            if (startResult.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
                String errorMsg = "Failed to start distribution client: " + startResult.getDistributionMessageResult();
                logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);
                return;
            }

            // Success. Cancel the timer job
            timer.cancel();
            logger.info(ModelLoaderMsgs.INITIALIZING, "Connection to SDC established");
        }
    }
}
