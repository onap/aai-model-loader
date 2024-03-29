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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.entity.Artifact;

public class ModelSorterTest {

    @Test
    public void edgeEquality() throws BabelArtifactParsingException {
        ModelArtifact model = buildTestModel();
        ModelSorter.Node nodeA = new ModelSorter.Node(model);
        ModelSorter.Node nodeB = new ModelSorter.Node(model);

        ModelSorter.Edge edgeA = new ModelSorter.Edge(nodeA, nodeB);

        assertThat(edgeA, is(equalTo(edgeA)));
        assertThat(edgeA, is(not(equalTo(null))));
        assertThat(edgeA, is(not(equalTo(model))));

        ModelSorter.Edge edgeB = new ModelSorter.Edge(nodeA, nodeB);
        ModelSorter.Edge edgeC = new ModelSorter.Edge(nodeB, nodeA);

        ModelSorter.Node nodeC = new ModelSorter.Node(model);
        ModelSorter.Edge edgeD = new ModelSorter.Edge(nodeA, nodeC);
        assertThat(edgeA, is(equalTo(edgeB)));
        assertThat(edgeA, is(not(equalTo(edgeC))));
        assertThat(edgeA, is(not(equalTo(edgeD))));
    }

    @Test
    public void nodeEquality() throws BabelArtifactParsingException {
        ModelArtifact model = buildTestModel();
        ModelSorter.Node nodeA = new ModelSorter.Node(model);

        assertThat(nodeA, is(equalTo(nodeA)));
        assertThat(nodeA, is(not(equalTo(null))));
        assertThat(nodeA, is(not(equalTo(model))));

        ModelSorter.Node nodeB = new ModelSorter.Node(model);
        assertThat(nodeA, is(equalTo(nodeB)));
        assertThat(nodeA.toString(), is(equalTo(nodeB.toString())));
        assertThat(nodeA, is(not(equalTo(new ModelSorter.Node(new ModelArtifact())))));
    }

    @Test
    public void testToString() throws BabelArtifactParsingException {
        ModelArtifact model = buildTestModel();

        ModelSorter.Node nodeA = new ModelSorter.Node(model);
        assertThat(nodeA.toString(), not(isEmptyString()));

        ModelSorter.Node nodeB = new ModelSorter.Node(model);
        nodeA.addEdge(nodeB);
        assertThat(nodeA.toString(), not(isEmptyString()));
        assertThat(nodeB.toString(), not(isEmptyString()));

    }

    @Test
    public void noModels() throws BabelArtifactParsingException {
        assertThat(new ModelSorter().sort(null), is(nullValue()));
        assertThat(new ModelSorter().sort(Collections.emptyList()).size(), is(0));
    }

    @Test
    public void singleModel() throws BabelArtifactParsingException {
        assertThat(new ModelSorter().sort(Arrays.asList(buildTestModel())).size(), is(1));
    }

    @Test
    public void multipleModels() throws BabelArtifactParsingException {
        Artifact artA = buildTestModel("aaaa", "mvaaaa", "cccc|mvcccc");
        Artifact artB = buildTestModel("bbbb", "mvbbbb", "aaaa|mvaaaa");
        Artifact artC = buildTestModel("cccc", "mvcccc");
        List<Artifact> expected = Arrays.asList(artC, artA, artB);
        assertThat(new ModelSorter().sort(Arrays.asList(artA, artB, artC)), is(expected));
    }


    @Test
    public void multipleModelsWithMultipleIncomingEdges() throws BabelArtifactParsingException {
        ModelArtifact artA = buildTestModel("aaaa", "mvaaaa", "cccc|mvcccc");
        Artifact artB = buildTestModel("bbbb", "mvbbbb", "aaaa|mvaaaa");
        Artifact artC = buildTestModel("cccc", "mvcccc");
        Artifact artD = buildTestModel("dddd", "mvdddd", "cccc|mvcccc");
        artA.addDependentModelId("dddd|mvdddd");
        List<Artifact> expected = Arrays.asList(artC, artD, artA, artB);
        assertThat(new ModelSorter().sort(Arrays.asList(artA, artB, artC, artD)), is(expected));
    }

    @Test
    public void multipleModelsAndNamedQueries() throws BabelArtifactParsingException {
        Artifact artifact = buildTestModel("aaaa", "1111", "cccc|2222");
        Artifact nq1 = buildTestNamedQuery("nq1", "aaaa|1111");
        Artifact nq2 = buildTestNamedQuery("nqw", "existing-model");
        List<Artifact> expected = Arrays.asList(artifact, nq2, nq1);
        assertThat(new ModelSorter().sort(Arrays.asList(nq1, nq2, artifact)), is(expected));
    }

    @Test
    public void circularDependency() throws BabelArtifactParsingException {
        assertThrows(BabelArtifactParsingException.class, () -> {
            List<Artifact> modelList = new ArrayList<Artifact>();
            modelList.add(buildTestModel("aaaa", "1111", "bbbb|1111"));
            modelList.add(buildTestModel("bbbb", "1111", "aaaa|1111"));
            new ModelSorter().sort(modelList);
        });
    }

    private ModelArtifact buildTestModel() {
        return buildTestModel("aaa", "111", "xyz|123");
    }

    private ModelArtifact buildTestModel(String id, String version) {
        return buildTestModel(id, version, null);
    }

    private ModelArtifact buildTestModel(String id, String version, String dependentModel) {
        ModelArtifact modelArtifact = new ModelArtifact();
        modelArtifact.setModelInvariantId(id);
        modelArtifact.setModelVerId(version);
        if (dependentModel != null) {
            modelArtifact.addDependentModelId(dependentModel);
        }
        return modelArtifact;
    }

    private NamedQueryArtifact buildTestNamedQuery(String uuid, String modelId) {
        NamedQueryArtifact nq = new NamedQueryArtifact();
        nq.setNamedQueryUuid(uuid);
        nq.addDependentModelId(modelId);
        return nq;
    }

}
