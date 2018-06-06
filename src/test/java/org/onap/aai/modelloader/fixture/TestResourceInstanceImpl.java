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
import org.onap.sdc.api.notification.IResourceInstance;

/**
 * This class is an implementation of IResourceInstance for test purposes.
 */
public class TestResourceInstanceImpl implements IResourceInstance {

    private List<IArtifactInfo> artifacts;

    @Override
    public String getResourceInstanceName() {
        return null;
    }

    @Override
    public String getResourceName() {
        return null;
    }

    @Override
    public String getResourceVersion() {
        return null;
    }

    @Override
    public String getResourceType() {
        return null;
    }

    @Override
    public String getResourceUUID() {
        return null;
    }

    @Override
    public List<IArtifactInfo> getArtifacts() {
        return artifacts;
    }

    @Override
    public String getResourceInvariantUUID() {
        return null;
    }

    @Override
    public String getResourceCustomizationUUID() {
        return null;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getSubcategory() {
        return null;
    }

    void setArtifacts(List<IArtifactInfo> artifacts) {
        this.artifacts = artifacts;
    }
}
