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
package org.onap.aai.modelloader.entity.model;

import java.util.List;

import javax.ws.rs.core.Response;

import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;

public class ModelV8Artifact extends AbstractModelArtifact {
  private static String AAI_CONVERSION_URL = "/aai/tools/modeltransform";	

  private static Logger logger = LoggerFactory.getInstance().getLogger(ModelArtifact.class.getName());

  private String modelNameVersionId; 
	private ModelArtifact translatedModel;

	public ModelV8Artifact() {
	  super(ArtifactType.MODEL_V8);
	}

	public String getModelNameVersionId() {
	  return modelNameVersionId;
	}
	
	public void setModelNameVersionId(String modelNameVersionId) {
		this.modelNameVersionId = modelNameVersionId;
	}
	
  @Override
  public String getUniqueIdentifier() {
    return getModelNameVersionId();
  }	
	
  @Override
  public boolean push(AaiRestClient aaiClient, ModelLoaderConfig config, String distId, List<AbstractModelArtifact> addedModels) {
    // For a legacy model (version <= v8), we need to call out to an A&AI endpoint to convert to the proper format
    ClientResponse response  = aaiClient.postResource(getConversionUrl(config), constructTransformPayload(), distId, AaiRestClient.MimeType.XML);
    if ( (response == null) || (response.getStatus() != Response.Status.OK.getStatusCode()) ) {
      logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed for " + 
          getType().toString() + " " + getModelNameVersionId() + ". Unable to convert model.  Rolling back distribution.");
      return false;
    }
    
    String translatedPayload = response.getEntity(String.class);
    
    logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Translated artifact payload:\n" + translatedPayload);
    
    ModelArtifactParser parser = new ModelArtifactParser();
    
    List<Artifact> parsedArtifacts = parser.parse(translatedPayload.getBytes(), "translated-payload");
    if (parsedArtifacts == null || parsedArtifacts.isEmpty()) {
      return false;
    } 
    
    translatedModel = (ModelArtifact)parsedArtifacts.get(0);
    return translatedModel.push(aaiClient, config, distId, addedModels);
  }

  @Override
  public void rollbackModel(AaiRestClient aaiClient, ModelLoaderConfig config, String distId) {
    if (translatedModel != null) {
      translatedModel.rollbackModel(aaiClient, config, distId);
    }
  }
  

  private String constructTransformPayload() {
    // A&AI requires that to transform a legacy model, we need to use the v8 namespace (even 
    // if the version < 8)
    return getPayload().replaceFirst("aai.inventory/v.", "aai.inventory/v8");
  }
  
  private String getConversionUrl(ModelLoaderConfig config) {
    String baseUrl = config.getAaiBaseUrl().trim();
    String subUrl = AAI_CONVERSION_URL;

    if ( (!baseUrl.endsWith("/")) && (!subUrl.startsWith("/")) ) {
      baseUrl = baseUrl + "/";
    }

    if ( baseUrl.endsWith("/") && subUrl.startsWith("/") ) {
      baseUrl = baseUrl.substring(0, baseUrl.length()-1);
    }

    if (!subUrl.endsWith("/")) {
      subUrl = subUrl + "/";
    }

    String url = baseUrl + subUrl;
    return url;
  }
}
