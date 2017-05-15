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

import jline.internal.Log;

import org.openecomp.modelloader.entity.Artifact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to sort the given Models according to their dependencies.
 * Example: Given a list of Models [A, B, C] where B depends on A, and A depends
 * on C, the sorted result will be [C, A, B]
 */
public class ModelSorter {

  /**
   * Wraps a Model object to form dependencies other Models using Edges.
   */
  static class Node {
    private final ModelArtifact model;
    private final HashSet<Edge> inEdges;
    private final HashSet<Edge> outEdges;

    public Node(ModelArtifact model) {
      this.model = model;
      inEdges = new HashSet<Edge>();
      outEdges = new HashSet<Edge>();
    }

    public Node addEdge(Node node) {
      Edge edge = new Edge(this, node);
      outEdges.add(edge);
      node.inEdges.add(edge);
      return this;
    }

    @Override
    public String toString() {
      if (model.getModelInvariantId() == null) {
        return model.getNameVersionId();
      }
      
      return model.getModelInvariantId();
    }

    @Override
    public boolean equals(Object other) {
      ModelArtifact otherModel = ((Node) other).model;
      String modelId1 = this.model.getModelInvariantId();
      if (modelId1 == null) {
        modelId1 = this.model.getNameVersionId();
      }
      
      String modelId2 = otherModel.getModelInvariantId();
      if (modelId2 == null) {
        modelId2 = otherModel.getNameVersionId();
      }
      
      return modelId1.equals(modelId2);
    }

    @Override
    public int hashCode() {
      if (this.model.getModelInvariantId() == null) {
        return this.model.getNameVersionId().hashCode();
      }
      
      return this.model.getModelInvariantId().hashCode();
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
      Edge edge = (Edge) obj;
      return edge.from == from && edge.to == to;
    }
  }

  /**
   * Returns the list of models sorted by order of dependency.
   * 
   * @param originalList
   *          the list that needs to be sorted
   * @return a list of sorted models
   */
  public List<Artifact> sort(List<Artifact> originalList) {

    if (originalList.size() <= 1) {
      return originalList;
    }

    Collection<Node> nodes = createNodes(originalList);
    Collection<Node> sortedNodes = sortNodes(nodes);
    
    List<Artifact> sortedModelsList = new ArrayList<Artifact>(sortedNodes.size());
    for (Node node : sortedNodes) {
      sortedModelsList.add(node.model);
    }

    return sortedModelsList;
  }

  /**
   * Create nodes from the list of models and their dependencies.
   * 
   * @param models
   *          what the nodes creation is based upon
   * @return Collection of Node objects
   */
  private Collection<Node> createNodes(Collection<Artifact> models) {

    // load list of models into a map, so we can later replace referenceIds with
    // real Models
    HashMap<String, ModelArtifact> versionIdToModelMap = new HashMap<String, ModelArtifact>();
    for (Artifact art : models) {
      ModelArtifact ma = (ModelArtifact) art;
      versionIdToModelMap.put(ma.getModelModelVerCombinedKey(), ma);
    }

    HashMap<String, Node> nodes = new HashMap<String, Node>();
    // create a node for each model and its referenced models
    for (Artifact art : models) {

      ModelArtifact model = (ModelArtifact) art;
      
      // node might have been created by another model referencing it
      Node node = nodes.get(model.getModelModelVerCombinedKey());

      if (null == node) {
        node = new Node(model);
        nodes.put(model.getModelModelVerCombinedKey(), node);
      }

      for (String referencedModelId : model.getDependentModelIds()) {
        // node might have been created by another model referencing it
        Node referencedNode = nodes.get(referencedModelId);

        if (null == referencedNode) {
          // create node
          ModelArtifact referencedModel = versionIdToModelMap.get(referencedModelId);
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
   * @param originalList
   *          the collection of nodes to be sorted
   * @return a sorted collection of the given nodes
   */
  private Collection<Node> sortNodes(Collection<Node> unsortedNodes) {
    // L <- Empty list that will contain the sorted elements
    ArrayList<Node> nodeList = new ArrayList<Node>();

    // S <- Set of all nodes with no incoming edges
    HashSet<Node> nodeSet = new HashSet<Node>();
    for (Node unsortedNode : unsortedNodes) {
      if (unsortedNode.inEdges.size() == 0) {
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
      throw new RuntimeException(
          "Circular dependency present between models, topological sort not possible");
    }

    return nodeList;
  }


}
