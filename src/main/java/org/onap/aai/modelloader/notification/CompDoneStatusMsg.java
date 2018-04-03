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

import org.openecomp.sdc.api.consumer.IComponentDoneStatusMessage;
import org.openecomp.sdc.utils.DistributionStatusEnum;

public class CompDoneStatusMsg implements IComponentDoneStatusMessage {

    private DistributionStatusEnum status;
    private String distributionId;
    private String consumerId;

    /**
     * Creates a new DistributionStatusMsg instance.
     *
     * @param status - The distribution status to be reported.
     * @param distributionId - The identifier of the distribution who's status is being rported on.
     * @param consumerId - Identifier of the consumer associated with the distribution.
     */
    public CompDoneStatusMsg(DistributionStatusEnum status, String distributionId, String consumerId) {
        this.status = status;
        this.distributionId = distributionId;
        this.consumerId = consumerId;
    }

    @Override
    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public DistributionStatusEnum getStatus() {
        return status;
    }

    @Override
    public String getDistributionID() {
        return distributionId;
    }

    @Override
    public String getConsumerID() {
        return consumerId;
    }

    @Override
    public String getComponentName() {
      return "AAI";
    }
}
