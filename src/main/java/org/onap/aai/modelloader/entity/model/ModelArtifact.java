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

import java.io.StringWriter;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.restclient.client.OperationResult;
import org.w3c.dom.Node;


public class ModelArtifact extends AbstractModelArtifact {

    private static final String AAI_MODEL_VER_SUB_URL = "/model-vers/model-ver";

    private static final String FAILURE_MSG_PREFIX = "Ingestion failed for ";
    private static final String ROLLBACK_MSG_SUFFIX = ". Rolling back distribution.";


    private String modelVerId;
    private String modelInvariantId;
    private Node modelVer;
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

    public Node getModelVer() {
        return modelVer;
    }

    public void setModelVer(Node modelVer) {
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
        OperationResult getResponse = getResourceModel(aaiClient, distId, xmlResourceUrl);
        return getResponse != null && getResponse.getResultCode() == Response.Status.OK.getStatusCode();
    }

    /**
     * Test whether the specified resource (URL) can be requested successfully
     *
     * @param aaiClient
     * @param distId
     * @param xmlResourceUrl
     * @return OperationResult the result of the operation
     */
    private OperationResult getResourceModel(AaiRestClient aaiClient, String distId, String xmlResourceUrl) {
        return aaiClient.getResource(xmlResourceUrl, distId, MediaType.APPLICATION_XML_TYPE);
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
        OperationResult putResponse =
                aaiClient.putResource(resourceUrl, payload, distId, MediaType.APPLICATION_XML_TYPE);
        return putResponse != null && putResponse.getResultCode() == Response.Status.CREATED.getStatusCode();
    }

    @Override
    public boolean push(AaiRestClient aaiClient, ModelLoaderConfig config, String distId,
            List<Artifact> completedArtifacts) {
        if (config.useGizmo()) {
            return pushToGizmo(aaiClient, config, distId);
        }

        return pushToResources(aaiClient, config, distId, completedArtifacts);
    }

    private boolean pushToResources(AaiRestClient aaiClient, ModelLoaderConfig config, String distId,
            List<Artifact> completedArtifacts) {
        boolean success = false;

        // See whether the model is already present
        String resourceUrl = getModelUrl(config);
        OperationResult result = getResourceModel(aaiClient, distId, resourceUrl);

        if (result != null) {
            if (result.getResultCode() == Response.Status.OK.getStatusCode()) {
                success = updateExistingModel(aaiClient, config, distId, completedArtifacts);
            } else if (result.getResultCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                success = createNewModel(aaiClient, distId, completedArtifacts, resourceUrl);
            } else {
                logModelUpdateFailure(
                        "Response code " + result.getResultCode() + " invalid for getting resource model");
            }
        } else {
            logModelUpdateFailure("Null response from RestClient");
        }

        return success;
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

    private boolean updateExistingModel(AaiRestClient aaiClient, ModelLoaderConfig config, String distId,
            List<Artifact> completedArtifacts) {
        boolean success;
        logInfoMsg(getType() + " " + getModelInvariantId() + " already exists.  Skipping ingestion.");
        success = pushModelVersion(aaiClient, config, distId, completedArtifacts);
        return success;
    }

    /**
     * @param aaiClient
     * @param config
     * @param distId
     * @param completedArtifacts
     * @return
     */
    private boolean pushModelVersion(AaiRestClient aaiClient, ModelLoaderConfig config, String distId,
            List<Artifact> completedArtifacts) {
        if (xmlResourceCanBeFetched(aaiClient, distId, getModelVerUrl(config))) {
            logInfoMsg(getType() + " " + getUniqueIdentifier() + " already exists.  Skipping ingestion.");
            return true;
        }

        // Load the model version
        boolean success = true;
        try {
            success = putXmlResource(aaiClient, distId, getModelVerUrl(config), nodeToString(getModelVer()));
            if (success) {
                completedArtifacts.add(this);
                logInfoMsg(getType() + " " + getUniqueIdentifier() + " successfully ingested.");
            } else {
                logModelUpdateFailure("Error pushing model");
            }
        } catch (TransformerException e) {
            logModelUpdateFailure(e.getMessage());
            success = false;
        }

        return success;
    }


    @Override
    public void rollbackModel(AaiRestClient aaiClient, ModelLoaderConfig config, String distId) {
        // Gizmo is resilient and doesn't require a rollback. A redistribution will work fine even if
        // the model is partially loaded.
        if (config.useGizmo()) {
            return;
        }

        String url = getModelVerUrl(config);
        if (firstVersionOfModel) {
            // If this was the first version of the model which was added, we want to remove the entire
            // model rather than just the version.
            url = getModelUrl(config);
        }

        // Best effort to delete. Nothing we can do in the event this fails.
        aaiClient.getAndDeleteResource(url, distId);
    }

    private String getModelUrl(ModelLoaderConfig config) {
        String baseURL = config.getAaiBaseUrl().trim();
        String subURL = config.getAaiModelUrl(getModelNamespaceVersion()).trim();
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

    private String getModelVerUrl(ModelLoaderConfig config) {
        String baseURL = config.getAaiBaseUrl().trim();
        String subURL = config.getAaiModelUrl(getModelNamespaceVersion()).trim() + getModelInvariantId()
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

    private String nodeToString(Node node) throws TransformerException {
        StringWriter sw = new StringWriter();
        TransformerFactory transFact = TransformerFactory.newInstance();
        transFact.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transFact.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        Transformer t = transFact.newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));
        return sw.toString();
    }
}
