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

import java.util.ArrayList;
import java.util.List;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifactHandler;
import org.onap.aai.modelloader.entity.model.ModelArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.sdc.api.notification.INotificationData;

/**
 * This class is responsible for deploying model and catalog artifacts.
 */
public class ArtifactDeploymentManager {

    private ModelLoaderConfig config;
    private ModelArtifactHandler modelArtifactHandler;
    private VnfCatalogArtifactHandler vnfCatalogArtifactHandler;

    public ArtifactDeploymentManager(ModelLoaderConfig config) {
        this.config = config;
    }

    /**
     * Deploys model and catalog artifacts to A&AI.
     *
     * @param data data about the notification that is being processed
     * @param modelArtifacts collection of artifacts that represent yml files found in a TOSCA_CSAR file that have been
     *        converted to XML and also those for model query specs
     * @param catalogArtifacts collection of artifacts that represent vnf catalog files
     * @return boolean <code>true</code> if all deployments were successful otherwise <code>false</code>
     */
    public boolean deploy(final INotificationData data, final List<Artifact> modelArtifacts,
            final List<Artifact> catalogArtifacts) {

        AaiRestClient aaiClient = new AaiRestClient(config);
        String distributionId = data.getDistributionID();

        List<Artifact> completedArtifacts = new ArrayList<>();
        boolean deploySuccess =
                getModelArtifactHandler().pushArtifacts(modelArtifacts, distributionId, completedArtifacts, aaiClient);

        if (!deploySuccess) {
            getModelArtifactHandler().rollback(completedArtifacts, distributionId, aaiClient);
        } else {
            List<Artifact> completedImageData = new ArrayList<>();
            deploySuccess = getVnfCatalogArtifactHandler().pushArtifacts(catalogArtifacts, distributionId,
                    completedImageData, aaiClient);
            if (!deploySuccess) {
                getModelArtifactHandler().rollback(completedArtifacts, distributionId, aaiClient);
                getVnfCatalogArtifactHandler().rollback(completedImageData, distributionId, aaiClient);
            }
        }

        return deploySuccess;
    }

    private ModelArtifactHandler getModelArtifactHandler() {
        if (modelArtifactHandler == null) {
            modelArtifactHandler = new ModelArtifactHandler(config);
        }

        return modelArtifactHandler;
    }

    private VnfCatalogArtifactHandler getVnfCatalogArtifactHandler() {
        if (vnfCatalogArtifactHandler == null) {
            this.vnfCatalogArtifactHandler = new VnfCatalogArtifactHandler(config);
        }

        return vnfCatalogArtifactHandler;
    }
}
