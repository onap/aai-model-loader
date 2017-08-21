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

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.modelloader.entity.Artifact;
import org.openecomp.modelloader.service.ModelLoaderMsgs;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class ModelArtifactParser implements IModelParser {

	private static String MODEL_VER = "model-ver";
	private static String MODEL_VERSION_ID = "model-version-id";
	private static String MODEL_INVARIANT_ID = "model-invariant-id";
	private static String RELATIONSHIP = "relationship";
	private static String RELATIONSHIP_DATA = "relationship-data";
	private static String RELATIONSHIP_KEY = "relationship-key";
	private static String RELATIONSHIP_VALUE = "relationship-value";
	private static String MODEL_ELEMENT_RELATIONSHIP_KEY = "model.model-invariant-id";
	private static String MODEL_VER_ELEMENT_RELATIONSHIP_KEY = "model-ver.model-version-id";
	
	private  static Logger logger = LoggerFactory.getInstance().getLogger(ModelArtifactParser.class.getName());
	
	public List<Artifact> parse(byte[] artifactPayload, String artifactName) {
	  String payload = new String(artifactPayload);
	  List<Artifact> modelList = new ArrayList<Artifact>();

	  try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(payload));
	    Document doc = builder.parse(is);

	    ModelArtifact model = parseModel(doc.getDocumentElement(), payload);

	    if (model != null) {
	      logger.info( ModelLoaderMsgs.DISTRIBUTION_EVENT, "Model parsed =====>>>> "
	          + "Model-invariant-Id: "+ model.getModelInvariantId()
	          + " Model-Version-Id: "+ model.getModelVerId());
	      modelList.add(model);
	    }
	    else {
	      logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName);
	      return null;
	    }
	  }
	  catch (Exception ex) {
	    logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName + ": " + ex.getLocalizedMessage());
	  }

	  return modelList;
	}

	private ModelArtifact parseModel(Node modelNode, String payload) {
	  ModelArtifact model = new ModelArtifact();
	  model.setPayload(payload);

	  Element e = (Element)modelNode;
	  model.setModelNamespace(e.getAttribute("xmlns"));

	  parseNode(modelNode, model);

	  if ( (model.getModelInvariantId() == null) || (model.getModelVerId() == null) ){
	    return null;
	  }

	  return model;
	}

	private void parseNode(Node node, ModelArtifact model) {
	  if (node.getNodeName().equalsIgnoreCase(MODEL_INVARIANT_ID)) {
	    model.setModelInvariantId(node.getTextContent().trim());
	  }
	  else if (node.getNodeName().equalsIgnoreCase(MODEL_VERSION_ID)) {
	    model.setModelVerId(node.getTextContent().trim());
	  }
	  else if (node.getNodeName().equalsIgnoreCase(RELATIONSHIP)) {
	    String dependentModelKey = parseRelationshipNode(node, model);
	    if (dependentModelKey != null) {
	      model.addDependentModelId(dependentModelKey);
	    }
	  }
	  else {
	    if (node.getNodeName().equalsIgnoreCase(MODEL_VER)) {
	      model.setModelVer(node);
	      if ( (model.getModelNamespace() != null) && (!model.getModelNamespace().isEmpty()) ) {
	        Element e = (Element) node;
	        e.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", model.getModelNamespace());
	      }
	    }

	    NodeList nodeList = node.getChildNodes();

	    for (int i = 0; i < nodeList.getLength(); i++) {
	      Node childNode = nodeList.item(i); 
	      parseNode(childNode, model);
	    }
	  }
	}

	private String parseRelationshipNode(Node node, ModelArtifact model) {
	  String currentKey = null;
	  String currentValue = null;
	  String modelVersionIdValue = null;
	  String modelInvariantIdValue = null;

	  NodeList nodeList = node.getChildNodes();
	  for (int i = 0; i < nodeList.getLength(); i++) {
	    Node childNode = nodeList.item(i);
	    
	    if (childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_DATA)) {
	      NodeList relDataChildList = childNode.getChildNodes();
	      
	      for (int j = 0; j < relDataChildList.getLength(); j++) {
	        Node relDataChildNode = relDataChildList.item(j);
	        
	        if (relDataChildNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_KEY)) {
	          currentKey = relDataChildNode.getTextContent().trim();

	          if (currentValue != null) {
	            if (currentKey.equalsIgnoreCase(MODEL_VER_ELEMENT_RELATIONSHIP_KEY)) {
	              modelVersionIdValue = currentValue;
	            }
	            else if (currentKey.equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY)) {
	              modelInvariantIdValue = currentValue;
	            }
	            
	            currentKey = null;
	            currentValue = null;
	          }
	        }
	        else if (relDataChildNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_VALUE)) {
	          currentValue = relDataChildNode.getTextContent().trim();

	          if (currentKey != null) {
              if (currentKey.equalsIgnoreCase(MODEL_VER_ELEMENT_RELATIONSHIP_KEY)) {
                modelVersionIdValue = currentValue;
              }
              else if (currentKey.equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY)) {
                modelInvariantIdValue = currentValue;
              }
              
              currentKey = null;
              currentValue = null;
	          }
	        }
	      }
	    }
	  }
	  
	  if ( (modelVersionIdValue != null) && (modelInvariantIdValue != null) ) {
	    return modelInvariantIdValue + "|" + modelVersionIdValue;
	  }
	  
	  return null;

	}

}
