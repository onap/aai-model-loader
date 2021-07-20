/**
 * ============LICENSE_START=======================================================
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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;
import org.onap.aai.modelloader.entity.Artifact;

public class TestNamedQueryArtifactParser {

    private static final String MODEL_FILE = "src/test/resources/models/named-query-wan-connector.xml";

    @Test
    public void testParseNamedQuery() throws Exception {
        try {
            String fileString = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));

            NamedQueryArtifactParser parser = new NamedQueryArtifactParser();
            List<Artifact> modelList = parser.parse(fileString, "test-artifact");

            assertEquals(1, modelList.size());

            NamedQueryArtifact model = (NamedQueryArtifact) modelList.get(0);

            assertThat(model.toString(), not(isEmptyString()));
            assertTrue(model.getNamedQueryUuid().equalsIgnoreCase("94cac189-8d88-4d63-a194-f44214e080ff"));
            assertTrue(model.getType().toString().equalsIgnoreCase("NAMED_QUERY"));
            assertEquals(4, model.getDependentModelIds().size());
            assertTrue(model.getDependentModelIds().contains("d09dd9da-0148-46cd-a947-591afc844d24"));
            assertTrue(model.getDependentModelIds().contains("997fc7-fca1-451f-b953-9a1e6197b4d6"));
            assertTrue(model.getDependentModelIds().contains("897df7ea-8938-42b0-bc57-46e913a4d93b"));
            assertTrue(model.getDependentModelIds().contains("f2b24d95-c582-48d5-b2d6-c5b3a94ce812"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}
