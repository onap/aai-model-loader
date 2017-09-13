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
import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
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


public class NamedQueryArtifactParser implements IModelParser {

  private static String NAMED_QUERY_VERSION_ID = "named-query-uuid";
	private static String RELATIONSHIP_DATA = "relationship-data";
	private static String RELATIONSHIP_KEY = "relationship-key";
	private static String RELATIONSHIP_VALUE = "relationship-value";
  private static String MODEL_ELEMENT_RELATIONSHIP_KEY = "model.model-invariant-id";

	
	private  static Logger logger = LoggerFactory.getInstance().getLogger(NamedQueryArtifactParser.class.getName());
	
	public List<Artifact> parse(byte[] artifactPayload, String artifactName) {
	  String payload = new String(artifactPayload);
	  List<Artifact> modelList = new ArrayList<Artifact>();

	  try {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(payload));
	    Document doc = builder.parse(is);

	    NamedQueryArtifact model = parseModel(doc.getDocumentElement(), payload);

	    if (model != null) {
	      logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Named-Query parsed =====>>>> " + "Named-Query-UUID: "+ model.getNamedQueryUuid());
	      modelList.add(model);
	    }
	    else {
	      logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse named-query artifact " + artifactName);
	      return null;
	    }
	  }
	  catch (Exception ex) {
	    logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse named-query artifact " + artifactName + ": " + ex.getLocalizedMessage());
	  }

	  return modelList;
	}

	private NamedQueryArtifact parseModel(Node modelNode, String payload) {
	  NamedQueryArtifact model = new NamedQueryArtifact();
	  model.setPayload(payload);

	  Element e = (Element)modelNode;
	  model.setModelNamespace(e.getAttribute("xmlns"));

	  parseNode(modelNode, model);

    if (model.getNamedQueryUuid() == null) {
      return null;
    }

	  return model;
	}

	private void parseNode(Node node, NamedQueryArtifact model) {
	  if (node.getNodeName().equalsIgnoreCase(NAMED_QUERY_VERSION_ID)) {
	    model.setNamedQueryUuid(node.getTextContent().trim());
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

	private void parseRelationshipNode(Node node, NamedQueryArtifact model) {
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
    
    if ( (key != null) && (key.equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY )) ) {
      if (value != null) {
        model.addDependentModelId(value);
      }
    }
	}
}
