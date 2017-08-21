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
package org.openecomp.modelloader.entity.model;

import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.modelloader.config.ModelLoaderConfig;
import org.openecomp.modelloader.entity.ArtifactType;
import org.openecomp.modelloader.restclient.AaiRestClient;
import org.openecomp.modelloader.service.ModelLoaderMsgs;
import org.w3c.dom.Node;

import com.sun.jersey.api.client.ClientResponse;

public class ModelArtifact extends AbstractModelArtifact {

  private static final String AAI_MODEL_VER_SUB_URL = "/model-vers/model-ver";
  
  private static Logger logger = LoggerFactory.getInstance().getLogger(ModelArtifact.class.getName());
  
  private String modelVerId;
	private String modelInvariantId;
	private Node modelVer;
	private boolean firstVersionOfModel = false;

	public ModelArtifact() {
	  super(ArtifactType.MODEL);
	}

	public String getModelVerId() {
	  return modelVerId;
	}
	
	public void setModelVerId(String modelVerId) {
		this.modelVerId = modelVerId;
	}
	
	public String getModelInvariantId() {
		return modelInvariantId;
	}
	
	public void setModelInvariantId(String modelInvariantId) {
		this.modelInvariantId = modelInvariantId;
	}
	
	public Node getModelVer() {
		return modelVer;
	}
	
	public void setModelVer(Node modelVer) {
		this.modelVer = modelVer;
	}

  @Override
  public String getUniqueIdentifier() {
    return getModelInvariantId() + "|" + getModelVerId();
  }

  @Override
  public boolean push(AaiRestClient aaiClient, ModelLoaderConfig config, String distId, List<AbstractModelArtifact> addedModels) {
    ClientResponse getResponse  = aaiClient.getResource(getModelUrl(config), distId, AaiRestClient.MimeType.XML);
    if ( (getResponse == null) || (getResponse.getStatus() != Response.Status.OK.getStatusCode()) ) {
      // Only attempt the PUT if the model doesn't already exist
      ClientResponse putResponse = aaiClient.putResource(getModelUrl(config), getPayload(), distId, AaiRestClient.MimeType.XML);
      if ( (putResponse != null) && (putResponse.getStatus() == Response.Status.CREATED.getStatusCode()) ) {
        addedModels.add(this);
        
        // Flag this as the first version of the model that has been added.
        firstVersionOfModel = true;
        
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, getType().toString() + " " + getUniqueIdentifier() + " successfully ingested.");
      }
      else {
        logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed for " + getType().toString() + " " + getUniqueIdentifier() +
            ". Rolling back distribution.");
        return false;
      }
    }
    else {
      logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, getType().toString() + " " + getModelInvariantId() + " already exists.  Skipping ingestion.");
      getResponse  = aaiClient.getResource(getModelVerUrl(config), distId, AaiRestClient.MimeType.XML);
      if ( (getResponse == null) || (getResponse.getStatus() != Response.Status.OK.getStatusCode()) ) {
        // Only attempt the PUT if the model-ver doesn't already exist
        ClientResponse putResponse = null;

        try {
          putResponse = aaiClient.putResource(getModelVerUrl(config), nodeToString(getModelVer()), distId, AaiRestClient.MimeType.XML);
        } catch (TransformerException e) {
          logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed for " + getType().toString() + " " + getUniqueIdentifier() 
            + ": " + e.getMessage() + ". Rolling back distribution.");
          return false;
        }
        if ( (putResponse != null) && (putResponse.getStatus() == Response.Status.CREATED.getStatusCode()) ) {
          addedModels.add(this);
          logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, getType().toString() + " " + getUniqueIdentifier() + " successfully ingested.");
        }
        else {
          logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed for " + getType().toString() + " " 
              + getUniqueIdentifier() + ". Rolling back distribution.");
          return false;
        }
      }
      else {
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, getType().toString() + " " + getUniqueIdentifier() + " already exists.  Skipping ingestion.");
      }
    }
    
    return true;
  }
  
  @Override
  public void rollbackModel(AaiRestClient aaiClient, ModelLoaderConfig config, String distId) {
    String url = getModelVerUrl(config);
    if (firstVersionOfModel) {
      // If this was the first version of the model which was added, we want to remove the entire
      // model rather than just the version.
      url = getModelUrl(config);
    }
    
    // Best effort to delete.  Nothing we can do in the event this fails.
    aaiClient.getAndDeleteResource(url, distId);
  }
  
  private String getModelUrl(ModelLoaderConfig config) {
    String baseURL = config.getAaiBaseUrl().trim();
    String subURL = null;
    String instance = null;

    subURL = config.getAaiModelUrl(getModelNamespaceVersion()).trim();
    instance = getModelInvariantId();

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

  private String getModelVerUrl(ModelLoaderConfig config) {
    String baseURL = config.getAaiBaseUrl().trim();
    String subURL = null;
    String instance = null;

    subURL = config.getAaiModelUrl(getModelNamespaceVersion()).trim() + getModelInvariantId() + AAI_MODEL_VER_SUB_URL;
    instance = getModelVerId();

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
  
  private String nodeToString(Node node) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.transform(new DOMSource(node), new StreamResult(sw));
    return sw.toString();
  }
}
