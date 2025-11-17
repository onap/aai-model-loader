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
package org.onap.aai.modelloader.entity.model;

import java.util.List;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.AaiProperties;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;


public class ModelArtifact extends AbstractModelArtifact {

    private static Logger logger = LoggerFactory.getInstance().getLogger(ModelArtifact.class);

    private static final String AAI_MODEL_VER_SUB_URL = "/model-vers/model-ver";

    private static final String FAILURE_MSG_PREFIX = "Ingestion failed for ";
    private static final String ROLLBACK_MSG_SUFFIX = ". Rolling back distribution.";


    private String modelVerId;
    private String modelInvariantId;
    private String modelVer;
    private boolean firstVersionOfModel = false;

    public ModelArtifact() {
        super(ArtifactType.MODEL);
    }

    public String getModelVerId() {
        return modelVerId;
    }

    public void setModelVerId(String modelVerId) {
        this.modelVerId = modelVerId;
    }

    public String getModelInvariantId() {
        return modelInvariantId;
    }

    public void setModelInvariantId(String modelInvariantId) {
        this.modelInvariantId = modelInvariantId;
    }

    public String getModelVer() {
        return modelVer;
    }

    public void setModelVer(String modelVer) {
        this.modelVer = modelVer;
    }

    @Override
    public String getUniqueIdentifier() {
        return getModelInvariantId() + "|" + getModelVerId();
    }


