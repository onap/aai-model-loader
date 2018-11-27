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
package org.onap.aai.modelloader.entity.model;

import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.restclient.AaiRestClient;

import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.restclient.client.OperationResult;


public class NamedQueryArtifact extends AbstractModelArtifact {

    private String namedQueryUuid;

    public NamedQueryArtifact() {
        super(ArtifactType.NAMED_QUERY);
    }

    public String getNamedQueryUuid() {
        return namedQueryUuid;
    }

    public void setNamedQueryUuid(String namedQueryUuid) {
        this.namedQueryUuid = namedQueryUuid;
    }

    @Override
    public String getUniqueIdentifier() {
        return getNamedQueryUuid();
    }

    @Override
    public boolean push(AaiRestClient aaiClient, ModelLoaderConfig config, String distId, List<Artifact> completedArtifacts) {
        if (config.useGizmo()) {
            return pushToGizmo(aaiClient, config, distId);
        }

        return pushToResources(aaiClient, config, distId, completedArtifacts);
    }

    private boolean pushToResources(AaiRestClient aaiClient, ModelLoaderConfig config, String distId,
            List<Artifact> completedArtifacts) {
        OperationResult getResponse =
                aaiClient.getResource(getNamedQueryUrl(config), distId, MediaType.APPLICATION_XML_TYPE);
        if (getResponse == null || getResponse.getResultCode() != Response.Status.OK.getStatusCode()) {
            // Only attempt the PUT if the model doesn't already exist
            OperationResult putResponse = aaiClient.putResource(getNamedQueryUrl(config), getPayload(), distId,
                    MediaType.APPLICATION_XML_TYPE);
            if (putResponse != null && putResponse.getResultCode() == Response.Status.CREATED.getStatusCode()) {
                completedArtifacts.add(this);
                logInfoMsg(getType().toString() + " " + getUniqueIdentifier() + " successfully ingested.");
            } else {
                logErrorMsg("Ingestion failed for " + getType().toString()
                        + " " + getUniqueIdentifier() + ". Rolling back distribution.");
                return false;
            }
        } else {
            logInfoMsg(getType().toString() + " " + getUniqueIdentifier() + " already exists.  Skipping ingestion.");
        }

        return true;
    }

    @Override
    public void rollbackModel(AaiRestClient aaiClient, ModelLoaderConfig config, String distId) {
        // Gizmo is resilient and doesn't require a rollback.  A redistribution will work fine even if 
        // the model is partially loaded.
        if (config.useGizmo()) {
            return;
        }
        
        // Best effort to delete. Nothing we can do in the event this fails.
        aaiClient.getAndDeleteResource(getNamedQueryUrl(config), distId);
    }

    private String getNamedQueryUrl(ModelLoaderConfig config) {
        String baseURL = config.getAaiBaseUrl().trim();
        String subURL = config.getAaiNamedQueryUrl(getModelNamespaceVersion()).trim();
        String instance = this.getNamedQueryUuid();

        if (!baseURL.endsWith("/") && !subURL.startsWith("/")) {
            baseURL = baseURL + "/";
        }

        if (baseURL.endsWith("/") && subURL.startsWith("/")) {
            baseURL = baseURL.substring(0, baseURL.length() - 1);
        }

        if (!subURL.endsWith("/")) {
            subURL = subURL + "/";
        }

        return baseURL + subURL + instance;
    }
}
