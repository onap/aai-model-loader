/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.gizmo.GizmoBulkPayload;

public class TestGizmoTranslator {

    @Test
    public void translateInvalidXml() throws IOException {
        assertThrows(IOException.class, () -> {
            GizmoTranslator.translate("not valid XML");
        });
    }

    @Test
    public void translateXmlModel1() throws IOException {
        GizmoBulkPayload request = createBulkRequest("src/test/resources/models/AAI-stellService-service-1.xml");
        assertThat(request.getVertexOperations(GizmoBulkPayload.ADD_OP).size(), is(5));
        assertThat(request.getVertexOperations(GizmoBulkPayload.EXISTS_OP).size(), is(3));
        assertThat(request.getEdgeOperations(GizmoBulkPayload.ADD_OP).size(), is(7));
    }

    @Test
    public void translateXmlModel2() throws IOException {
        GizmoBulkPayload request = createBulkRequest("src/test/resources/models/l3-network-widget.xml");
        assertThat(request.getVertexOperations(GizmoBulkPayload.ADD_OP).size(), is(2));
        assertThat(request.getVertexOperations(GizmoBulkPayload.EXISTS_OP).size(), is(0));
        assertThat(request.getEdgeOperations(GizmoBulkPayload.ADD_OP).size(), is(1));
    }

    @Test
    public void translateXmlNamedQuery() throws IOException {
        GizmoBulkPayload request = createBulkRequest("src/test/resources/models/named-query-wan-connector.xml");
        assertThat(request.getVertexOperations(GizmoBulkPayload.ADD_OP).size(), is(5));
        assertThat(request.getVertexOperations(GizmoBulkPayload.EXISTS_OP).size(), is(4));
        assertThat(request.getEdgeOperations(GizmoBulkPayload.ADD_OP).size(), is(8));
    }

    private GizmoBulkPayload createBulkRequest(String filePath) throws IOException {
        final String xmlPayload = new String(Files.readAllBytes(Paths.get(filePath)));
        return GizmoBulkPayload.fromJson(GizmoTranslator.translate(xmlPayload));
    }

}
