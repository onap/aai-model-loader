/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.modelloader.notification;

import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.consumer.IDistributionStatusMessage;
import org.openecomp.sdc.api.consumer.INotificationCallback;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.INotificationData;
import org.openecomp.sdc.api.notification.IResourceInstance;
import org.openecomp.sdc.api.results.IDistributionClientDownloadResult;
import org.openecomp.sdc.api.results.IDistributionClientResult;
import org.openecomp.sdc.utils.ArtifactTypeEnum;
import org.openecomp.sdc.utils.DistributionActionResultEnum;
import org.openecomp.sdc.utils.DistributionStatusEnum;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifactHandler;
import org.onap.aai.modelloader.entity.model.IModelParser;
import org.onap.aai.modelloader.entity.model.ModelArtifactHandler;
import org.onap.aai.modelloader.entity.model.ModelParserFactory;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.cl.mdc.MdcContext;
import org.openecomp.cl.mdc.MdcOverride;
import org.slf4j.MDC;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class EventCallback implements INotificationCallback {

  private IDistributionClient client;
  private ModelLoaderConfig config;
  private static Logger logger = LoggerFactory.getInstance()
      .getLogger(EventCallback.class.getName());
  private static Logger auditLogger = LoggerFactory.getInstance()
      .getAuditLogger(EventCallback.class.getName());
  private static Logger metricsLogger = LoggerFactory.getInstance()
      .getMetricsLogger(EventCallback.class.getName());

  private static SimpleDateFormat dateFormatter = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  public EventCallback(IDistributionClient client, ModelLoaderConfig config) {
    this.client = client;
    this.config = config;
  }

  @Override
  public void activateCallback(INotificationData data) {
    // Init MDC
    MdcContext.initialize(data.getDistributionID(), "ModelLoader", "", "Event-Bus", "");

    logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
        "Received distribution " + data.getDistributionID());

    boolean success = true;
    List<IArtifactInfo> artifacts = getArtifacts(data);
    List<Artifact> modelArtifacts = new ArrayList<Artifact>();
    List<Artifact> catalogArtifacts = new ArrayList<Artifact>();

    for (IArtifactInfo artifact : artifacts) {
      // Grab the current time so we can measure the download time for the
      // metrics log
      long startTimeInMs = System.currentTimeMillis();
      MdcOverride override = new MdcOverride();
      override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

      // Download Artifact
      IDistributionClientDownloadResult downloadResult = client.download(artifact);

      // Generate metrics log
      metricsLogger.info(ModelLoaderMsgs.DOWNLOAD_COMPLETE, null, override,
          artifact.getArtifactName(), downloadResult.getDistributionActionResult().toString());

      if (downloadResult.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
        publishDownloadFailure(data, artifact, downloadResult.getDistributionMessageResult());
        success = false;
        break;
      }

      logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
    	        "Downloaded artifact: " + artifact.getArtifactName() + "  Payload:\n" + new String(downloadResult.getArtifactPayload()));
      
      publishDownloadSuccess(data, artifact, downloadResult);

      if ((artifact.getArtifactType().compareToIgnoreCase(ArtifactTypeEnum.MODEL_INVENTORY_PROFILE.toString()) == 0)
          || (artifact.getArtifactType().compareToIgnoreCase(ArtifactTypeEnum.MODEL_QUERY_SPEC.toString()) == 0)) {
        IModelParser parser = ModelParserFactory.createModelParser(downloadResult.getArtifactPayload(), downloadResult.getArtifactName());
        List<Artifact> parsedArtifacts = parser.parse(downloadResult.getArtifactPayload(), downloadResult.getArtifactName());
        if (parsedArtifacts != null && !parsedArtifacts.isEmpty()) {
          modelArtifacts.addAll(parsedArtifacts);
        } else {
          success = false;
          publishDeployFailure(data, artifact);
          break;
        }
      } else if (artifact.getArtifactType()
          .compareToIgnoreCase(ArtifactTypeEnum.VNF_CATALOG.toString()) == 0) {
        catalogArtifacts
            .add(new VnfCatalogArtifact(new String(downloadResult.getArtifactPayload())));
      }
    }

    String statusString = "SUCCESS";
    if (success) {
      ModelArtifactHandler modelHandler = new ModelArtifactHandler(config);
      boolean modelDeploySuccess = modelHandler.pushArtifacts(modelArtifacts,
          data.getDistributionID());

      VnfCatalogArtifactHandler catalogHandler = new VnfCatalogArtifactHandler(config);
      boolean catalogDeploySuccess = catalogHandler.pushArtifacts(catalogArtifacts,
          data.getDistributionID());

      for (IArtifactInfo artifact : artifacts) {
        if ((artifact.getArtifactType()
            .compareToIgnoreCase(ArtifactTypeEnum.MODEL_INVENTORY_PROFILE.toString()) == 0)
            || (artifact.getArtifactType()
                .compareToIgnoreCase(ArtifactTypeEnum.MODEL_QUERY_SPEC.toString()) == 0)) {
          if (modelDeploySuccess) {
            publishDeploySuccess(data, artifact);
          } else {
            publishDeployFailure(data, artifact);
            statusString = "FAILURE";
          }
        } else if (artifact.getArtifactType()
            .compareToIgnoreCase(ArtifactTypeEnum.VNF_CATALOG.toString()) == 0) {
          if (catalogDeploySuccess) {
            publishDeploySuccess(data, artifact);
          } else {
            publishDeployFailure(data, artifact);
            statusString = "FAILURE";
          }
        }
      }
    } else {
      statusString = "FAILURE";
    }

    auditLogger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
        "Processed distribution " + data.getDistributionID() + "  (" + statusString + ")");
    MDC.clear();
  }

  private List<IArtifactInfo> getArtifacts(INotificationData data) {
    List<IArtifactInfo> artifacts = new ArrayList<IArtifactInfo>();
    List<IResourceInstance> resources = data.getResources();

    if (data.getServiceArtifacts() != null) {
      artifacts.addAll(data.getServiceArtifacts());
    }

    if (resources != null) {
      for (IResourceInstance resource : resources) {
        if (resource.getArtifacts() != null) {
          artifacts.addAll(resource.getArtifacts());
        }
      }
    }

    return artifacts;
  }

  private void publishDownloadFailure(INotificationData data, IArtifactInfo artifact,
      String errorMessage) {
    // Grab the current time so we can measure the download time for the metrics
    // log
    long startTimeInMs = System.currentTimeMillis();
    MdcOverride override = new MdcOverride();
    override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

    IDistributionClientResult sendDownloadStatus = client.sendDownloadStatus(
        buildStatusMessage(client, data, artifact, DistributionStatusEnum.DOWNLOAD_ERROR));
    metricsLogger.info(ModelLoaderMsgs.EVENT_PUBLISHED, null, override, "download failure",
        artifact.getArtifactName(), sendDownloadStatus.getDistributionActionResult().toString());

    if (sendDownloadStatus.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
      logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
          "Failed to publish download failure status: "
              + sendDownloadStatus.getDistributionMessageResult());
    }

    logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
        "Failed to download artifact " + artifact.getArtifactName() + ": " + errorMessage);
  }

  private void publishDownloadSuccess(INotificationData data, IArtifactInfo artifact,
      IDistributionClientDownloadResult downloadResult) {
    // Grab the current time so we can measure the download time for the metrics
    // log
    long startTimeInMs = System.currentTimeMillis();
    MdcOverride override = new MdcOverride();
    override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

    IDistributionClientResult sendDownloadStatus = client.sendDownloadStatus(
        buildStatusMessage(client, data, artifact, DistributionStatusEnum.DOWNLOAD_OK));
    metricsLogger.info(ModelLoaderMsgs.EVENT_PUBLISHED, null, override, "download success",
        artifact.getArtifactName(), sendDownloadStatus.getDistributionActionResult().toString());

    if (sendDownloadStatus.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
      logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
          "Failed to publish download success status: "
              + sendDownloadStatus.getDistributionMessageResult());
    }

    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Downloaded artifact:\n");
      sb.append("ArtInfo_Art_Name: " + artifact.getArtifactName());
      sb.append("\nArtInfo_Art_description: " + artifact.getArtifactDescription());
      sb.append("\nArtInfo_Art_CheckSum: " + artifact.getArtifactChecksum());
      sb.append("\nArtInfo_Art_Url: " + artifact.getArtifactURL());
      sb.append("\nArtInfo_Art_Type: " + artifact.getArtifactType());
      sb.append("\nArtInfo_Serv_description: " + data.getServiceDescription());
      sb.append("\nArtInfo_Serv_Name: " + data.getServiceName());
      sb.append("\nGet_serviceVersion: " + data.getServiceVersion());
      sb.append("\nGet_Service_UUID: " + data.getServiceUUID());
      sb.append("\nArtInfo_DistributionId: " + data.getDistributionID());
      logger.debug(sb.toString());
    }
  }

  private void publishDeployFailure(INotificationData data, IArtifactInfo artifact) {
    // Grab the current time so we can measure the download time for the metrics
    // log
    long startTimeInMs = System.currentTimeMillis();
    MdcOverride override = new MdcOverride();
    override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

    IDistributionClientResult sendStatus = client.sendDeploymentStatus(
        buildStatusMessage(client, data, artifact, DistributionStatusEnum.DEPLOY_ERROR));
    metricsLogger.info(ModelLoaderMsgs.EVENT_PUBLISHED, null, override, "deploy failure",
        artifact.getArtifactName(), sendStatus.getDistributionActionResult().toString());

    if (sendStatus.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
      logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
          "Failed to publish deploy failure status: " + sendStatus.getDistributionMessageResult());
    }
  }

  private void publishDeploySuccess(INotificationData data, IArtifactInfo artifact) {
    // Grab the current time so we can measure the download time for the metrics
    // log
    long startTimeInMs = System.currentTimeMillis();
    MdcOverride override = new MdcOverride();
    override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

    IDistributionClientResult sendStatus = client.sendDownloadStatus(
        buildStatusMessage(client, data, artifact, DistributionStatusEnum.DEPLOY_OK));
    metricsLogger.info(ModelLoaderMsgs.EVENT_PUBLISHED, null, override, "deploy success",
        artifact.getArtifactName(), sendStatus.getDistributionActionResult().toString());

    if (sendStatus.getDistributionActionResult() != DistributionActionResultEnum.SUCCESS) {
      logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
          "Failed to publish deploy success status: " + sendStatus.getDistributionMessageResult());
    }
  }

  private IDistributionStatusMessage buildStatusMessage(IDistributionClient client,
      INotificationData data, IArtifactInfo artifact, DistributionStatusEnum status) {
    IDistributionStatusMessage statusMessage = new DistributionStatusMsg(status,
        data.getDistributionID(), client.getConfiguration().getConsumerID(),
        artifact.getArtifactURL());

    return statusMessage;
  }

}
