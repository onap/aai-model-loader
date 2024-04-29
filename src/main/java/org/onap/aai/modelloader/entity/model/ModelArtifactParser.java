/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.modelloader.entity.model;

import java.io.StringWriter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class ModelArtifactParser extends AbstractModelArtifactParser {

    public static final String MODEL_VER = "model-ver";
    public static final String MODEL_VERSION_ID = "model-version-id";
    public static final String MODEL_INVARIANT_ID = "model-invariant-id";
    private static final String RELATIONSHIP = "relationship";
    private static final String MODEL_ELEMENT_RELATIONSHIP_KEY = "model." + MODEL_INVARIANT_ID;
    private static final String MODEL_VER_ELEMENT_RELATIONSHIP_KEY = MODEL_VER + "." + MODEL_VERSION_ID;

    private static Logger logger = LoggerFactory.getInstance().getLogger(ModelArtifactParser.class.getName());

    @Override
    void parseNode(Node node, IModelArtifact model) {
        if (node.getNodeName().equalsIgnoreCase(MODEL_INVARIANT_ID)
                || node.getNodeName().equalsIgnoreCase(MODEL_VERSION_ID)) {
            setVersionId(model, node);
        } else if (node.getNodeName().equalsIgnoreCase(RELATIONSHIP)) {
            parseRelationshipNode(node, model);
        } else {
            if (node.getNodeName().equalsIgnoreCase(MODEL_VER)) {
                String modelVersion;
                try {
                    modelVersion = nodeToString(node);
                    ((ModelArtifact) model).setModelVer(modelVersion);
                } catch (TransformerException e) {
                    logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Failed to parse resource version for input: " + node.toString());
                }
                if (((ModelArtifact) model).getModelNamespace() != null
                        && !((ModelArtifact) model).getModelNamespace().isEmpty()) {
                    Element e = (Element) node;
                    e.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns",
                            ((ModelArtifact) model).getModelNamespace());
                }
            }

            parseChildNodes(node, model);
        }
    }

    private String nodeToString(Node node) throws TransformerException {
        StringWriter sw = new StringWriter();
        TransformerFactory transFact = TransformerFactory.newInstance();
        transFact.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transFact.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transFact.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer t = transFact.newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));
        return sw.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setVersionId(IModelArtifact model, Node node) {
        if (MODEL_INVARIANT_ID.equals(node.getNodeName())) {
            ((ModelArtifact) model).setModelInvariantId(node.getTextContent().trim());
        } else if (MODEL_VERSION_ID.equals(node.getNodeName())) {
            ((ModelArtifact) model).setModelVerId(node.getTextContent().trim());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ModelId buildModelId(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item)
                .filter(childNode -> childNode.getNodeName().equalsIgnoreCase(RELATIONSHIP_DATA))
                .map(this::getRelationship) //
                .collect(Collector.of(ModelId::new, ModelId::setRelationship, (m, p) -> m));
    }

    /**
     * Find a relationship key and value pair from the children of the supplied node.
     *
     * @param node containing children storing relationship keys and values
     * @return a pair containing a relationship key and its value. Note: if multiple relationships are found, existing
     *         values stored in the pair will be overwritten.
     */
    private Pair<String, String> getRelationship(Node node) {
        Objects.requireNonNull(node);
        NodeList relDataChildList = node.getChildNodes();
        Objects.requireNonNull(relDataChildList);

        return IntStream.range(0, relDataChildList.getLength()).mapToObj(relDataChildList::item)
                .filter(this::filterRelationshipNode)
                .collect(Collector.of(Pair::new, applyRelationshipValue, (p, n) -> p));
    }

    /**
     * This method is responsible for creating an instance of {@link ModelArtifactParser.ModelId}
     *
     * @return IModelId instance of {@link ModelArtifactParser.ModelId}
     */
    @Override
    IModelId createModelIdInstance() {
        return new ModelId();
    }

    private class ModelId implements IModelId {

        private String modelInvariantIdValue;
        private String modelVersionIdValue;

        @Override
        public void setRelationship(Pair<String, String> p) {
            if (p.getKey().equalsIgnoreCase(MODEL_VER_ELEMENT_RELATIONSHIP_KEY)) {
                modelVersionIdValue = p.getValue();
            } else if (p.getKey().equalsIgnoreCase(MODEL_ELEMENT_RELATIONSHIP_KEY)) {
                modelInvariantIdValue = p.getValue();
            }
        }

        @Override
        public boolean defined() {
            return modelInvariantIdValue != null && modelVersionIdValue != null;
        }

        @Override
        public String toString() {
            return modelInvariantIdValue + "|" + modelVersionIdValue;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String buildArtifactParseExceptionMessage(String artifactName, String localisedMessage) {
        return "Unable to parse legacy model artifact " + artifactName + ": " + localisedMessage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    IModelArtifact createModelArtifactInstance() {
        return new ModelArtifact();
    }

    @Override
    boolean modelIsValid(IModelArtifact model) {
        return ((ModelArtifact) model).getModelInvariantId() != null && ((ModelArtifact) model).getModelVerId() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean processParsedModel(List<Artifact> modelList, String artifactName, IModelArtifact model) {
        boolean valid = false;

        if (model != null) {
            ModelArtifact modelImpl = (ModelArtifact) model;
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Model parsed =====>>>> " + "Model-invariant-Id: "
                    + modelImpl.getModelInvariantId() + " Model-Version-Id: " + modelImpl.getModelVerId());
            modelList.add(modelImpl);
            valid = true;
        } else {
            logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName);
        }

        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getModelElementRelationshipKey() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getVersionIdNodeName() {
        return null;
    }
}
