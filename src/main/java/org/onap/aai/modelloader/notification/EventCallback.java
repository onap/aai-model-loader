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
package org.onap.aai.modelloader.notification;

import java.util.ArrayList;
import java.util.List;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.extraction.ArtifactInfoExtractor;
import org.onap.aai.modelloader.service.ArtifactDeploymentManager;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.consumer.INotificationCallback;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class EventCallback implements INotificationCallback {
    private static Logger logger = LoggerFactory.getInstance().getLogger(EventCallback.class.getName());
    private static Logger auditLogger = LoggerFactory.getInstance().getAuditLogger(EventCallback.class.getName());

    private final ArtifactDeploymentManager artifactDeploymentManager;
    private final ArtifactDownloadManager artifactDownloadManager;
    private final NotificationPublisher notificationPublisher;
    private final IDistributionClient client;

    public EventCallback(IDistributionClient client, ArtifactDeploymentManager artifactDeploymentManager, ArtifactDownloadManager artifactDownloadManager, NotificationPublisher notificationPublisher) {
        this.artifactDeploymentManager = artifactDeploymentManager;
        this.artifactDownloadManager = artifactDownloadManager;
        this.notificationPublisher = notificationPublisher;
        this.client = client;
    }

    @Override
    public void activateCallback(INotificationData data) {
        MdcContext.initialize(data.getDistributionID(), "ModelLoader", "", "Event-Bus", "");
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Received distribution " + data.getDistributionID());

        List<IArtifactInfo> artifacts = new ArtifactInfoExtractor().extract(data);
        List<Artifact> catalogArtifacts = new ArrayList<>();
        List<Artifact> modelArtifacts = new ArrayList<>();

        boolean success =
                artifactDownloadManager.downloadArtifacts(data, artifacts, modelArtifacts, catalogArtifacts);

        if (success) {
            success = artifactDeploymentManager.deploy(data, modelArtifacts, catalogArtifacts);
        }

        String statusString = success ? "SUCCESS" : "FAILURE";
        auditLogger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                "Processed distribution " + data.getDistributionID() + "  (" + statusString + ")");

        publishNotifications(data, "TOSCA_CSAR", artifacts, success);

        MDC.clear();
    }


    private void publishNotifications(INotificationData data, String filterType, List<IArtifactInfo> artifacts,
            boolean deploymentSuccess) {
        if (deploymentSuccess) {
            artifacts.stream().filter(a -> filterType.equalsIgnoreCase(a.getArtifactType()))
                    .forEach(a -> notificationPublisher.publishDeploySuccess(client, data, a));
            notificationPublisher.publishComponentSuccess(client, data);
        } else {
            artifacts.stream().filter(a -> filterType.equalsIgnoreCase(a.getArtifactType()))
                    .forEach(a -> notificationPublisher.publishDeployFailure(client, data, a));
            notificationPublisher.publishComponentFailure(client, data, "deploy failure");
        }
    }
}
