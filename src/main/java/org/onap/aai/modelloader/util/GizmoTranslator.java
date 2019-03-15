/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
 * ===================================================================
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
 * ============LICENSE_END============================================
 */

package org.onap.aai.modelloader.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.gizmo.GizmoBulkPayload;
import org.onap.aai.modelloader.gizmo.GizmoEdge;
import org.onap.aai.modelloader.gizmo.GizmoEdgeOperation;
import org.onap.aai.modelloader.gizmo.GizmoVertex;
import org.onap.aai.modelloader.gizmo.GizmoVertexOperation;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GizmoTranslator {

    private enum NodeType {
        VERTEX, ATTRIBUTE, CONTAINER, RELATIONSHIP_LIST, RELATIONSHIP, RELATED_TO, RELATIONSHIP_DATA, RELATIONSHIP_KEY, RELATIONSHIP_VALUE, MODEL_ELEMENT_VERTEX, NQ_ELEMENT_VERTEX, UNKNOWN
    }

    private static Logger logger = LoggerFactory.getInstance().getLogger(GizmoTranslator.class.getName());

    public static String translate(String xmlPayload) throws ParserConfigurationException, SAXException, IOException {
        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Process XML model artifact: " + xmlPayload);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlPayload));
        Document doc = builder.parse(is);

        GizmoBulkPayload gizmoPayload = new GizmoBulkPayload();

        processNode(doc.getDocumentElement(), null, null, gizmoPayload);

        return gizmoPayload.toJson();
    }

    private static void processNode(Node node, Node parentNode, GizmoVertexOperation parentVertexOp,
            GizmoBulkPayload gizmoPayload) {
        if (!(node instanceof Element)) {
            return;
        }

        Node newParent = null;
        NodeType nodeType = getNodeType(node);

        switch (nodeType) {
            case VERTEX:
            case MODEL_ELEMENT_VERTEX:
            case NQ_ELEMENT_VERTEX:
                parentVertexOp = createGizmoVertexOp(node, GizmoBulkPayload.ADD_OP);
                gizmoPayload.addVertexOperation(parentVertexOp);
                if (parentNode != null) {
                    gizmoPayload.addEdgeOperation(createGizmoEdgeOp(node, parentNode));
                }
                newParent = node;
                break;
            case RELATIONSHIP:
                processRelationship((Element) node, parentVertexOp, gizmoPayload);
                newParent = parentNode;
                break;
            default:
                newParent = parentNode;
                break;
        }

        NodeList childNodes = node.getChildNodes();
        for (int ix = 0; ix < childNodes.getLength(); ix++) {
            processNode(childNodes.item(ix), newParent, parentVertexOp, gizmoPayload);
        }
    }

    private static void processRelationship(Element relationshipNode, GizmoVertexOperation sourceNode,
            GizmoBulkPayload gizmoPayload) {
        NodeList relatedToList = relationshipNode.getElementsByTagName("related-to");
        if (relatedToList.getLength() != 1) {
            logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Unable to resolve relationship");
            return;
        }

        GizmoVertex targetVertex = new GizmoVertex();
        targetVertex.setType(relatedToList.item(0).getTextContent().trim());

        NodeList relationData = relationshipNode.getElementsByTagName("relationship-data");
        for (int ix = 0; ix < relationData.getLength(); ix++) {
            Element relationNode = (Element) relationData.item(ix);
            NodeList keyList = relationNode.getElementsByTagName("relationship-key");
            NodeList valueList = relationNode.getElementsByTagName("relationship-value");

            if ((keyList.getLength() != 1) || (valueList.getLength() != 1)) {
                logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
                        "Unable to resolve relationship.  Missing key/value.");
                return;
            }

            String[] keyBits = keyList.item(0).getTextContent().trim().split("\\.");
            String value = valueList.item(0).getTextContent().trim();

            if (keyBits[0].equalsIgnoreCase(targetVertex.getType())) {
                targetVertex.setProperty(keyBits[1], value);
            }
        }

        gizmoPayload.addVertexOperation(
                new GizmoVertexOperation(GizmoBulkPayload.EXISTS_OP, getVertexId(targetVertex), targetVertex));

        GizmoEdge edge = new GizmoEdge();

        edge.setSource("$" + getVertexId(sourceNode.getVertex()));
        edge.setTarget("$" + getVertexId(targetVertex));

        gizmoPayload.addEdgeOperation(
                new GizmoEdgeOperation(GizmoBulkPayload.ADD_OP, edge.getSource() + "_" + edge.getTarget(), edge));
    }

    private static GizmoEdgeOperation createGizmoEdgeOp(Node node, Node parentNode) {
        GizmoEdge edge = new GizmoEdge();

        edge.setSource("$" + getVertexId(createGizmoVertex(node)));
        edge.setTarget("$" + getVertexId(createGizmoVertex(parentNode)));

        return new GizmoEdgeOperation(GizmoBulkPayload.ADD_OP, edge.getSource() + "_" + edge.getTarget(), edge);
    }

    private static GizmoVertexOperation createGizmoVertexOp(Node node, String operationType) {
        GizmoVertex vertex = createGizmoVertex(node);
        return new GizmoVertexOperation(operationType, getVertexId(vertex), vertex);
    }

    private static String getVertexId(GizmoVertex vertex) {
        StringBuilder sb = new StringBuilder();
        sb.append(vertex.getType());
        for (Map.Entry<String, String> entry : vertex.getProperties().entrySet()) {
            sb.append("-" + entry.getValue());
        }

        return sb.toString();
    }

    private static GizmoVertex createGizmoVertex(Node node) {
        GizmoVertex vertex = new GizmoVertex();
        vertex.setType(node.getNodeName().trim());

        NodeList childNodes = node.getChildNodes();

        for (int ix = 0; ix < childNodes.getLength(); ix++) {
            if (getNodeType(childNodes.item(ix)).equals(NodeType.ATTRIBUTE)) {
                vertex.setProperty(childNodes.item(ix).getNodeName().trim(),
                        childNodes.item(ix).getTextContent().trim());
            }
        }

        // Special case for model-element, where we need to generate an id field
        if (getNodeType(node).equals(NodeType.MODEL_ELEMENT_VERTEX)) {
            vertex.setProperty("model-element-uuid", generateModelElementId((Element) node));
        }

        // Special case for nq-element, where we need to generate an id field
        if (getNodeType(node).equals(NodeType.NQ_ELEMENT_VERTEX)) {
            vertex.setProperty("named-query-element-uuid", generateModelElementId((Element) node));
        }

        return vertex;
    }

    // Generate a unique hash to store as the id for this node
    private static String generateModelElementId(Element node) {
        // Get the parent model version / named query version
        Optional<String> parentVersion = Optional.empty();

        Node parentNode = node.getParentNode();
        while (parentNode != null && !parentVersion.isPresent()) {
            if (getNodeType(parentNode) == NodeType.VERTEX) {
                NodeList childNodes = ((Element) parentNode).getElementsByTagName("*");
                parentVersion = IntStream.range(0, childNodes.getLength()) //
                        .mapToObj(childNodes::item) //
                        .filter(child -> child.getNodeName().equalsIgnoreCase("named-query-uuid")
                                || child.getNodeName().equalsIgnoreCase("model-version-id")) //
                        .map(child -> child.getTextContent().trim()) //
                        .findFirst();
            }
            parentNode = parentNode.getParentNode();
        }

        Set<String> elemSet = new HashSet<>();
        parentVersion.ifPresent(elemSet::add);

        Set<NodeType> validNodeTypes = //
                Stream.of(NodeType.ATTRIBUTE, NodeType.RELATIONSHIP_KEY, NodeType.RELATIONSHIP_VALUE)
                        .collect(Collectors.toSet());

        NodeList childNodes = node.getElementsByTagName("*");
        IntStream.range(0, childNodes.getLength()) //
                .mapToObj(childNodes::item) //
                .filter(child -> validNodeTypes.contains(getNodeType(child))) //
                .map(child -> child.getTextContent().trim()) //
                .forEachOrdered(elemSet::add);

        return Integer.toString(elemSet.hashCode());
    }

    private static NodeType getNodeType(Node node) {
        if (!(node instanceof Element)) {
            return NodeType.UNKNOWN;
        }

        if (node.getNodeName().equalsIgnoreCase("relationship-list")) {
            return NodeType.RELATIONSHIP_LIST;
        }

        if (node.getNodeName().equalsIgnoreCase("relationship")) {
            return NodeType.RELATIONSHIP;
        }

        if (node.getNodeName().equalsIgnoreCase("relationship-data")) {
            return NodeType.RELATIONSHIP_DATA;
        }

        if (node.getNodeName().equalsIgnoreCase("related-to")) {
            return NodeType.RELATED_TO;
        }

        if (node.getNodeName().equalsIgnoreCase("relationship-key")) {
            return NodeType.RELATIONSHIP_KEY;
        }

        if (node.getNodeName().equalsIgnoreCase("relationship-value")) {
            return NodeType.RELATIONSHIP_VALUE;
        }

        if (node.getNodeName().equalsIgnoreCase("model-element")) {
            return NodeType.MODEL_ELEMENT_VERTEX;
        }

        if (node.getNodeName().equalsIgnoreCase("named-query-element")) {
            return NodeType.NQ_ELEMENT_VERTEX;
        }

        NodeList childNodes = node.getChildNodes();
        int childElements = countChildElements(childNodes);

        if ((childElements == 0) && (node.getTextContent() != null) && (!node.getTextContent().trim().isEmpty())) {
            return NodeType.ATTRIBUTE;
        }

        for (int ix = 0; ix < childNodes.getLength(); ix++) {
            if (getNodeType(childNodes.item(ix)) == NodeType.ATTRIBUTE) {
                return NodeType.VERTEX;
            }
        }

        if (childElements > 0) {
            return NodeType.CONTAINER;
        }

        return NodeType.UNKNOWN;
    }

    static int countChildElements(NodeList nodes) {
        int count = 0;
        for (int ix = 0; ix < nodes.getLength(); ix++) {
            if (nodes.item(ix) instanceof Element) {
                count++;
            }
        }

        return count;
    }
}
