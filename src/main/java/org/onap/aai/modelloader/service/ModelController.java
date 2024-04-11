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
package org.onap.aai.modelloader.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.notification.ArtifactDownloadManager;
import org.onap.aai.modelloader.notification.NotificationDataImpl;
import org.onap.aai.modelloader.notification.NotificationPublisher;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service class in charge of managing the negotiating model loading capabilities between AAI and an ASDC.
 */
@RestController
@RequestMapping("/services/model-loader/v1/model-service")
public class ModelController implements ModelLoaderInterface {

    private static final Logger logger = LoggerFactory.getInstance().getLogger(ModelController.class);

    private final IDistributionClient client;
    private final ModelLoaderConfig config;
    private final ArtifactDeploymentManager artifactDeploymentManager;
    private final ArtifactDownloadManager artifactDownloadManager;

    public ModelController(IDistributionClient client, ModelLoaderConfig config, ArtifactDeploymentManager artifactDeploymentManager, ArtifactDownloadManager artifactDownloadManager) {
        this.client = client;
        this.config = config;
        this.artifactDeploymentManager = artifactDeploymentManager;
        this.artifactDownloadManager = artifactDownloadManager;
    }

    /**
     * (non-Javadoc)
     *
     * @see org.onap.aai.modelloader.service.ModelLoaderInterface#loadModel(java.lang.String)
     */
    @Override
    public Response loadModel(@PathVariable String modelid) {
        return Response.ok("{\"model_loaded\":\"" + modelid + "\"}").build();
    }

    /**
     * (non-Javadoc)
     *
     * @see org.onap.aai.modelloader.service.ModelLoaderInterface#saveModel(java.lang.String, java.lang.String)
     */
    @Override
    public Response saveModel(@PathVariable String modelid, @PathVariable String modelname) {
        return Response.ok("{\"model_saved\":\"" + modelid + "-" + modelname + "\"}").build();
    }

    @Override
    public Response ingestModel(@PathVariable String modelName, @PathVariable String modelVersion,
            @RequestBody String payload) throws IOException {
        Response response;

        if (config.getIngestSimulatorEnabled()) {
            response = processTestArtifact(modelName, modelVersion, payload);
        } else {
            logger.debug("Simulation interface disabled");
            response = Response.serverError().build();
        }

        return response;
    }

    private Response processTestArtifact(String modelName, String modelVersion, String payload) {
        IArtifactInfo artifactInfo = new ArtifactInfoImpl();
        ((ArtifactInfoImpl) artifactInfo).setArtifactName(modelName);
        ((ArtifactInfoImpl) artifactInfo).setArtifactVersion(modelVersion);

        Response response;
        try {
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Received test artifact " + modelName + " " + modelVersion);

            byte[] csarFile = Base64.getDecoder().decode(payload);

            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Generating XML models from test artifact");

            List<Artifact> modelArtifacts = new ArrayList<>();
            List<Artifact> catalogArtifacts = new ArrayList<>();

            artifactDownloadManager.processToscaArtifacts(modelArtifacts,
                    catalogArtifacts, csarFile, artifactInfo, "test-transaction-id", modelVersion);

            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Loading xml models from test artifacts: "
                    + modelArtifacts.size() + " model(s) and " + catalogArtifacts.size() + " catalog(s)");

            NotificationDataImpl notificationData = new NotificationDataImpl();
            notificationData.setDistributionID("TestDistributionID");
            boolean success =
                    artifactDeploymentManager.deploy(notificationData, modelArtifacts, catalogArtifacts);
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Deployment success was " + success);
            response = success ? Response.ok().build() : Response.serverError().build();
        } catch (Exception e) {
            String responseMessage = e.getMessage();
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Exception handled: " + responseMessage);
            if (config.getASDCConnectionDisabled()) {
                // Make sure the NotificationPublisher logger is invoked as per the standard processing flow.
                new NotificationPublisher().publishDeployFailure(client, new NotificationDataImpl(), artifactInfo);
            } else {
                responseMessage += "\nSDC publishing is enabled but has been bypassed";
            }
            response = Response.serverError().entity(responseMessage).type(MediaType.APPLICATION_XML).build();
        }
        return response;
    }
}
