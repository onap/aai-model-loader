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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.openecomp.modelloader.entity.model;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.openecomp.modelloader.entity.Artifact;

public class ModelV8ArtifactParserTest {

  @Test
  public void testParseModelFile() throws Exception {
    final String MODEL_FILE = "src/test/resources/models/v8-wan-connector-model.xml";

    try {
      byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

      ModelV8ArtifactParser parser = new ModelV8ArtifactParser();
      List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

      assertTrue(modelList.size() == 1);

      ModelV8Artifact model = (ModelV8Artifact) modelList.get(0);
      System.out.println(model.toString());

      assertTrue(model.getModelNameVersionId().equalsIgnoreCase("93d9d45d-7eec-4371-9083-675e4c353de3"));
      assertTrue(model.getModelNamespace().equalsIgnoreCase("http://com.att.aai.inventory/v7"));
      assertTrue(model.getModelNamespaceVersion().equalsIgnoreCase("v7"));			
      assertTrue(model.getType().toString().equalsIgnoreCase("MODEL_V8"));
      assertTrue(model.getDependentModelIds().size() == 7);
      assertTrue(model.getDependentModelIds().contains("d09dd9da-0148-46cd-a947-591afc844d24"));
      assertTrue(model.getDependentModelIds().contains("997fc7-fca1-451f-b953-9a1e6197b4d6"));
      assertTrue(model.getDependentModelIds().contains("ae16244f-4d29-4801-a559-e25f2db2a4c3"));
      assertTrue(model.getDependentModelIds().contains("759dbd4a-2473-46f3-a932-48d987c9b4a1"));
      assertTrue(model.getDependentModelIds().contains("a6d9de88-4046-4b78-a59e-5691243d292a"));
      assertTrue(model.getDependentModelIds().contains("35be1acf-1298-48c6-a128-66850083b8bd"));
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @Test
  public void testParseModelFileInvalidArtifact() throws Exception {
    final String MODEL_FILE = "src/test/resources/models/invalid-model.xml";

    try {
      byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));		

      ModelV8ArtifactParser parser = new ModelV8ArtifactParser();
      List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

      assertTrue(modelList == null || modelList.isEmpty());
    }
    catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @Test
  public void testParseModelFileIncompleteArtifact() throws Exception {
    final String MODEL_FILE = "src/test/resources/models/incomplete-model.xml";

    try {
      byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));		

      ModelV8ArtifactParser parser = new ModelV8ArtifactParser();
      List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

      assertTrue(modelList == null || modelList.isEmpty());
    }
    catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

}