    /**
     * Test whether the specified resource (URL) can be requested successfully
     *
     * @param aaiClient
     * @param distId
     * @param xmlResourceUrl
     * @return true if a request to GET this resource as XML media is successful (status OK)
     */
    private boolean xmlResourceCanBeFetched(AaiRestClient aaiClient, String distId, String xmlResourceUrl) {
        try {
            ResponseEntity<Model> getResponse = getResourceModel(aaiClient, distId, xmlResourceUrl);
            return getResponse.getStatusCode().equals(HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            if(e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return false;
            } else {
                throw e;
            }
        }
    }

    /**
     * Test whether the specified resource (URL) can be requested successfully
     *
     * @param aaiClient
     * @param distId
     * @param xmlResourceUrl
     * @return OperationResult the result of the operation
     */
    private ResponseEntity<Model> getResourceModel(AaiRestClient aaiClient, String distId, String xmlResourceUrl) {
        return aaiClient.getResource(xmlResourceUrl, distId, MediaType.APPLICATION_XML, Model.class);
    }

    /**
     * PUT the specified XML resource
     *
     * @param aaiClient
     * @param distId
     * @param resourceUrl
     * @param payload
     * @return true if the resource PUT as XML media was successful (status OK)
     */
    private boolean putXmlResource(AaiRestClient aaiClient, String distId, String resourceUrl, String payload) {
        ResponseEntity<String> putResponse = null;
        try {
            putResponse =
                    aaiClient.putResource(resourceUrl, payload, distId, MediaType.APPLICATION_XML, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error(ModelLoaderMsgs.AAI_REST_REQUEST_ERROR, "Error putting resource: " + e.toString());
        }
        return putResponse != null && putResponse.getStatusCode() == HttpStatus.CREATED;
    }

    @Override
    public boolean push(AaiRestClient aaiClient, AaiProperties aaiProperties, String distId,
            List<Artifact> completedArtifacts) {
        if (aaiProperties.isUseGizmo()) {
            return pushToGizmo(aaiClient, aaiProperties, distId);
        }

        return pushToResources(aaiClient, aaiProperties, distId, completedArtifacts);
    }

    private boolean pushToResources(AaiRestClient aaiClient, AaiProperties aaiProperties, String distId,
            List<Artifact> completedArtifacts) {
        boolean success = false;

        // See whether the model is already present
        String resourceUrl = getModelUrl(aaiProperties);
        // ResponseEntity<Model> result;
        boolean modelExists = checkIfModelExists(aaiClient, distId, resourceUrl);

        if(modelExists) {
            success = updateExistingModel(aaiClient, aaiProperties, distId, completedArtifacts);
        } else {
            success = createNewModel(aaiClient, distId, completedArtifacts, resourceUrl);
        }

        return success;
    }

    private boolean checkIfModelExists(AaiRestClient aaiClient, String distId, String resourceUrl) throws HttpClientErrorException {
        try {
            ResponseEntity<Model> response = getResourceModel(aaiClient, distId, resourceUrl);
            return response.getStatusCode().equals(HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            if(e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return false;
            } else {
                throw e;
            }
        }
    }

    private boolean createNewModel(AaiRestClient aaiClient, String distId, List<Artifact> completedArtifacts,
            String resourceUrl) {
        boolean success;
        // Assume that the model does not exist and attempt the PUT
        success = putXmlResource(aaiClient, distId, resourceUrl, getPayload());
        if (success) {
            completedArtifacts.add(this);

            // Record state to remember that this is the first version of the model (just added).
            firstVersionOfModel = true;

            logInfoMsg(getType() + " " + getUniqueIdentifier() + " successfully ingested.");
        } else {
            logModelUpdateFailure("Error creating model. Skipping ingestion.");
        }
        return success;
    }

    private void logModelUpdateFailure(String message) {
        logErrorMsg(FAILURE_MSG_PREFIX + getType() + " " + getUniqueIdentifier() + " " + message + ROLLBACK_MSG_SUFFIX);
    }

    private boolean updateExistingModel(AaiRestClient aaiClient, AaiProperties aaiProperties, String distId,
            List<Artifact> completedArtifacts) {
        boolean success;
        logInfoMsg(getType() + " " + getModelInvariantId() + " already exists.  Skipping ingestion.");
        success = pushModelVersion(aaiClient, aaiProperties, distId, completedArtifacts);
        return success;
    }

    /**
     * @param aaiClient
     * @param aaiProperties
     * @param distId
     * @param completedArtifacts
     * @return
     */
    private boolean pushModelVersion(AaiRestClient aaiClient, AaiProperties aaiProperties, String distId,
            List<Artifact> completedArtifacts) {
        if (xmlResourceCanBeFetched(aaiClient, distId, getModelVerUrl(aaiProperties))) {
            logInfoMsg(getType() + " " + getUniqueIdentifier() + " already exists.  Skipping ingestion.");
            return true;
        }

        // Load the model version
        boolean success = true;
        success = putXmlResource(aaiClient, distId, getModelVerUrl(aaiProperties), getModelVer());
        if (success) {
            completedArtifacts.add(this);
            logInfoMsg(getType() + " " + getUniqueIdentifier() + " successfully ingested.");
        } else {
            logModelUpdateFailure("Error pushing model");
        }

        return success;
    }


    @Override
    public void rollbackModel(AaiRestClient aaiClient, AaiProperties aaiProperties, String distId) {
        // Gizmo is resilient and doesn't require a rollback. A redistribution will work fine even if
        // the model is partially loaded.
        if (aaiProperties.isUseGizmo()) {
            return;
        }

        String url = getModelVerUrl(aaiProperties);
        if (firstVersionOfModel) {
            // If this was the first version of the model which was added, we want to remove the entire
            // model rather than just the version.
            url = getModelUrl(aaiProperties);
        }

        // Best effort to delete. Nothing we can do in the event this fails.
        aaiClient.getAndDeleteResource(url, distId);
    }

    private String getModelUrl(AaiProperties aaiProperties) {
        String baseURL = aaiProperties.getBaseUrl().trim();
        String subURL = aaiProperties.getModelUrl().formatted(getModelNamespaceVersion()).trim();
        String instance = getModelInvariantId();

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

    private String getModelVerUrl(AaiProperties aaiProperties) {
        String baseURL = aaiProperties.getBaseUrl().trim();
        String subURL = aaiProperties.getModelUrl().formatted(getModelNamespaceVersion()).trim() + getModelInvariantId()
                + AAI_MODEL_VER_SUB_URL;
        String instance = getModelVerId();

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
