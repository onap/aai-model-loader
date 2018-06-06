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
package org.onap.aai.modelloader.fixture;

import java.util.List;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;

/**
 * This class is an implementation of INotificationData for test purposes.
 */
public class MockNotificationDataImpl implements INotificationData {

    private String distributionId;
    private List<IResourceInstance> resources;
    private List<IArtifactInfo> serviceArtifacts;

    @Override
    public String getDistributionID() {
        return distributionId;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public String getServiceVersion() {
        return null;
    }

    @Override
    public String getServiceUUID() {
        return null;
    }

    @Override
    public String getServiceDescription() {
        return null;
    }

    @Override
    public List<IResourceInstance> getResources() {
        return resources;
    }

    @Override
    public List<IArtifactInfo> getServiceArtifacts() {
        return serviceArtifacts;
    }

    @Override
    public IArtifactInfo getArtifactMetadataByUUID(String uuid) {
        return null;
    }

    @Override
    public String getServiceInvariantUUID() {
        return null;
    }

    public void setResources(List<IResourceInstance> resources) {
        this.resources = resources;
    }

    public void setServiceArtifacts(List<IArtifactInfo> serviceArtifacts) {
        this.serviceArtifacts = serviceArtifacts;
    }

    public void setDistributionId(String distributionId) {
        this.distributionId = distributionId;
    }

    @Override
    public String getWorkloadContext() {
        return null;
    }

    @Override
    public void setWorkloadContext(String arg0) {}
}
