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

import java.util.Collections;
import java.util.List;
import org.onap.sdc.api.notification.IArtifactInfo;

/**
 * This class is an implementation of IArtifactInfo for test purposes.
 */
public class ArtifactInfoImpl implements IArtifactInfo {

    private String artifactName;
    private String artifactType;
    private String artifactDescription;
    private String artifactVersion;

    @Override
    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    @Override
    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    @Override
    public String getArtifactURL() {
        return null;
    }

    @Override
    public String getArtifactChecksum() {
        return null;
    }

    @Override
    public String getArtifactDescription() {
        return artifactDescription;
    }

    public void setArtifactDescription(String artifactDescription) {
        this.artifactDescription = artifactDescription;
    }

    @Override
    public Integer getArtifactTimeout() {
        return null;
    }

    @Override
    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    @Override
    public String getArtifactUUID() {
        return null;
    }

    @Override
    public IArtifactInfo getGeneratedArtifact() {
        return null;
    }

    @Override
    public List<IArtifactInfo> getRelatedArtifacts() {
        return Collections.emptyList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtifactInfoImpl that = (ArtifactInfoImpl) o;

        if (artifactName != null ? !artifactName.equals(that.artifactName) : that.artifactName != null) {
            return false;
        }
        if (artifactType != null ? !artifactType.equals(that.artifactType) : that.artifactType != null) {
            return false;
        }
        if (artifactDescription != null ? !artifactDescription.equals(that.artifactDescription)
                : that.artifactDescription != null) {
            return false;
        }
        return artifactVersion != null ? artifactVersion.equals(that.artifactVersion) : that.artifactVersion == null;
    }

    @Override
    public int hashCode() {
        int result = artifactName != null ? artifactName.hashCode() : 0;
        result = 31 * result + (artifactType != null ? artifactType.hashCode() : 0);
        result = 31 * result + (artifactDescription != null ? artifactDescription.hashCode() : 0);
        result = 31 * result + (artifactVersion != null ? artifactVersion.hashCode() : 0);
        return result;
    }
}
