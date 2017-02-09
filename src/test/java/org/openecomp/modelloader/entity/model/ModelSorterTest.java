/*-
 * ============LICENSE_START=======================================================
 * MODEL LOADER SERVICE
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.modelloader.entity.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.openecomp.modelloader.entity.Artifact;

public class ModelSorterTest {

  @Test
  public void noModels() {

    List<Artifact> emptyList = Collections.emptyList();

    ModelSorter sorter = new ModelSorter();
    sorter = new ModelSorter();

    List<Artifact> sortedList = sorter.sort(emptyList);
    assertNotNull(sortedList);
    assertEquals(0, sortedList.size());

  }

  @Test
  public void singleModel() {

    List<Artifact> modelList = new ArrayList<Artifact>();

    ModelArtifact model = new ModelArtifact();
    model.setNameVersionId("aaaaa");
    model.addDependentModelId("xyz");
    modelList.add(model);

    ModelSorter sorter = new ModelSorter();
    sorter = new ModelSorter();

    List<Artifact> sortedList = sorter.sort(modelList);
    assertNotNull(sortedList);
    assertEquals(1, sortedList.size());

  }

  /**
   * 
   * depends on depends on B ------> A -------> C
   *
   *
   * Input list = a, b, c Sorted list = c, a, b
   *
   */
  @Test
  public void multipleModels() {

    List<Artifact> modelList = new ArrayList<Artifact>();

    ModelArtifact aaaa = new ModelArtifact();
    aaaa.setNameVersionId("aaaa");
    aaaa.addDependentModelId("cccc");

    ModelArtifact bbbb = new ModelArtifact();
    bbbb.setNameVersionId("bbbb");
    bbbb.addDependentModelId("aaaa");

    ModelArtifact cccc = new ModelArtifact();
    cccc.setNameVersionId("cccc");

    modelList.add(aaaa);
    modelList.add(bbbb);
    modelList.add(cccc);

    ModelSorter sorter = new ModelSorter();
    sorter = new ModelSorter();

    List<Artifact> sortedList = sorter.sort(modelList);
    assertNotNull(sortedList);
    assertEquals(3, sortedList.size());

    assertEquals(cccc, sortedList.get(0));
    assertEquals(aaaa, sortedList.get(1));
    assertEquals(bbbb, sortedList.get(2));
  }

  @Test(expected = RuntimeException.class)
  public void circularDependency() {

    List<Artifact> modelList = new ArrayList<Artifact>();

    ModelArtifact aaaa = new ModelArtifact();
    aaaa.setNameVersionId("aaaa");
    aaaa.addDependentModelId("bbbb");

    ModelArtifact bbbb = new ModelArtifact();
    bbbb.setNameVersionId("bbbb");
    bbbb.addDependentModelId("aaaa");

    modelList.add(aaaa);
    modelList.add(bbbb);

    ModelSorter sorter = new ModelSorter();
    sorter = new ModelSorter();

    List<Artifact> sortedList = sorter.sort(modelList);
    assertNotNull(sortedList);
    assertEquals(3, sortedList.size());

  }

}
