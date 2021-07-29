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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GizmoEdgeOperation {
    private String operation;
    private String internalId;
    private GizmoEdge edge;

    public GizmoEdgeOperation(String op, String id, GizmoEdge edge) {
        this.operation = op;
        this.internalId = id;
        this.edge = edge;
    }

    public JsonElement toJsonElement() {
        JsonObject opObj = new JsonObject();
        JsonObject edgeObj = JsonParser.parseString(edge.toJson()).getAsJsonObject();

        opObj.addProperty(GizmoBulkPayload.OP_KEY, operation);
        opObj.add(internalId, edgeObj);

        return opObj;
    }

    public static GizmoEdgeOperation fromJsonElement(JsonElement element) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(element.getAsJsonObject().entrySet());

        String op = null;
        String id = null;
        GizmoEdge edge = null;

        for (Map.Entry<String, JsonElement> entry : entries) {
            if (entry.getKey().equalsIgnoreCase(GizmoBulkPayload.OP_KEY)) {
                op = entry.getValue().getAsString();
            } else {
                id = entry.getKey();
                edge = GizmoEdge.fromJson(entry.getValue().getAsJsonObject().toString());
            }
        }

        if (op == null) {
            // Use default
            op = GizmoBulkPayload.EXISTS_OP;
        }

        return new GizmoEdgeOperation(op, id, edge);
    }

    public String getOperation() {
        return operation;
    }

    public String getInternalId() {
        return internalId;
    }

    public GizmoEdge getEdge() {
        return edge;
    }
}
