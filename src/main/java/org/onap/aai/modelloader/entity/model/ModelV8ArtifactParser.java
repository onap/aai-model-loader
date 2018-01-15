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

import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class ModelV8ArtifactParser implements IModelParser {

	private static String MODEL_NAME_VERSION_ID = "model-name-version-id";
	private static String RELATIONSHIP_DATA = "relationship-data";
	private static String RELATIONSHIP_KEY = "relationship-key";
	private static String RELATIONSHIP_VALUE = "relationship-value";
  private static String MODEL_ELEMENT_RELATIONSHIP_KEY = "model.model-name-version-id";

	
	private  static Logger logger = LoggerFactory.getInstance().getLogger(ModelV8ArtifactParser.class.getName());
	@Override
	public List<Artifact> parse(byte[] artifactPayload, String artifactName) {
	  String payload = new String(artifactPayload);
	  List<Artifact> modelList = new ArrayList<Artifact>();

	  try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(payload));
	    Document doc = builder.parse(is);

	    ModelV8Artifact model = parseModel(doc.getDocumentElement(), payload);

	    if (model != null) {
	      logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Model parsed =====>>>> " + "Model-Named-Version-Id: "+ model.getModelNameVersionId());
	      modelList.add(model);
	    }
	    else {
	      logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse legacy model artifact " + artifactName);
	      return null;
	    }
	  }
	  catch (Exception ex) {
	    logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse legacy model artifact " + artifactName + ": " + ex.getLocalizedMessage());
	  }

	  return modelList;
	}

	private ModelV8Artifact parseModel(Node modelNode, String payload) {
	  ModelV8Artifact model = new ModelV8Artifact();
	  model.setPayload(payload);

	  Element e = (Element)modelNode;
	  model.setModelNamespace(e.getAttribute("xmlns"));

	  parseNode(modelNode, model);

    if (model.getModelNameVersionId() == null) {
      return null;
    }

	  return model;
	}

	private void parseNode(Node node, ModelV8Artifact model) {
	  if (node.getNodeName().equalsIgnoreCase(MODEL_NAME_VERSION_ID)) {
	    model.setModelNameVersionId(node.getTextContent().trim());
	  }
	  else if (node.getNodeName().equalsIgnoreCase(RELATIONSHIP_DATA)) {
	    parseRelationshipNode(node, model);
	  }
	  else {
	    NodeList nodeList = node.getChildNodes();
	    for (int i = 0; i < nodeList.getLength(); i++) {
	      Node childNode = nodeList.item(i);
	      parseNode(childNode, model);
	    }
	  }
	}

	private void parseRelationshipNode(Node node, ModelV8Artifact model) {
    String key = null;
    String value = null;
    
    NodeList nodeList = node.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node childNode = nodeList.item(i);
      if (childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_KEY)) {
        key = childNode.getTextContent().trim();
      }
      else if (childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_VALUE)) {
        value = childNode.getTextContent().trim();
      }
    }
    
    if ( (key != null) && (key.equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY )) && (value != null) ) {
     
        model.addDependentModelId(value);
      
    }
	}
}
