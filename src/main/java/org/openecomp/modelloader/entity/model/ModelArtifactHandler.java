/**
 * ============LICENSE_START=======================================================
 * Model Loader
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP and OpenECOMP are trademarks
 * and service marks of AT&T Intellectual Property.
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

import org.w3c.dom.Node;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class ModelArtifactHandler extends ArtifactHandler {

  private static final String AAI_MODEL_VER = "/model-vers/model-ver";
  private static Logger logger = LoggerFactory.getInstance().getLogger(ArtifactHandler.class.getName());


  public ModelArtifactHandler(ModelLoaderConfig config) {
    super(config);
  }

  @Override
  public boolean pushArtifacts(List<Artifact> artifacts, String distributionID) {
    ModelSorter modelSorter = new ModelSorter();
    List<Artifact> sortedModelArtifacts = modelSorter.sort(artifacts);

    // Push the ordered list of model artifacts to A&AI.  If one fails, we need to roll back
    // the changes.
    List<ModelArtifact> completedModels = new ArrayList<ModelArtifact>();
    AaiRestClient aaiClient = new AaiRestClient(config);

    for (Artifact art : sortedModelArtifacts) {
      ModelArtifact model = (ModelArtifact)art;

      boolean version = model.isV9Artifact();
      //Non - V9 version for models
      if(version == false){
        ClientResponse getResponse = aaiClient.getResource(getModelVerURL(model), distributionID, AaiRestClient.MimeType.XML);
        if ( (getResponse == null) || (getResponse.getStatus() != Response.Status.OK.getStatusCode()) ) {
          logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, " Artifact format not valid for " + 
              model.getType().toString() + "- model-invariant-id[model-id]: " + 
              model.getModelInvariantId() + " and model-version-id[model-name-version-id]: "+ 
              model.getModelVerId()+ " . Rolling back distribution.");
          return false;
        }
        else{
          completedModels.add(model);
          logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, model.getType().toString() + 
              " " + model.getModelInvariantId() + " successfully ingested.");
        }
      }
      else
      {
        ClientResponse getResponse  = aaiClient.getResource(getURL(model), distributionID, AaiRestClient.MimeType.XML);
        if ( (getResponse == null) || (getResponse.getStatus() != Response.Status.OK.getStatusCode()) ) {
          // Only attempt the PUT if the model doesn't already exist
          ClientResponse putResponse = aaiClient.putResource(getURL(model), model.getPayload(), distributionID, AaiRestClient.MimeType.XML);
          if ( (putResponse != null) && (putResponse.getStatus() == Response.Status.CREATED.getStatusCode()) ) {
            completedModels.add(model);
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, model.getType().toString() + 
                " " + model.getModelInvariantId() + " successfully ingested.");
          }
          else {
            logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed for " + 
                model.getType().toString() + " " + model.getModelInvariantId() + ". Rolling back distribution.");

            for (ModelArtifact modelToDelete : completedModels) {
              // Best effort to delete.  Nothing we can do in the event this fails.
              aaiClient.getAndDeleteResource(getURL(modelToDelete), distributionID);
            }

            return false;
          }
        }
        else {
          logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, model.getType().toString() + " " + model.getModelInvariantId() + 
              " already exists.  Skipping ingestion.");
          getResponse  = aaiClient.getResource(getModelVerURL(model), distributionID, AaiRestClient.MimeType.XML);
          if ( (getResponse == null) || (getResponse.getStatus() != Response.Status.OK.getStatusCode()) ) {
            // Only attempt the PUT if the model-ver doesn't already exist
            ClientResponse putResponse = null;

            try {
              putResponse = aaiClient.putResource(getModelVerURL(model), nodeToString(model.getModelVer()), distributionID, AaiRestClient.MimeType.XML);
            } catch (TransformerException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            if ( (putResponse != null) && (putResponse.getStatus() == Response.Status.CREATED.getStatusCode()) ) {
              completedModels.add(model);
              logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, model.getType().toString() + " " + 
                  model.getNameVersionId() + " successfully ingested.");
            }
            else {
              logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed for " + 
                  model.getType().toString() + " " + model.getNameVersionId() + ". Rolling back distribution.");

              for (ModelArtifact modelToDelete : completedModels) {
                // Best effort to delete.  Nothing we can do in the event this fails.
                aaiClient.getAndDeleteResource(getModelVerURL(modelToDelete), distributionID);
              }

              return false;
            }
          }
          else {
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, model.getType().toString() + " " + 
                model.getModelInvariantId() + " already exists.  Skipping ingestion.");
          }
        }
      }
    }

    return true;
  }


  private String nodeToString(Node node) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.transform(new DOMSource(node), new StreamResult(sw));
    System.out.println(sw.toString());
    return sw.toString();
  }

  private String getURL(ModelArtifact model) {
    String baseURL = config.getAaiBaseUrl().trim();
    String subURL = null;
    String instance = null;
    if (model.getType().equals(ArtifactType.MODEL)) {
      subURL = config.getAaiModelUrl(model.getModelNamespaceVersion()).trim();
      instance = model.getModelInvariantId();
    }
    else {
      subURL = config.getAaiNamedQueryUrl(model.getModelNamespaceVersion()).trim();
      instance = model.getNameVersionId();
    }

    if ( (!baseURL.endsWith("/")) && (!subURL.startsWith("/")) ) {
      baseURL = baseURL + "/";
    }

    if ( baseURL.endsWith("/") && subURL.startsWith("/") ) {
      baseURL = baseURL.substring(0, baseURL.length()-1);
    }

    if (!subURL.endsWith("/")) {
      subURL = subURL + "/";
    }

    String url = baseURL + subURL + instance;
    return url;
  }

  private String getModelVerURL(ModelArtifact model) {
    String baseURL = config.getAaiBaseUrl().trim();
    String subURL = null;
    String instance = null;
    if (model.getType().equals(ArtifactType.MODEL)) {
      subURL = config.getAaiModelUrl(model.getModelNamespaceVersion()).trim() + model.getModelInvariantId() + AAI_MODEL_VER;
      instance = model.getModelVerId();
    }
    else {
      subURL = config.getAaiNamedQueryUrl(model.getModelNamespaceVersion()).trim();
      instance = model.getNameVersionId();
    }

    if ( (!baseURL.endsWith("/")) && (!subURL.startsWith("/")) ) {
      baseURL = baseURL + "/";
    }

    if ( baseURL.endsWith("/") && subURL.startsWith("/") ) {
      baseURL = baseURL.substring(0, baseURL.length()-1);
    }

    if (!subURL.endsWith("/")) {
      subURL = subURL + "/";
    }

    String url = baseURL + subURL + instance;
    return url;
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
