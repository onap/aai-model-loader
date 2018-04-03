/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.aai.modelloader.notification;

import java.util.ArrayList;
import java.util.List;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.extraction.ArtifactInfoExtractor;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.consumer.INotificationCallback;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.INotificationData;
import org.slf4j.MDC;

public class EventCallback implements INotificationCallback {
    private static Logger logger = LoggerFactory.getInstance().getLogger(EventCallback.class.getName());
    private static Logger auditLogger = LoggerFactory.getInstance().getAuditLogger(EventCallback.class.getName());

    private ArtifactDeploymentManager artifactDeploymentManager;
    private ArtifactDownloadManager artifactDownloadManager;
    private IDistributionClient client;
    private ModelLoaderConfig config;

    public EventCallback(IDistributionClient client, ModelLoaderConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public void activateCallback(INotificationData data) {
        MdcContext.initialize(data.getDistributionID(), "ModelLoader", "", "Event-Bus", "");
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Received distribution " + data.getDistributionID());

        List<IArtifactInfo> artifacts = new ArtifactInfoExtractor().extract(data);
        List<Artifact> catalogArtifacts = new ArrayList<>();
        List<Artifact> modelArtifacts = new ArrayList<>();

        boolean success = getArtifactDownloadManager()
                .downloadArtifacts(data, artifacts, modelArtifacts, catalogArtifacts);

        if (success) {
            success = getArtifactDeploymentManager().deploy(data, artifacts, modelArtifacts, catalogArtifacts);
        }

        String statusString = success ? "SUCCESS" : "FAILURE";
        auditLogger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                "Processed distribution " + data.getDistributionID() + "  (" + statusString + ")");
        MDC.clear();
    }

    private ArtifactDeploymentManager getArtifactDeploymentManager() {
        if (artifactDeploymentManager == null) {
            artifactDeploymentManager = new ArtifactDeploymentManager(client, config);
        }

        return artifactDeploymentManager;
    }

    private ArtifactDownloadManager getArtifactDownloadManager() {
        if (artifactDownloadManager == null) {
            artifactDownloadManager = new ArtifactDownloadManager(client, config);
        }

        return artifactDownloadManager;
    }
}
