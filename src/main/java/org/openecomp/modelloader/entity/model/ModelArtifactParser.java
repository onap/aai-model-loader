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

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.modelloader.entity.Artifact;
import org.openecomp.modelloader.entity.ArtifactType;
import org.openecomp.modelloader.service.ModelLoaderMsgs;
import org.openecomp.modelloader.util.JsonXmlConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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
  private static String NAMED_QUERY_VERSION_ID = "named-query-uuid";
  private static String RELATIONSHIP_DATA = "relationship-data";
  private static String RELATIONSHIP_KEY = "relationship-key";
  private static String RELATIONSHIP_VALUE = "relationship-value";
  private static String MODEL_ELEMENT_RELATIONSHIP_KEY = "model.model-name-version-id";

  private static Logger logger = LoggerFactory.getInstance()
      .getLogger(ModelArtifactParser.class.getName());

  /**
   * This method parses the given artifact payload in byte array format and
   * generates a list of model artifacts according to the content.
   * 
   * @param artifactPayload
   *          artifact content to be parsed
   * @param artifactName
   *          name of the artifact
   * @return a list of model artifacts
   */
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

      if ((doc.getDocumentElement().getNodeName().equalsIgnoreCase(MODEL_ELEMENT))
          || (doc.getDocumentElement().getNodeName().equalsIgnoreCase(NAMED_QUERY_ELEMENT))) {
        ModelArtifact model = parseModel(doc.getDocumentElement(), payload);
        if (model != null) {
          modelList.add(model);
        } else {
          // TODO: A WARN message?
          logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
              "Unable to parse artifact " + artifactName);
        }
      } else if ((doc.getDocumentElement().getNodeName().equalsIgnoreCase(MODELS_ELEMENT))
          || (doc.getDocumentElement().getNodeName().equalsIgnoreCase(NAMED_QUERIES_ELEMENT))) {
        // The complete set of models/named-queries were contained in this
        // artifact
        NodeList nodeList = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
          Node childNode = nodeList.item(i);
          if ((childNode.getNodeName().equalsIgnoreCase(MODEL_ELEMENT))
              || (childNode.getNodeName().equalsIgnoreCase(NAMED_QUERY_ELEMENT))) {
            String modelPayload = nodeToString(childNode);
            ModelArtifact model = parseModel(childNode, modelPayload);
            if (model != null) {
              modelList.add(model);
            } else {
              // TODO: A WARN message?
              logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                  "Unable to parse artifact " + artifactName);
              modelList.clear();
              break;
            }
          }
        }
      }
    } catch (Exception ex) {
      // This may not be an error. We may be receiving an artifact that is
      // unrelated
      // to models. In this case, we just ignore it.
      // TODO: A WARN message?
      logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
          "Unable to parse artifact " + artifactName + ": " + ex.getLocalizedMessage());
    }

    return modelList;
  }

  private ModelArtifact parseModel(Node modelNode, String payload) {
    ModelArtifact model = new ModelArtifact();
    model.setPayload(payload);

    if (modelNode.getNodeName().equalsIgnoreCase(MODEL_ELEMENT)) {
      model.setType(ArtifactType.MODEL);
    } else {
      model.setType(ArtifactType.NAMED_QUERY);
    }

    parseNode(modelNode, model);

    if (model.getNameVersionId() == null) {
      return null;
    }

    return model;
  }

  private void parseNode(Node node, ModelArtifact model) {
    if (node.getNodeName().equalsIgnoreCase(MODEL_NAME_VERSION_ID)) {
      model.setNameVersionId(node.getTextContent().trim());
    } else if (node.getNodeName().equalsIgnoreCase(NAMED_QUERY_VERSION_ID)) {
      model.setNameVersionId(node.getTextContent().trim());
    } else if (node.getNodeName().equalsIgnoreCase(RELATIONSHIP_DATA)) {
      parseRelationshipNode(node, model);
    } else {
      NodeList nodeList = node.getChildNodes();
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node childNode = nodeList.item(i);
        parseNode(childNode, model);
      }
    }
  }

  private void parseRelationshipNode(Node node, ModelArtifact model) {
    String key = null;
    String value = null;

    NodeList nodeList = node.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node childNode = nodeList.item(i);
      if (childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_KEY)) {
        key = childNode.getTextContent().trim();
      } else if (childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_VALUE)) {
        value = childNode.getTextContent().trim();
      }
    }

    if ((key != null) && (key.equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY))) {
      if (value != null) {
        model.addDependentModelId(value);
      }
    }
  }

  private String nodeToString(Node node) throws TransformerException {
    StringWriter sw = new StringWriter();
    Transformer transfomer = TransformerFactory.newInstance().newTransformer();
    transfomer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transfomer.transform(new DOMSource(node), new StreamResult(sw));
    return sw.toString();
  }
}
