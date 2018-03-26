/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.aai.modelloader.entity.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.ModelSorter;
import org.onap.aai.modelloader.entity.model.ModelV8Artifact;
import org.onap.aai.modelloader.entity.model.NamedQueryArtifact;

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
    model.setModelInvariantId("aaa");
    model.setModelVerId("111");
    model.addDependentModelId("xyz|123");
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
    aaaa.setModelInvariantId("aaaa");
    aaaa.setModelVerId("mvaaaa");
    aaaa.addDependentModelId("cccc|mvcccc");

    ModelArtifact bbbb = new ModelArtifact();
    bbbb.setModelInvariantId("bbbb");
    bbbb.setModelVerId("mvbbbb");
    bbbb.addDependentModelId("aaaa|mvaaaa");

    ModelArtifact cccc = new ModelArtifact();
    cccc.setModelInvariantId("cccc");
    cccc.setModelVerId("mvcccc");

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
  
  @Test
  public void multipleModelsV8() {

    List<Artifact> modelList = new ArrayList<Artifact>();

    ModelV8Artifact aaaa = new ModelV8Artifact();
    aaaa.setModelNameVersionId("aaaa");
    aaaa.addDependentModelId("cccc");

    ModelV8Artifact bbbb = new ModelV8Artifact();
    bbbb.setModelNameVersionId("bbbb");
    bbbb.addDependentModelId("aaaa");

    ModelV8Artifact cccc = new ModelV8Artifact();
    cccc.setModelNameVersionId("cccc");

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
    
  @Test
  public void multipleModelsAndNamedQueries() {

    List<Artifact> modelList = new ArrayList<Artifact>();

    ModelArtifact aaaa = new ModelArtifact();
    aaaa.setModelInvariantId("aaaa");
    aaaa.setModelVerId("1111");
    aaaa.addDependentModelId("cccc|2222");
    
    NamedQueryArtifact nq1 = new NamedQueryArtifact();
    nq1.setNamedQueryUuid("nq1");
    nq1.addDependentModelId("aaaa|1111");
    
    NamedQueryArtifact nq2 = new NamedQueryArtifact();
    nq2.setNamedQueryUuid("nq2");
    nq2.addDependentModelId("existing-model");
    

    modelList.add(nq1);
    modelList.add(nq2);
    modelList.add(aaaa);

    ModelSorter sorter = new ModelSorter();
    sorter = new ModelSorter();

    List<Artifact> sortedList = sorter.sort(modelList);
    assertNotNull(sortedList);
    assertEquals(3, sortedList.size());

    System.out.println(sortedList.get(0) + "-" + sortedList.get(1) + "-" + sortedList.get(2));
    assertEquals(aaaa, sortedList.get(0));
    assertEquals(nq2, sortedList.get(1));
    assertEquals(nq1, sortedList.get(2));
  }
  
  @Test(expected = RuntimeException.class)
  public void circularDependency() {

    List<Artifact> modelList = new ArrayList<Artifact>();

    ModelArtifact aaaa = new ModelArtifact();
    aaaa.setModelInvariantId("aaaa");
    aaaa.setModelVerId("1111");
    aaaa.addDependentModelId("bbbb|1111");

    ModelArtifact bbbb = new ModelArtifact();
    bbbb.setModelInvariantId("bbbb");
    bbbb.setModelVerId("1111");
    bbbb.addDependentModelId("aaaa|1111");

    modelList.add(aaaa);
    modelList.add(bbbb);

    ModelSorter sorter = new ModelSorter();
    sorter = new ModelSorter();

    List<Artifact> sortedList = sorter.sort(modelList);
    assertNotNull(sortedList);
    assertEquals(2, sortedList.size());

  }

}
