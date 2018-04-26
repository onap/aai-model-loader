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
package org.onap.aai.modelloader.notification;

import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.consumer.IComponentDoneStatusMessage;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.utils.DistributionStatusEnum;

/**
 * This class is responsible for building an instance of {@link DistributionStatusMsg}.
 */
public class CompDoneStatusMessageBuilder {

    private CompDoneStatusMessageBuilder() {}

    /**
     * Builds an instance of {@link CompDoneStatusMsg} from the given parameters about the status of the distribution of
     * the given artifact.
     *
     * @param client the distribution client this message pertains to
     * @param data data about the notification that resulted in this message being created
     * @param status the status of the distribution of the artifact to be reported
     * @return IComponentDoneStatusMessage implementation of IComponentDoneStatusMessage from the given parameters
     */
    public static IComponentDoneStatusMessage build(IDistributionClient client, INotificationData data,
            DistributionStatusEnum status) {
        return new CompDoneStatusMsg(status, data.getDistributionID(), client.getConfiguration().getConsumerID());
    }
}
