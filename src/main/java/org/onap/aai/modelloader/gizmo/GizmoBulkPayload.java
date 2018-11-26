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
package org.onap.aai.modelloader.gizmo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

public class GizmoBulkPayload {

    public static final String ADD_OP = "add";
    public static final String UPDATE_OP = "modify";
    public static final String DELETE_OP = "delete";
    public static final String PATCH_OP = "patch";
    public static final String EXISTS_OP = "exists";
    public static final String ALL_OPS = "all";
    public static final String OP_KEY = "operation";


    private List<JsonElement> objects = new ArrayList<JsonElement>();
    private List<JsonElement> relationships = new ArrayList<JsonElement>();

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public String toJson() {
        return gson.toJson(this);
    }

    public static GizmoBulkPayload fromJson(String payload) {
        return gson.fromJson(payload, GizmoBulkPayload.class);
    }

    public List<JsonElement> getObjects() {
        return objects;
    }

    public void setObjects(List<JsonElement> objects) {
        this.objects = objects;
    }

    public List<JsonElement> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<JsonElement> relationships) {
        this.relationships = relationships;
    }

    public List<GizmoVertexOperation> getVertexOperations() {
        return getVertexOperations(ALL_OPS);
    }
    
    public List<GizmoVertexOperation> getVertexOperations(String opType) {
        List<GizmoVertexOperation> ops = new ArrayList<GizmoVertexOperation>();
        for (JsonElement v : getObjects()) {
            GizmoVertexOperation op = GizmoVertexOperation.fromJsonElement(v);
                        
            if ( (opType.equalsIgnoreCase(ALL_OPS)) || (op.getOperation().equalsIgnoreCase(opType)) ) {
                ops.add(op);
            }
        }
        
        return ops;
    }
    
    public void addVertexOperation(GizmoVertexOperation newOp) {
        objects.add(newOp.toJsonElement());
    }
    
    public List<GizmoEdgeOperation> getEdgeOperations() {
        return getEdgeOperations(ALL_OPS);
    }
    
    public List<GizmoEdgeOperation> getEdgeOperations(String opType) {
        List<GizmoEdgeOperation> ops = new ArrayList<GizmoEdgeOperation>();

        for (JsonElement v : getRelationships()) {
            GizmoEdgeOperation op = GizmoEdgeOperation.fromJsonElement(v);
                        
            if ( (opType.equalsIgnoreCase(ALL_OPS)) || (op.getOperation().equalsIgnoreCase(opType)) ) {
                ops.add(op);
            }
        }
        
        return ops;
    }
    
    public void addEdgeOperation(GizmoEdgeOperation newOp) {
        relationships.add(newOp.toJsonElement());
    }
}
