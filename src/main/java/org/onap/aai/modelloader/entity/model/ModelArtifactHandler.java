/**
 * ﻿============LICENSE_START=======================================================
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
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.springframework.stereotype.Service;

@Service
public class ModelArtifactHandler extends ArtifactHandler {

    private static Logger logger = LoggerFactory.getInstance().getLogger(ModelArtifactHandler.class.getName());

    public ModelArtifactHandler(ModelLoaderConfig config) {
        super(config);
    }

    @Override
    public boolean pushArtifacts(List<Artifact> artifacts, String distributionID, List<Artifact> completedArtifacts,
            AaiRestClient aaiClient) {
        ModelSorter modelSorter = new ModelSorter();
        List<Artifact> sortedModelArtifacts;
        try {
            sortedModelArtifacts = modelSorter.sort(artifacts);
        } catch (BabelArtifactParsingException ex) {
            logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Unable to resolve models: " + ex.getMessage());
            return false;
        }

        // Push the ordered list of model artifacts to A&AI. If one fails, we need to roll back the changes.
        for (Artifact art : sortedModelArtifacts) {
            AbstractModelArtifact model = (AbstractModelArtifact) art;
            if (!model.push(aaiClient, config, distributionID, completedArtifacts)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void rollback(List<Artifact> completedArtifacts, String distributionId, AaiRestClient aaiClient) {
        for (Artifact artifactToDelete : completedArtifacts) {
            AbstractModelArtifact model = (AbstractModelArtifact) artifactToDelete;
            model.rollbackModel(aaiClient, config, distributionId);
        }
    }
}
