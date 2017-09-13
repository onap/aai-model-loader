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
package org.onap.aai.modelloader.entity.model;

import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class ModelArtifactHandler extends ArtifactHandler {

  private static Logger logger = LoggerFactory.getInstance().getLogger(ModelArtifactHandler.class.getName());
  
  public ModelArtifactHandler(ModelLoaderConfig config) {
    super(config);
  }

  @Override
  public boolean pushArtifacts(List<Artifact> artifacts, String distributionID) {
    ModelSorter modelSorter = new ModelSorter();
    List<Artifact> sortedModelArtifacts; 
    try {
      sortedModelArtifacts = modelSorter.sort(artifacts);
    }
    catch (RuntimeException ex) {
      logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Unable to resolve models: " + ex.getMessage());
      return false;
    }
    
    // Push the ordered list of model artifacts to A&AI.  If one fails, we need to roll back
    // the changes.
    List<AbstractModelArtifact> completedModels = new ArrayList<AbstractModelArtifact>();
    AaiRestClient aaiClient = new AaiRestClient(config);

    for (Artifact art : sortedModelArtifacts) {
      AbstractModelArtifact model = (AbstractModelArtifact)art;
      if (model.push(aaiClient, config, distributionID, completedModels) != true) {
        for (AbstractModelArtifact modelToDelete : completedModels) {
          modelToDelete.rollbackModel(aaiClient, config, distributionID);
        }

        return false;
      }
    }

    return true;
  }

  // This method is used for the test REST interface to load models without an ASDC
  public void loadModelTest(byte[] payload) {
    List<Artifact> modelArtifacts = new ArrayList<Artifact>();
    ModelArtifactParser parser = new ModelArtifactParser();
    modelArtifacts.addAll(parser.parse(payload, "Test-Artifact"));
    ModelSorter modelSorter = new ModelSorter();
    List<Artifact> sortedModelArtifacts = modelSorter.sort(modelArtifacts);
    pushArtifacts(sortedModelArtifacts, "Test-Distribution");
  }
}
