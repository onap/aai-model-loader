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
package org.onap.aai.modelloader.util;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.onap.aai.modelloader.gizmo.GizmoBulkPayload;
import org.onap.aai.modelloader.gizmo.GizmoEdgeOperation;
import org.onap.aai.modelloader.gizmo.GizmoVertexOperation;

public class GizmoTranslatorTest {
    
    @Test
    public void translateXmlModel1() throws Exception {
        final String XML_MODEL_FILE = "src/test/resources/models/AAI-stellService-service-1.xml";

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(XML_MODEL_FILE));
            String originalXml = new String(encoded);
            
            String output = GizmoTranslator.translate(originalXml);
            System.out.println("Test1 Outgoing:\n" + output);
            
            GizmoBulkPayload request = GizmoBulkPayload.fromJson(output);
            
            List<GizmoVertexOperation> ops = request.getVertexOperations(GizmoBulkPayload.ADD_OP);
            assertTrue(ops.size() == 5);
            
            ops = request.getVertexOperations(GizmoBulkPayload.EXISTS_OP);
            assertTrue(ops.size() == 3);
            
            List<GizmoEdgeOperation> edgeOps = request.getEdgeOperations(GizmoBulkPayload.ADD_OP);
            assertTrue(edgeOps.size() == 7);                 
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }     
    }
    
    @Test
    public void translateXmlModel2() throws Exception {
        final String XML_MODEL_FILE2 = "src/test/resources/models/l3-network-widget.xml";

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(XML_MODEL_FILE2));
            String originalXml = new String(encoded);
            
            String output = GizmoTranslator.translate(originalXml);
            System.out.println("Test2 Outgoing:\n" + output);
            
            GizmoBulkPayload request = GizmoBulkPayload.fromJson(output);
            
            List<GizmoVertexOperation> ops = request.getVertexOperations(GizmoBulkPayload.ADD_OP);
            assertTrue(ops.size() == 2);
            
            ops = request.getVertexOperations(GizmoBulkPayload.EXISTS_OP);
            assertTrue(ops.size() == 0);
            
            List<GizmoEdgeOperation> edgeOps = request.getEdgeOperations(GizmoBulkPayload.ADD_OP);
            assertTrue(edgeOps.size() == 1);                     
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }     
    }
    
    @Test
    public void translateXmlNamedQuery() throws Exception {
        final String XML_MODEL_FILE3 = "src/test/resources/models/named-query-wan-connector.xml";

        try {
            byte[] encoded = Files.readAllBytes(Paths.get(XML_MODEL_FILE3));
            String originalXml = new String(encoded);
            
            String output = GizmoTranslator.translate(originalXml);
            System.out.println("Test3 Outgoing:\n" + output);
            
            GizmoBulkPayload request = GizmoBulkPayload.fromJson(output);
            
            List<GizmoVertexOperation> ops = request.getVertexOperations(GizmoBulkPayload.ADD_OP);
            assertTrue(ops.size() == 5);
            
            ops = request.getVertexOperations(GizmoBulkPayload.EXISTS_OP);
            assertTrue(ops.size() == 4);
            
            List<GizmoEdgeOperation> edgeOps = request.getEdgeOperations(GizmoBulkPayload.ADD_OP);
            assertTrue(edgeOps.size() == 8);                     
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }     
    }
}
