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

import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifactHandler;
import org.onap.aai.modelloader.entity.model.ModelArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for deploying model and catalog artifacts.
 */
@Component
public class ArtifactDeploymentManager {

    private final ModelArtifactHandler modelArtifactHandler;
    private final VnfCatalogArtifactHandler vnfCatalogArtifactHandler;
    private final AaiRestClient aaiClient;

    public ArtifactDeploymentManager(ModelArtifactHandler modelArtifactHandler, VnfCatalogArtifactHandler vnfCatalogArtifactHandler, AaiRestClient aaiClient) {
        this.modelArtifactHandler = modelArtifactHandler;
        this.vnfCatalogArtifactHandler = vnfCatalogArtifactHandler;
        this.aaiClient = aaiClient;
    }

    /**
     * Deploys model and catalog artifacts to A&AI.
     *
     * @param distributionId data about the notification that is being processed
     * @param modelArtifacts collection of artifacts that represent yml files found in a TOSCA_CSAR file that have been
     *        converted to XML and also those for model query specs
     * @param catalogArtifacts collection of artifacts that represent vnf catalog files
     * @return boolean <code>true</code> if all deployments were successful otherwise <code>false</code>
     */
    public boolean deploy(final String distributionId, final List<Artifact> modelArtifacts,
            final List<Artifact> catalogArtifacts) {

        List<Artifact> completedArtifacts = new ArrayList<>();
        boolean deploySuccess =
                modelArtifactHandler.pushArtifacts(modelArtifacts, distributionId, completedArtifacts, aaiClient);

        if (!deploySuccess) {
            modelArtifactHandler.rollback(completedArtifacts, distributionId, aaiClient);
        } else {
            List<Artifact> completedImageData = new ArrayList<>();
            deploySuccess = vnfCatalogArtifactHandler.pushArtifacts(catalogArtifacts, distributionId,
                    completedImageData, aaiClient);
            if (!deploySuccess) {
                modelArtifactHandler.rollback(completedArtifacts, distributionId, aaiClient);
                vnfCatalogArtifactHandler.rollback(completedImageData, distributionId, aaiClient);
            }
        }

        return deploySuccess;
    }
}