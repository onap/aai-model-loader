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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jline.utils.Log;
import org.onap.aai.modelloader.entity.Artifact;

/**
 * Utility class to sort the given Models according to their dependencies.<br>
 * Example: Given a list of Models [A, B, C] <br>
 * where B depends on A, and A depends on C, the sorted result will be [C, A, B]
 */
public class ModelSorter {

    /**
     * Wraps a Model object to form dependencies other Models using Edges.
     */
    static class Node {

        private final AbstractModelArtifact model;
        private final HashSet<Edge> inEdges;
        private final HashSet<Edge> outEdges;

        public Node(AbstractModelArtifact model) {
            this.model = model;
            inEdges = new HashSet<>();
            outEdges = new HashSet<>();
        }

        public Node addEdge(Node node) {
            Edge edge = new Edge(this, node);
            outEdges.add(edge);
            node.inEdges.add(edge);
            return this;
        }

        @Override
        public String toString() {
            return model.getUniqueIdentifier();
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || this.getClass() != other.getClass()) {
                return false;
            }
            AbstractModelArtifact otherModel = ((Node) other).model;
            return this.model.getUniqueIdentifier().equals(otherModel.getUniqueIdentifier());
        }

        @Override
        public int hashCode() {
            return this.model.getUniqueIdentifier().hashCode();
        }
    }

    /**
     * Represents a dependency between two Nodes.
     */
    static class Edge {

        public final Node from;
        public final Node to;

        public Edge(Node from, Node to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            Edge edge = (Edge) obj;
            return edge.from == from && edge.to == to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.from, this.to);
        }
    }

    /**
     * Returns the list of models sorted by order of dependency.
     *
     * @param originalList the list that needs to be sorted
     * @return a list of sorted models
     * @throws BabelArtifactParsingException
     */
    public List<Artifact> sort(List<Artifact> originalList) throws BabelArtifactParsingException {
        if (originalList == null || originalList.size() <= 1) {
            return originalList;
        }

        Collection<Node> sortedNodes = sortNodes(createNodes(originalList));

        List<Artifact> sortedModelsList = new ArrayList<>(sortedNodes.size());
        for (Node node : sortedNodes) {
            sortedModelsList.add(node.model);
        }

        return sortedModelsList;
    }

    /**
     * Create nodes from the list of models and their dependencies.
     *
     * @param models what the nodes creation is based upon
     * @return Collection of Node objects
     */
    private Collection<Node> createNodes(Collection<Artifact> models) {

        // load list of models into a map, so we can later replace referenceIds with real Models
        Map<String, AbstractModelArtifact> versionIdToModelMap = new HashMap<>();
        for (Artifact art : models) {
            AbstractModelArtifact ma = (AbstractModelArtifact) art;
            versionIdToModelMap.put(ma.getUniqueIdentifier(), ma);
        }

        Map<String, Node> nodes = new HashMap<>();
        // create a node for each model and its referenced models
        for (Artifact art : models) {

            AbstractModelArtifact model = (AbstractModelArtifact) art;

            // node might have been created by another model referencing it
            Node node = nodes.get(model.getUniqueIdentifier());

            if (null == node) {
                node = new Node(model);
                nodes.put(model.getUniqueIdentifier(), node);
            }

            for (String referencedModelId : model.getDependentModelIds()) {
                // node might have been created by another model referencing it
                Node referencedNode = nodes.get(referencedModelId);

                if (null == referencedNode) {
                    // create node
                    AbstractModelArtifact referencedModel = versionIdToModelMap.get(referencedModelId);
                    if (referencedModel == null) {
                        Log.debug("ignoring " + referencedModelId);
                        continue; // referenced model not supplied, no need to sort it
                    }
                    referencedNode = new Node(referencedModel);
                    nodes.put(referencedModelId, referencedNode);
                }
                referencedNode.addEdge(node);
            }
        }

        return nodes.values();
    }

    /**
     * Sorts the given Nodes by order of dependency.
     *
     * @param unsortedNodes the collection of nodes to be sorted
     * @return a sorted collection of the given nodes
     * @throws BabelArtifactParsingException
     */
    private Collection<Node> sortNodes(Collection<Node> unsortedNodes) throws BabelArtifactParsingException {
        // L <- Empty list that will contain the sorted elements
        List<Node> nodeList = new ArrayList<>();

        // S <- Set of all nodes with no incoming edges
        Set<Node> nodeSet = new HashSet<>();
        for (Node unsortedNode : unsortedNodes) {
            if (unsortedNode.inEdges.isEmpty()) {
                nodeSet.add(unsortedNode);
            }
        }

        // while S is non-empty do
        while (!nodeSet.isEmpty()) {
            // remove a node n from S
            Node node = nodeSet.iterator().next();
            nodeSet.remove(node);

            // insert n into L
            nodeList.add(node);

            // for each node m with an edge e from n to m do
            for (Iterator<Edge> it = node.outEdges.iterator(); it.hasNext();) {
                // remove edge e from the graph
                Edge edge = it.next();
                Node to = edge.to;
                it.remove();// Remove edge from n
                to.inEdges.remove(edge);// Remove edge from m

                // if m has no other incoming edges then insert m into S
                if (to.inEdges.isEmpty()) {
                    nodeSet.add(to);
                }
            }
        }
        // Check to see if all edges are removed
        boolean cycle = false;
        for (Node node : unsortedNodes) {
            if (!node.inEdges.isEmpty()) {
                cycle = true;
                break;
            }
        }
        if (cycle) {
            throw new BabelArtifactParsingException(
                    "Circular dependency present between models, topological sort not possible");
        }

        return nodeList;
    }

}
