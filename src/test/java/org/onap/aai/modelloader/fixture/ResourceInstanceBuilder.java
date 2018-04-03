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
package org.onap.aai.modelloader.fixture;

import java.util.List;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.IResourceInstance;

/**
 * This class builds an instance of IArtifactInfo for test purposes.
 */
class ResourceInstanceBuilder {

    /**
     * Builds an implementation of IResourceInstance for test purposes.
     *
     * @param artifacts collection of artifacts that make up the resource
     * @return IResourceInstance implementation of IResourceInstance for test purposes
     */
    static IResourceInstance build(final List<IArtifactInfo> artifacts) {
        IResourceInstance instance = new TestResourceInstanceImpl();

        ((TestResourceInstanceImpl) instance).setArtifacts(artifacts);

        return instance;
    }
}
