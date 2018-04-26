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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class provides common behaviour for implementations of IModelParser.
 *
 * Some of the common behaviour takes the form of abstract methods that will be implemented in concrete classes.
 *
 * Some other behaviour will be overridden in concrete classes.
 */
public abstract class AbstractModelArtifactParser implements IModelParser {
    private static Logger logger = LoggerFactory.getInstance().getLogger(AbstractModelArtifactParser.class);

    protected static final String RELATIONSHIP_DATA = "relationship-data";
    private static final String RELATIONSHIP_KEY = "relationship-key";
    private static final String RELATIONSHIP_VALUE = "relationship-value";

    BiConsumer<Pair<String, String>, Node> applyRelationshipValue = (p, n) -> {
        if (n.getNodeName().equalsIgnoreCase(RELATIONSHIP_KEY)) {
            p.setKey(n.getTextContent().trim());
        } else {
            p.setValue(n.getTextContent().trim());
        }
    };


    /**
     * This method is responsible for parsing the payload to produce a list of artifacts.
     *
     * @param artifactPayload the payload to be parsed
     * @param artifactName the name of the artifact to be parsed
     * @return List<Artifact> a list of artifacts that have been parsed from the payload.
     */
    @Override
    public List<Artifact> parse(String artifactPayload, String artifactName) {
        List<Artifact> modelList = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(artifactPayload));
            Document doc = builder.parse(is);

            IModelArtifact model = parseModel(doc.getDocumentElement(), artifactPayload);

