/*-
 * ============LICENSE_START=======================================================
 * MODEL LOADER SERVICE
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.modelloader.service;

import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.results.IDistributionClientResult;
import org.openecomp.sdc.impl.DistributionClientFactory;
import org.openecomp.sdc.utils.DistributionActionResultEnum;

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.modelloader.config.ModelLoaderConfig;
import org.openecomp.modelloader.entity.model.ModelArtifactHandler;
import org.openecomp.modelloader.notification.EventCallback;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

/**
 * Service class in charge of managing the negotiating model loading
 * capabilities between AAI and an ASDC.
 */
public class ModelLoaderService implements ModelLoaderInterface {

  protected static String CONFIG_FILE = "model-loader.properties";

  private IDistributionClient client;
  private ModelLoaderConfig config;

  static Logger logger = LoggerFactory.getInstance().getLogger(ModelLoaderService.class.getName());

  /**
   * Responsible for loading configuration files and calling initialization.
   */
  @PostConstruct
  protected void start() {
    // Load model loader system configuration
    logger.info(ModelLoaderMsgs.LOADING_CONFIGURATION);
    Properties configProperties = new Properties();
    try {
      configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
    } catch (IOException e) {
      String errorMsg = "Failed to load configuration: " + e.getMessage();
      logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);
      shutdown();
    }
    
    config = new ModelLoaderConfig(configProperties);
    init();
  }

  /**
   * Responsible for stopping the connection to the distribution client before
   * the resource is destroyed.
   */
  @PreDestroy
  protected void preShutdownOperations() {
    logger.info(ModelLoaderMsgs.STOPPING_CLIENT);
    if (client != null) {
      client.stop();
    }
  }

  /**
   * Responsible for loading configuration files, initializing model
   * distribution clients, and starting them.
   */
  protected void init() {
    // Initialize distribution client
    logger.debug(ModelLoaderMsgs.INITIALIZING, "Initializing distribution client...");
    client = DistributionClientFactory.createDistributionClient();
    IDistributionClientResult initResult = client.init(config, new EventCallback(client, config));
    if (initResult.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
      String errorMsg = "Failed to initialize distribution client: "
          + initResult.getDistributionMessageResult();
      logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);
      shutdown();
    }

    // Start distribution client
    logger.debug(ModelLoaderMsgs.INITIALIZING, "Starting distribution client...");
    IDistributionClientResult startResult = client.start();
    if (startResult.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
      String errorMsg = "Failed to start distribution client: "
          + startResult.getDistributionMessageResult();
      logger.error(ModelLoaderMsgs.ASDC_CONNECTION_ERROR, errorMsg);
      shutdown();
    }

    logger.debug(ModelLoaderMsgs.INITIALIZING,
        "Succcessfully loaded service: " + this.getClass().getSimpleName());
  }

  /**
   * Shut down the process.
   */
  private void shutdown() {
    preShutdownOperations();

    // TODO: Find a better way to shut down the model loader.
    try {
      // Give logs time to write to file
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // Nothing we can do at this point
    }

    Runtime.getRuntime().halt(1);
  }

  /** (non-Javadoc)
   * @see org.openecomp.modelloader.service.ModelLoaderInterface#loadModel(java.lang.String)
   */
  @Override
  public Response loadModel(String modelid) {
    Response response = Response.ok("{\"model_loaded\":\"" + modelid + "\"}").build();

    return response;
  }

  /** (non-Javadoc)
   * @see org.openecomp.modelloader.service.ModelLoaderInterface#saveModel(java.lang.String, java.lang.String)
   */
  @Override
  public Response saveModel(String modelid, String modelname) {
    Response response = Response.ok("{\"model_saved\":\"" + modelid + "-" + modelname + "\"}")
        .build();

    return response;
  }

  @Override
  public Response ingestModel(String modelid, HttpServletRequest req, String payload)
      throws IOException {
    Response response;

    if (config.getIngestSimulatorEnabled()) {
      logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Received test artifact");

      ModelArtifactHandler handler = new ModelArtifactHandler(config);
      handler.loadModelTest(payload.getBytes());

      response = Response.ok().build();
    } else {
      logger.debug("Simulation interface disabled");
      response = Response.serverError().build();
    }

    return response;
  }
}
