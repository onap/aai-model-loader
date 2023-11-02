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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.onap.aai.modelloader.entity.Artifact;
import org.springframework.util.CollectionUtils;

public class TestModelArtifactParser {

    private static final String MODEL_FILE = "src/test/resources/models/l3-network-widget.xml";
    private static final String MODEL_FILE_SERVICE = "src/test/resources/models/AAI-stellService-service-1.xml";
    private static final String MODEL_FILE_INCOMPLETE = "src/test/resources/models/incomplete-model.xml";
    private static final String MODEL_FILE_INVALID = "src/test/resources/models/invalid-model.xml";

    @Test
    public void testParseModelFileNoDeps() throws Exception {
        String fileString = new String(Files.readAllBytes(Paths.get(MODEL_FILE)));

        ModelArtifactParser parser = new ModelArtifactParser();
        List<Artifact> modelList = parser.parse(fileString, "test-artifact");

        assertThat(modelList.size(), is(1));

        ModelArtifact model = (ModelArtifact) modelList.get(0);
        assertThat(model.toString(), is(not(nullValue())));

        assertThat(model.getModelInvariantId(), equalToIgnoringCase("3d560d81-57d0-438b-a2a1-5334dba0651a"));
        assertThat(model.getModelNamespace(), equalToIgnoringCase("http://org.onap.aai.inventory/v25"));
        assertThat(model.getModelNamespaceVersion(), equalToIgnoringCase("v25"));
        assertThat(model.getType().toString(), equalToIgnoringCase("MODEL"));
        assertThat(model.getDependentModelIds().size(), is(0));
    }

    @Test
    public void testParseModelFileDeps() throws Exception {
        String fileString = new String(Files.readAllBytes(Paths.get(MODEL_FILE_SERVICE)));

        ModelArtifactParser parser = new ModelArtifactParser();
        List<Artifact> modelList = parser.parse(fileString, "test-artifact");

        assertThat(modelList.size(), is(1));

        ModelArtifact model = (ModelArtifact) modelList.get(0);
        assertThat(model.toString(), is(not(nullValue())));
        assertThat(model.getModelInvariantId(), equalToIgnoringCase("fedf9da3-6a74-4813-8fa2-221a98b0e7ad"));
        assertThat(model.getModelVerId(), equalToIgnoringCase("e0373537-7f66-4094-9939-e2f5de6ff5f6"));
        assertThat(model.getType().toString(), equalToIgnoringCase("MODEL"));
        assertThat(model.getDependentModelIds().size(), is(3));
        assertThat(model.getDependentModelIds()
                .contains("5c12984d-db0f-4300-a0e0-9791775cc40f|88bdbadf-db8a-490f-881e-c8effcbc3f66"), is(true));
        assertThat(model.getDependentModelIds()
                .contains("959b7c09-9f34-4e5f-8b63-505381db176e|374d0899-bbc2-4403-9320-fe9bebef75c6"), is(true));
    }

    @Test
    public void testParseModelFileInvalidArtifact() throws Exception {
        String fileString = new String(Files.readAllBytes(Paths.get(MODEL_FILE_INVALID)));

        ModelArtifactParser parser = new ModelArtifactParser();
        List<Artifact> modelList = parser.parse(fileString, "test-artifact");

        assertThat(CollectionUtils.isEmpty(modelList), is(true));
    }

    @Test
    public void testParseModelFileIncompleteArtifact() throws Exception {
        String fileString = new String(Files.readAllBytes(Paths.get(MODEL_FILE_INCOMPLETE)));

        ModelArtifactParser parser = new ModelArtifactParser();
        List<Artifact> modelList = parser.parse(fileString, "test-artifact");

        assertThat(CollectionUtils.isEmpty(modelList), is(true));
    }
}
