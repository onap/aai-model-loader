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

import java.util.List;

import javax.ws.rs.core.Response;

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.modelloader.config.ModelLoaderConfig;
import org.openecomp.modelloader.entity.ArtifactType;
import org.openecomp.modelloader.restclient.AaiRestClient;
import org.openecomp.modelloader.service.ModelLoaderMsgs;

import com.sun.jersey.api.client.ClientResponse;

public class NamedQueryArtifact extends AbstractModelArtifact {
		
  private Logger logger = LoggerFactory.getInstance().getLogger(NamedQueryArtifact.class.getName());
  
	private String namedQueryUuid; 
	
	public NamedQueryArtifact() {
	  super(ArtifactType.NAMED_QUERY);
	}
	
	public String getNamedQueryUuid() {
		return namedQueryUuid;
	}
	
	public void setNamedQueryUuid(String namedQueryUuid) {
		this.namedQueryUuid = namedQueryUuid;
	}
	
  @Override
  public String getUniqueIdentifier() {
    return getNamedQueryUuid();
  }	

  @Override
  public boolean push(AaiRestClient aaiClient, ModelLoaderConfig config, String distId, List<AbstractModelArtifact> addedModels) {
    ClientResponse getResponse  = aaiClient.getResource(getNamedQueryUrl(config), distId, AaiRestClient.MimeType.XML);
    if ( (getResponse == null) || (getResponse.getStatus() != Response.Status.OK.getStatusCode()) ) {
      // Only attempt the PUT if the model doesn't already exist
      ClientResponse putResponse = aaiClient.putResource(getNamedQueryUrl(config), getPayload(), distId, AaiRestClient.MimeType.XML);
      if ( (putResponse != null) && (putResponse.getStatus() == Response.Status.CREATED.getStatusCode()) ) {
        addedModels.add(this);
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, getType().toString() + " " + getUniqueIdentifier() + " successfully ingested.");
      }
      else {
        logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed for " + getType().toString() + " " + getUniqueIdentifier() +
            ". Rolling back distribution.");
        return false;
      }
    }
    else {
      logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, getType().toString() + " " + getUniqueIdentifier() + " already exists.  Skipping ingestion.");
    }
    
    return true;
  }

  @Override
  public void rollbackModel(AaiRestClient aaiClient, ModelLoaderConfig config, String distId) {
    // Best effort to delete.  Nothing we can do in the event this fails.
    aaiClient.getAndDeleteResource(getNamedQueryUrl(config), distId);
  }

  private String getNamedQueryUrl(ModelLoaderConfig config) {
    String baseURL = config.getAaiBaseUrl().trim();
    String subURL = null;
    String instance = null;

    subURL = config.getAaiNamedQueryUrl(getModelNamespaceVersion()).trim();
    instance = this.getNamedQueryUuid();

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
}
