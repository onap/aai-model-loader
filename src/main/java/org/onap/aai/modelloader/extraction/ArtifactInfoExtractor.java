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
package org.onap.aai.modelloader.extraction;

import java.util.ArrayList;
import java.util.List;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;

/**
 * This class is responsible for extracting implementations of IArtifactInto from an implementation of
 * INotificationData.
 */
public class ArtifactInfoExtractor {

    /**
     * This method is responsible for extracting a collection of IArtifactInfo objects from a given instance of
     * INotificationData.
     * <p/>
     *
     * @param data an object that may contain instances of IArtifactInfo
     * @return List<IArtifactInfo> instances of IArtifactInfo extracted from the given data
     */
    public List<IArtifactInfo> extract(INotificationData data) {
        List<IArtifactInfo> artifacts = new ArrayList<>();

        if (data != null) {
            if (data.getServiceArtifacts() != null) {
                artifacts.addAll(data.getServiceArtifacts());
            }

            if (data.getResources() != null) {
                for (IResourceInstance resource : data.getResources()) {
                    if (resource.getArtifacts() != null) {
                        artifacts.addAll(resource.getArtifacts());
                    }
                }
            }
        }

        return artifacts;
    }
}
