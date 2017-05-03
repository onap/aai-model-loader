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

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.modelloader.entity.Artifact;
import org.openecomp.modelloader.entity.ArtifactType;
import org.openecomp.modelloader.service.ModelLoaderMsgs;
import org.openecomp.modelloader.util.JsonXmlConverter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class ModelArtifactParser {

	private static String MODELS_ELEMENT = "models";
	private static String MODEL_ELEMENT = "model";
	private static String NAMED_QUERIES_ELEMENT = "named-queries";
	private static String NAMED_QUERY_ELEMENT = "named-query";
	private static String MODEL_NAME_VERSION_ID = "model-name-version-id";
	private static String MODEL_VER = "model-ver";
	private static String MODEL_VERSION_ID = "model-version-id";
	private static String MODEL_INVARIANT_ID = "model-invariant-id";
	private static String NAMED_QUERY_VERSION_ID = "named-query-uuid";
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
	    // Artifact could be JSON or XML
	    if (JsonXmlConverter.isValidJson(payload)) {
	      payload = JsonXmlConverter.convertJsonToXml(payload);
	    }

	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(payload));
	    Document doc = builder.parse(is);

	    if ( (doc.getDocumentElement().getNodeName().equalsIgnoreCase(MODEL_ELEMENT)) || 
	        (doc.getDocumentElement().getNodeName().equalsIgnoreCase(NAMED_QUERY_ELEMENT)) ) {

	      ModelArtifact model = parseModel(doc.getDocumentElement(), payload);

	      if (model != null) {
	        if ( ArtifactType.MODEL.equals(model.getType())) {
	          logger.info( ModelLoaderMsgs.DISTRIBUTION_EVENT, "Model parsed =====>>>> "
	              + "Model-invariant-Id: "+ model.getModelInvariantId()
	              + " Model-Version-Id: "+ model.getModelVerId());
	        }
	        modelList.add(model);
	      }
	      else {
	        logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName);
	        return null;
	      }
	    }
	    else if ( (doc.getDocumentElement().getNodeName().equalsIgnoreCase(MODELS_ELEMENT)) ||
	        (doc.getDocumentElement().getNodeName().equalsIgnoreCase(NAMED_QUERIES_ELEMENT)) ) {
	      // The complete set of models/named-queries were contained in this artifact
	      NodeList nodeList = doc.getDocumentElement().getChildNodes();
	      for (int i = 0; i < nodeList.getLength(); i++) {
	        Node childNode = nodeList.item(i);
	        if ( (childNode.getNodeName().equalsIgnoreCase(MODEL_ELEMENT)) || 
	            (childNode.getNodeName().equalsIgnoreCase(NAMED_QUERY_ELEMENT)) ) {
	          String modelPayload = nodeToString(childNode);
	          ModelArtifact model = parseModel(childNode, modelPayload);
	          if (model != null) {
	            modelList.add(model);
	          }
	          else {
	            logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName);
	            modelList.clear();
	            break;
	          }
	        }
	      }
	    }
	  }
	  catch (Exception ex) {
	    logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName + ": " + ex.getLocalizedMessage());
	  }

	  return modelList;
	}

	private void printDetails(Node modelVer) throws TransformerException {
	  logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, nodeToString(modelVer));
	}

	private ModelArtifact parseModel(Node modelNode, String payload) {
	  ModelArtifact model = new ModelArtifact();
	  model.setPayload(payload);

	  if (modelNode.getNodeName().equalsIgnoreCase(MODEL_ELEMENT)) {
	    //compare with Model-ver
	    model.setType(ArtifactType.MODEL);
	  }
	  else {
	    model.setType(ArtifactType.NAMED_QUERY);
	  }

	  Element e = (Element)modelNode;
	  model.setModelNamespace(e.getAttribute("xmlns"));

	  parseNode(modelNode, model);

	  if (model.getModelInvariantId() == null  && model.getNameVersionId() == null) {
	    return null;
	  }

	  return model;
	}

	private void parseNode(Node node, ModelArtifact model) {

	  if(node.getNodeName().equalsIgnoreCase(MODEL_NAME_VERSION_ID)){
	    model.setModelVerId(node.getTextContent().trim());
	    model.setV9Artifact(false);
	  }
	  else if(node.getNodeName().equalsIgnoreCase("model-id")){
	    model.setModelInvariantId(node.getTextContent().trim());
	    model.setV9Artifact(false);
	  }

	  else if (node.getNodeName().equalsIgnoreCase(MODEL_INVARIANT_ID)) {
	    model.setModelInvariantId(node.getTextContent().trim());
	  }
	  else if (node.getNodeName().equalsIgnoreCase(MODEL_VERSION_ID)) {
	    model.setModelVerId(node.getTextContent().trim());
	    //Change to Model Invariant Id
	  }
	  else if (node.getNodeName().equalsIgnoreCase(NAMED_QUERY_VERSION_ID)) {
	    model.setNameVersionId(node.getTextContent().trim());
	  }
	  else if (node.getNodeName().equalsIgnoreCase(RELATIONSHIP_DATA)) {
	    parseRelationshipNode(node, model);
	    if(model.getModelModelInvariantId()!=null && model.getModelVerModelVersionId()!=null && !model.getModelModelInvariantId().isEmpty() && !model.getModelVerModelVersionId().isEmpty()){
	      model.addDependentModelId(model.getModelModelInvariantId() + "|" + model.getModelVerModelVersionId());
	      model.setModelModelInvariantId("");
	      model.setModelVerModelVersionId("");
	    }
	  }
	  else {

	    if (node.getNodeName().equalsIgnoreCase(MODEL_VER)) {
	      model.setModelVer(node);
	      if ( (model.getModelNamespace() != null) && (!model.getModelNamespace().isEmpty()) ) {
	        Element e = (Element) node;
	        e.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", model.getModelNamespace());
	        System.out.println("Setting NS: " + e.getAttribute("xmlns"));
	      }
	    }

	    NodeList nodeList = node.getChildNodes();

	    for (int i = 0; i < nodeList.getLength(); i++) {
	      Node childNode = nodeList.item(i); 
	      parseNode(childNode, model);
	    }
	  }
	}

	private void parseRelationshipNode(Node node, ModelArtifact model) {

	  //invariant-id comes before model-version-id .. create a list of values
	  String key = null;
	  String value = null;
	  String modelVersionIdKey=null;
	  String modelInvariantIdIdKey=null;
	  String modelVersionIdValue=null;
	  String modelInvariantIdIdValue=null;

	  NodeList nodeList = node.getChildNodes();
	  for (int i = 0; i < nodeList.getLength(); i++) {
	    Node childNode = nodeList.item(i);


	    if (childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_KEY)) {
	      key = childNode.getTextContent().trim();
	      if(key.equalsIgnoreCase(MODEL_VER_ELEMENT_RELATIONSHIP_KEY)){
	        modelVersionIdKey = key;
	      }
	      else if(key.equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY)){
	        modelInvariantIdIdKey = key;
	      }
	    }
	    else if (childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_VALUE)) {
	      value = childNode.getTextContent().trim();
	      if(modelVersionIdKey!=null){
	        modelVersionIdValue = value;
	        model.setModelVerModelVersionId(modelVersionIdValue);
	      }
	      else if(modelInvariantIdIdKey!=null){
	        modelInvariantIdIdValue = value;
	        model.setModelModelInvariantId(modelInvariantIdIdValue);
	      } 

	    }
	  }

	  if ( (key != null) && (key.equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY )) && 
	      (model.isV9Artifact == false ||ArtifactType.NAMED_QUERY.equals(model.getType())) ) {
	    if (value != null) {
	      model.addDependentModelId(value);
	    }
	  }
	}

	private String nodeToString(Node node) throws TransformerException {
	  StringWriter sw = new StringWriter();
	  Transformer t = TransformerFactory.newInstance().newTransformer();
	  t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	  t.transform(new DOMSource(node), new StreamResult(sw));
	  return sw.toString();
	}
}
