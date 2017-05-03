/**
 * ============LICENSE_START=======================================================
 * Model Loader
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP and OpenECOMP are trademarks
 * and service marks of AT&T Intellectual Property.
 */
package org.openecomp.modelloader.notification;

import org.openecomp.sdc.api.consumer.IDistributionStatusMessage;
import org.openecomp.sdc.utils.DistributionStatusEnum;

public class DistributionStatusMsg implements IDistributionStatusMessage {
  private DistributionStatusEnum status;
  private String distributionId;
  private String consumerId;
  private String artifactUrl;

  /**
   * Creates a new DistributionStatusMsg instance.
   * 
   * @param status         - The distribution status to be reported.
   * @param distributionId - The identifier of the distribution who's status is being rported on.
   * @param consumerId     - Identifier of the consumer associated with the distribution.
   * @param artifactUrl    - Resource identifier for the artifact.
   */
  public DistributionStatusMsg(DistributionStatusEnum status, 
                               String distributionId,
                               String consumerId, 
                               String artifactUrl) {
    this.status = status;
    this.distributionId = distributionId;
    this.consumerId = consumerId;
    this.artifactUrl = artifactUrl;
  }

  @Override
  public long getTimestamp() {
    long currentTimeMillis = System.currentTimeMillis();
    return currentTimeMillis;
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
  public String getArtifactURL() {
    return artifactUrl;
  }
}
