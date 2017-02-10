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

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.openecomp.modelloader.entity.Artifact;

public class ModelArtifactParserTest {

  @Test
  public void testParseModelFileNoDeps() throws Exception {
    final String MODEL_FILE = "src/test/resources/models/vnf-model.xml";

    try {
      byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

      ModelArtifactParser parser = new ModelArtifactParser();
      List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

      assertTrue(modelList.size() == 1);

      ModelArtifact model = (ModelArtifact) modelList.get(0);
      System.out.println(model.toString());

      assertTrue(model.getNameVersionId().equalsIgnoreCase("model-vid"));
      assertTrue(model.getType().toString().equalsIgnoreCase("MODEL"));
      assertTrue(model.getDependentModelIds().size() == 0);
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @Test
  public void testParseModelFileDeps() throws Exception {
    final String MODEL_FILE = "src/test/resources/models/wan-connector-model.xml";

    try {
      byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

      ModelArtifactParser parser = new ModelArtifactParser();
      List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

      assertTrue(modelList.size() == 1);

      ModelArtifact model = (ModelArtifact) modelList.get(0);
      System.out.println(model.toString());

      assertTrue(model.getNameVersionId().equalsIgnoreCase("93d9d45d-7eec-4371-9083-675e4c353de3"));
      assertTrue(model.getType().toString().equalsIgnoreCase("MODEL"));
      assertTrue(model.getDependentModelIds().size() == 7);
      assertTrue(model.getDependentModelIds().contains("d09dd9da-0148-46cd-a947-591afc844d24"));
      assertTrue(model.getDependentModelIds().contains("ae16244f-4d29-4801-a559-e25f2db2a4c3"));
      assertTrue(model.getDependentModelIds().contains("a6d9de88-4046-4b78-a59e-5691243d292a"));
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @Test
  public void testParseCompleteModel() throws Exception {
    final String MODEL_FILE = "src/test/resources/models/complete-model.xml";

    try {
      byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

      ModelArtifactParser parser = new ModelArtifactParser();
      List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

      for (Artifact art : modelList) {
        ModelArtifact model = (ModelArtifact) art;
        System.out.println(model.toString());
      }

      assertTrue(modelList.size() == 3);

      ModelArtifact modelVdc = (ModelArtifact) modelList.get(0);
      assertTrue(
          modelVdc.getNameVersionId().equalsIgnoreCase("997fc7-fca1-451f-b953-9a1e6197b4d6"));
      assertTrue(modelVdc.getType().toString().equalsIgnoreCase("MODEL"));
      assertTrue(modelVdc.getDependentModelIds().size() == 1);
      assertTrue(modelVdc.getDependentModelIds().contains("93d9d45d-7eec-4371-9083-675e4c353de3"));

      ModelArtifact modelPserver = (ModelArtifact) modelList.get(2);
      assertTrue(
          modelPserver.getNameVersionId().equalsIgnoreCase("f2b24d95-c582-48d5-b2d6-c5b3a94ce812"));
      assertTrue(modelPserver.getType().toString().equalsIgnoreCase("MODEL"));
      assertTrue(modelPserver.getDependentModelIds().size() == 2);
      assertTrue(
          modelPserver.getDependentModelIds().contains("35be1acf-1298-48c6-a128-66850083b8bd"));
      assertTrue(
          modelPserver.getDependentModelIds().contains("759dbd4a-2473-46f3-a932-48d987c9b4a1"));
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  @Test
  public void testParseNamedQuery() throws Exception {
    final String MODEL_FILE = "src/test/resources/models/named-query-wan-connector.xml";

    try {
      byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

      ModelArtifactParser parser = new ModelArtifactParser();
      List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

      assertTrue(modelList.size() == 1);

      ModelArtifact model = (ModelArtifact) modelList.get(0);
      System.out.println(model.toString());

      assertTrue(model.getNameVersionId().equalsIgnoreCase("94cac189-8d88-4d63-a194-f44214e080ff"));
      assertTrue(model.getType().toString().equalsIgnoreCase("NAMED_QUERY"));
      assertTrue(model.getDependentModelIds().size() == 4);
      assertTrue(model.getDependentModelIds().contains("d09dd9da-0148-46cd-a947-591afc844d24"));
      assertTrue(model.getDependentModelIds().contains("997fc7-fca1-451f-b953-9a1e6197b4d6"));
      assertTrue(model.getDependentModelIds().contains("897df7ea-8938-42b0-bc57-46e913a4d93b"));
      assertTrue(model.getDependentModelIds().contains("f2b24d95-c582-48d5-b2d6-c5b3a94ce812"));
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
}
