/**
 * ============LICENSE_START=======================================================
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

import java.util.Collections;
import java.util.List;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;

public class NotificationDataImpl implements INotificationData {

    private String distributionID;

    @Override
    public IArtifactInfo getArtifactMetadataByUUID(String arg0) {
        return null;
    }

    @Override
    public String getDistributionID() {
        return distributionID;
    }

    public void setDistributionID(String distributionID) {
        this.distributionID = distributionID;
    }

    @Override
    public List<IResourceInstance> getResources() {
        return Collections.emptyList();
    }

    @Override
    public List<IArtifactInfo> getServiceArtifacts() {
        return Collections.emptyList();
    }

    @Override
    public String getServiceDescription() {
        return null;
    }

    @Override
    public String getServiceInvariantUUID() {
        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public String getServiceUUID() {
        return null;
    }

    @Override
    public String getServiceVersion() {
        return null;
    }

    @Override
    public String getWorkloadContext() {
        return null;
    }

    @Override
    public void setWorkloadContext(String arg0) {
        // Unsupported method - not expected to be called
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((distributionID == null) ? 0 : distributionID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NotificationDataImpl other = (NotificationDataImpl) obj;
        if (distributionID == null) {
            if (other.distributionID != null)
                return false;
        } else if (!distributionID.equals(other.distributionID))
            return false;
        return true;
    }

}
