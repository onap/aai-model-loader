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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.notification.ArtifactDownloadManager;
import org.onap.aai.modelloader.notification.EventCallback;
import org.onap.aai.modelloader.notification.NotificationDataImpl;
import org.onap.aai.modelloader.notification.NotificationPublisher;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.results.IDistributionClientResult;
import org.onap.sdc.impl.DistributionClientFactory;
import org.onap.sdc.utils.DistributionActionResultEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service class in charge of managing the negotiating model loading capabilities between AAI and an ASDC.
 */
@RestController
@RequestMapping("/services/model-loader/v1/model-service")
public class ModelLoaderService implements ModelLoaderInterface {

    private static Logger logger = LoggerFactory.getInstance().getLogger(ModelLoaderService.class.getName());

    @Value("${CONFIG_HOME}")
    private String configDir;
    private IDistributionClient client;
    private ModelLoaderConfig config;
    @Autowired
    private BabelServiceClientFactory babelClientFactory;

    /**
     * Responsible for loading configuration files and calling initialization.
     */
    @PostConstruct
    protected void start() {
        // Load model loader system configuration
        logger.info(ModelLoaderMsgs.LOADING_CONFIGURATION);
        ModelLoaderConfig.setConfigHome(configDir);
        Properties configProperties = new Properties();
        try (InputStream configInputStream = Files.newInputStream(Paths.get(configDir, "model-loader.properties"))) {
            configProperties.load(configInputStream);
            config = new ModelLoaderConfig(configProperties);
            
            // Set the truststore for SDC Client to connect to Dmaap central bus if applicable (as in case of TI)
            if (Boolean.TRUE.equals(config.isUseHttpsWithDmaap())) {
                String trustStorePath = config.getKeyStorePath();
                String trustStorePassword = config.getKeyStorePassword();
                if (trustStorePath != null && Paths.get(trustStorePath).toFile().isFile() && trustStorePassword != null
                        && !trustStorePassword.isEmpty()) {
                    System.setProperty("javax.net.ssl.trustStore", trustStorePath);
                    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
                } else {
                    throw new IllegalArgumentException("Model Loader property ml.distribution.KEYSTORE_FILE "
                    		+ "or ml.distribution.KEYSTORE_PASSWORD not set or invalid");
                }
            }
            
            if (!config.getASDCConnectionDisabled()) {
                initSdcClient();
            }
        } catch (IOException e) {
            String errorMsg = "Failed to load configuration: " + e.getMessage();
            logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);
        }
    }

    /**
     * Responsible for stopping the connection to the distribution client before the resource is destroyed.
     */
    public void preShutdownOperations() {
        logger.info(ModelLoaderMsgs.STOPPING_CLIENT);
        if (client != null) {
            client.stop();
        }
    }

    /**
     * Responsible for loading configuration files, initializing model distribution clients, and starting them.
     */
    protected void initSdcClient() {
        // Initialize distribution client
        logger.debug(ModelLoaderMsgs.INITIALIZING, "Initializing distribution client...");
        client = DistributionClientFactory.createDistributionClient();
        EventCallback callback = new EventCallback(client, config, babelClientFactory);

        IDistributionClientResult initResult = client.init(config, callback);

        if (initResult.getDistributionActionResult() == DistributionActionResultEnum.SUCCESS) {
            // Start distribution client
            logger.debug(ModelLoaderMsgs.INITIALIZING, "Starting distribution client...");
            IDistributionClientResult startResult = client.start();
            if (startResult.getDistributionActionResult() == DistributionActionResultEnum.SUCCESS) {
                logger.info(ModelLoaderMsgs.INITIALIZING, "Connection to SDC established");
            } else {
                String errorMsg = "Failed to start distribution client: " + startResult.getDistributionMessageResult();
                logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);

                // Kick off a timer to retry the SDC connection
                Timer timer = new Timer();
                TimerTask task = new SdcConnectionJob(client, config, callback, timer);
                timer.schedule(task, new Date(), 60000);
            }
        } else {
            String errorMsg = "Failed to initialize distribution client: " + initResult.getDistributionMessageResult();
            logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);

            // Kick off a timer to retry the SDC connection
            Timer timer = new Timer();
            TimerTask task = new SdcConnectionJob(client, config, callback, timer);
            timer.schedule(task, new Date(), 60000);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::preShutdownOperations));
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

            new ArtifactDownloadManager(client, config, babelClientFactory).processToscaArtifacts(modelArtifacts,
                    catalogArtifacts, csarFile, artifactInfo, "test-transaction-id", modelVersion);

            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Loading xml models from test artifacts: "
                    + modelArtifacts.size() + " model(s) and " + catalogArtifacts.size() + " catalog(s)");

            NotificationDataImpl notificationData = new NotificationDataImpl();
            notificationData.setDistributionID("TestDistributionID");
            boolean success =
                    new ArtifactDeploymentManager(config).deploy(notificationData, modelArtifacts, catalogArtifacts);
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
