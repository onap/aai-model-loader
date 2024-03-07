/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
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

package org.onap.aai.modelloader.entity.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.modelloader.util.GizmoTranslator;
import org.onap.aai.restclient.client.OperationResult;
import org.springframework.http.HttpStatus;

public abstract class AbstractModelArtifact extends Artifact implements IModelArtifact {

    private static Logger logger = LoggerFactory.getInstance().getLogger(AbstractModelArtifact.class.getName());

    private String modelNamespace;
    private String modelNamespaceVersion;
    private Set<String> referencedModelIds = new HashSet<>();

    protected AbstractModelArtifact(ArtifactType type) {
        super(type);
    }

    public Set<String> getDependentModelIds() {
        return referencedModelIds;
    }

    @Override
    public void addDependentModelId(String dependentModelId) {
        this.referencedModelIds.add(dependentModelId);
    }

    public String getModelNamespace() {
        return modelNamespace;
    }

    @Override
    public void setModelNamespace(String modelNamespace) {
        this.modelNamespace = modelNamespace;

        // Get the version from the namespace (in format 'http://org.onap.aai.inventory/v14')
        String[] parts = modelNamespace.split("/");
        modelNamespaceVersion = parts[parts.length - 1].trim();
    }

    public String getModelNamespaceVersion() {
        return modelNamespaceVersion;
    }

    public abstract String getUniqueIdentifier();

    public abstract boolean push(AaiRestClient aaiClient, ModelLoaderConfig config, String distId,
            List<Artifact> completedArtifacts);

    public abstract void rollbackModel(AaiRestClient aaiClient, ModelLoaderConfig config, String distId);

    protected boolean pushToGizmo(AaiRestClient aaiClient, ModelLoaderConfig config, String distId) {
        try {
            String gizmoPayload = GizmoTranslator.translate(getPayload());
            OperationResult postResponse = aaiClient.postResource(config.getAaiProperties().getBaseUrl().trim(), gizmoPayload, distId,
                    MediaType.APPLICATION_JSON_TYPE);

            if (postResponse.getResultCode() != HttpStatus.OK.value()) {
                return false;
            }
        } catch (IOException e) {
            logErrorMsg("Ingest failed for " + getType() + " " + getUniqueIdentifier() + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    protected void logInfoMsg(String infoMsg) {
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, infoMsg);
    }

    protected void logErrorMsg(String errorMsg) {
        logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, errorMsg);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("Type=").append(getType()) //
                .append("\n").append("Id=").append(getUniqueIdentifier()) //
                .append("\n").append("Version=").append(getModelNamespaceVersion());

        sb.append("\n").append("Dependant models: ");
        referencedModelIds.forEach(dep -> sb.append(dep).append("  "));

        return sb.toString();
    }

}
