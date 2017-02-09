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

package org.openecomp.modelloader.entity.model;

import com.sun.jersey.api.client.ClientResponse;

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.modelloader.config.ModelLoaderConfig;
import org.openecomp.modelloader.entity.Artifact;
import org.openecomp.modelloader.entity.ArtifactHandler;
import org.openecomp.modelloader.entity.ArtifactType;
import org.openecomp.modelloader.restclient.AaiRestClient;
import org.openecomp.modelloader.service.ModelLoaderMsgs;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

public class ModelArtifactHandler extends ArtifactHandler {

  private static Logger logger = LoggerFactory.getInstance()
      .getLogger(ArtifactHandler.class.getName());

  public ModelArtifactHandler(ModelLoaderConfig config) {
    super(config);
  }

  @Override
  public boolean pushArtifacts(List<Artifact> artifacts, String distributionId) {
    ModelSorter modelSorter = new ModelSorter();
    List<Artifact> sortedModelArtifacts = modelSorter.sort(artifacts);

    // Push the ordered list of model artifacts to A&AI. If one fails, we need
    // to roll back
    // the changes.
    List<ModelArtifact> completedModels = new ArrayList<ModelArtifact>();
    AaiRestClient aaiClient = new AaiRestClient(config);

    for (Artifact art : sortedModelArtifacts) {
      ModelArtifact model = (ModelArtifact) art;
      ClientResponse getResponse = aaiClient.getResource(getUrl(model), distributionId,
          AaiRestClient.MimeType.XML);
      if ((getResponse == null)
          || (getResponse.getStatus() != Response.Status.OK.getStatusCode())) {
        // Only attempt the PUT if the model doesn't already exist
        ClientResponse putResponse = aaiClient.putResource(getUrl(model), model.getPayload(),
            distributionId, AaiRestClient.MimeType.XML);
        if ((putResponse != null)
            && (putResponse.getStatus() == Response.Status.CREATED.getStatusCode())) {
          completedModels.add(model);
          logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, model.getType().toString() + " "
              + model.getNameVersionId() + " successfully ingested.");
        } else {
          logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
              "Ingestion failed for " + model.getType().toString() + " " + model.getNameVersionId()
                  + ". Rolling back distribution.");

          for (ModelArtifact modelToDelete : completedModels) {
            // Best effort to delete. Nothing we can do in the event this fails.
            aaiClient.getAndDeleteResource(getUrl(modelToDelete), distributionId);
          }

          return false;
        }
      } else {
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, model.getType().toString() + " "
            + model.getNameVersionId() + " already exists.  Skipping ingestion.");
      }
    }

    return true;
  }

  private String getUrl(ModelArtifact model) {
    String baseUrl = config.getAaiBaseUrl().trim();
    String subUrl = null;
    if (model.getType().equals(ArtifactType.MODEL)) {
      subUrl = config.getAaiModelUrl().trim();
    } else {
      subUrl = config.getAaiNamedQueryUrl().trim();
    }

    if ((!baseUrl.endsWith("/")) && (!subUrl.startsWith("/"))) {
      baseUrl = baseUrl + "/";
    }

    if (baseUrl.endsWith("/") && subUrl.startsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }

    if (!subUrl.endsWith("/")) {
      subUrl = subUrl + "/";
    }

    String url = baseUrl + subUrl + model.getNameVersionId();
    return url;
  }

  /**
   * This method is used for the test REST interface to load models without an
   * ASDC.
   * 
   * @param payload content of the request
   */
  public void loadModelTest(byte[] payload) {
    List<Artifact> modelArtifacts = new ArrayList<Artifact>();
    ModelArtifactParser parser = new ModelArtifactParser();
    modelArtifacts.addAll(parser.parse(payload, "Test-Artifact"));
    ModelSorter modelSorter = new ModelSorter();
    List<Artifact> sortedModelArtifacts = modelSorter.sort(modelArtifacts);
    pushArtifacts(sortedModelArtifacts, "Test-Distribution");
  }
}