            if (!processParsedModel(modelList, artifactName, model)) {
                modelList = null;
            }
        } catch (Exception ex) {
            logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR,
                    buildArtifactParseExceptionMessage(artifactName, ex.getLocalizedMessage()));
        }

        return modelList;
    }

    private IModelArtifact parseModel(Node modelNode, String payload) {
        IModelArtifact model = createModelArtifactInstance();
        model.setPayload(payload);

        Element e = (Element) modelNode;
        model.setModelNamespace(e.getAttribute("xmlns"));

        parseNode(modelNode, model);

        return modelIsValid(model) ? model : null;
    }

    /**
     * This method is responsible for creating a new instance of IModel that represents the model id for a concrete
     * implementation of IArtifactParser.
     *
     * @return IModelArtifact implementation of IModel that represents the model id for a concrete implementation of
     *         IArtifactParser
     */
    abstract IModelArtifact createModelArtifactInstance();

    /**
     * This method is responsible for the actual parsing of a node.
     *
     * It will do one of three things:
     * <ol>
     * <li>set the version id if the name of the node is the same as the name of the node that is the version Id</li>
     * <li>if the node is contains data about the relationship it will parse the node accordingly</li>
     * <li>if it does neither of option 1 or 2 it will parse the children of this node</li>
     * </ol>
     * 
     * @param node node to be parsed
     * @param model the model artifact to be updated with either the versionId or details of dependent node
     */
    void parseNode(Node node, IModelArtifact model) {
        if (node.getNodeName().equalsIgnoreCase(getVersionIdNodeName())) {
            setVersionId(model, node);
        } else if (node.getNodeName().equalsIgnoreCase(RELATIONSHIP_DATA)) {
            parseRelationshipNode(node, model);
        } else {
            parseChildNodes(node, model);
        }
    }

    /**
     * This method gets the name of the node that acts as the version Id for the node.
     *
     * @return String name of the node that acts as the version Id for the node
     */
    abstract String getVersionIdNodeName();

    /**
     * This method is responsible for setting the values on the model artifact that represent the version Id. Each
     * implementation of a IModelArtifact has its own properties that define the version Id.
     *
     * @param model the model artifact upon which the version Id will be set
     * @param node the source of the data that holds the actual value of the version id to be set on the model artifact
     */
    abstract void setVersionId(IModelArtifact model, Node node);

    /**
     * @param relationshipNode a node containing child nodes storing relationship data
     * @param model artifact whose dependent node id will be update with any relationship data if it exists
     */
    void parseRelationshipNode(Node relationshipNode, IModelArtifact model) {
        NodeList nodeList = getChildNodes(relationshipNode);

        IModelId modelId = buildModelId(nodeList);

        updateModelsDependentNodeId(model, modelId);
    }

    private NodeList getChildNodes(Node relationshipNode) {
        Objects.requireNonNull(relationshipNode);
        NodeList nodeList = relationshipNode.getChildNodes();
        Objects.requireNonNull(nodeList);

        return nodeList;
    }

    /**
     * This method is responsible for building an instance of IModelId representing the id of the model.
     *
     * @param nodeList list of modes used to build the model id.
     * @return IModelId instance of IModelId representing the id of the model
     */
    IModelId buildModelId(NodeList nodeList) {
        Pair<String, String> relationship = IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item) //
                .filter(this::filterRelationshipNode)
                .collect(Collector.of(Pair::new, applyRelationshipValue, (p, n) -> p));

        IModelId modelId = createModelIdInstance();
        modelId.setRelationship(relationship);

        return modelId;
    }

    /**
     * This method tests if a node is either one that either represents a relationship key or a relationship value.
     *
     * @param n the node to to be tested
     * @return <code>true</code> if the node is either represents a relationship key or a relationship value
     */
    boolean filterRelationshipNode(Node n) {
        return n.getNodeName().equalsIgnoreCase(RELATIONSHIP_KEY)
                || n.getNodeName().equalsIgnoreCase(RELATIONSHIP_VALUE);
    }

    /**
     * This method is responsible for creating an instance of {@link AbstractModelArtifactParser.ModelId}
     *
     * @return IModelId instance of {@link AbstractModelArtifactParser.ModelId}
     */
    IModelId createModelIdInstance() {
        return new ModelId();
    }

    private void updateModelsDependentNodeId(IModelArtifact model, IModelId modelId) {
        if (modelId.defined()) {
            model.addDependentModelId(modelId.toString());
        }
    }

    /**
     * This method is responsible for parsing the children of a given node.
     *
     * @param node node whose children, if any, should be parsed.
     * @param model model to be updated as a result of parsing the node
     */
    void parseChildNodes(Node node, IModelArtifact model) {
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            parseNode(childNode, model);
        }
    }

    /**
     * Validates if the mode is valid or not by examining specific properties of the model.
     *
     * @param model model to be validated
     * @return <code>true</code> if the mode is valid otherwise <code>false</code>
     */
    abstract boolean modelIsValid(IModelArtifact model);

    /**
     * This method is responsible for building a message used for logging artifact parsing errors.
     *
     * @param artifactName the name of the artifact
     * @param localisedMessage the message associated with the exception that is raised by the error
     * @return String a message used for logging artifact parsing errors
     */
    abstract String buildArtifactParseExceptionMessage(String artifactName, String localisedMessage);

    /**
     * This method is responsible for either adding the model artifact to the list of model artifacts or reporting an
     * error.
     *
     * If the model is not null then it will be added to the list of artifacts otherwise an error will be logged.
     *
     * @param modelList the list of artifacts to which the model will be added if it is not null
     * @param artifactName the name of the artifact
     * @param artifactModel the model artifact to be added to the list of model artifacts
     * @return <code>true/code> if the model is not null otherwise <code>false</code>
     */
    abstract boolean processParsedModel(List<Artifact> modelList, String artifactName, IModelArtifact artifactModel);

    private class ModelId implements IModelId {
        private String modelIdValue;

        @Override
        public void setRelationship(Pair<String, String> p) {
            if (getModelElementRelationshipKey().equals(p.getKey())) {
                modelIdValue = p.getValue();
            }
        }

        @Override
        public boolean defined() {
            return modelIdValue != null;
        }

        @Override
        public String toString() {
            return modelIdValue;
        }
    }

    /**
     * This method gets the name of the key of the element relationship for the model.
     *
     * @return String name of the key of the element relationship for the model
     */
    abstract String getModelElementRelationshipKey();
}
