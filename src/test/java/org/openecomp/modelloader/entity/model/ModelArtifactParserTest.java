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
 * ECOMP and OpenECOMP are trademarks
 * and service marks of AT&T Intellectual Property.
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
		final String MODEL_FILE = "src/test/resources/models/l3-network-widget.xml";

		try {
			byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

			ModelArtifactParser parser = new ModelArtifactParser();
			List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

			assertTrue(modelList.size() == 1);

			ModelArtifact model = (ModelArtifact) modelList.get(0);
			System.out.println(model.toString());

			assertTrue(model.getModelInvariantId().equalsIgnoreCase("3d560d81-57d0-438b-a2a1-5334dba0651a"));
			assertTrue(model.getType().toString().equalsIgnoreCase("MODEL"));
			System.out.println(model.getDependentModelIds().size());
			assertTrue(model.getDependentModelIds().size() == 0);
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void testParseModelFileDeps() throws Exception {
		final String MODEL_FILE = "src/test/resources/models/AAI-stellService-service-1.xml";

		try {
			byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

			ModelArtifactParser parser = new ModelArtifactParser();
			List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

			assertTrue(modelList.size() == 1);

			ModelArtifact model = (ModelArtifact) modelList.get(0);
			System.out.println(model.toString());

			assertTrue(model.getModelInvariantId().equalsIgnoreCase("fedf9da3-6a74-4813-8fa2-221a98b0e7ad"));
			assertTrue(model.getModelVerId().equalsIgnoreCase("e0373537-7f66-4094-9939-e2f5de6ff5f6"));
			assertTrue(model.getType().toString().equalsIgnoreCase("MODEL"));
			assertTrue(model.getDependentModelIds().size() == 3);
			assertTrue(model.getDependentModelIds().contains("5c12984d-db0f-4300-a0e0-9791775cc40f|88bdbadf-db8a-490f-881e-c8effcbc3f66"));
			assertTrue(model.getDependentModelIds().contains("959b7c09-9f34-4e5f-8b63-505381db176e|374d0899-bbc2-4403-9320-fe9bebef75c6"));
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

			assertTrue(modelList.size() == 5);

			ModelArtifact model1 = (ModelArtifact)modelList.get(0);
			assertTrue(model1.getModelVerId().equalsIgnoreCase("88bdbadf-db8a-490f-881e-c8effcbc3f66"));
			assertTrue(model1.getType().toString().equalsIgnoreCase("MODEL"));
			assertTrue(model1.getDependentModelIds().size() == 1);
			assertTrue(model1.getDependentModelIds().contains("3d560d81-57d0-438b-a2a1-5334dba0651a|9111f20f-e680-4001-b83f-19a2fc23bfc1"));

			ModelArtifact model4 = (ModelArtifact)modelList.get(4);
			assertTrue(model4.getModelInvariantId().equalsIgnoreCase("fedf9da3-6a74-4813-8fa2-221a98b0e7ad"));
			assertTrue(model4.getType().toString().equalsIgnoreCase("MODEL"));
			assertTrue(model4.getDependentModelIds().size() == 3);
			assertTrue(model4.getDependentModelIds().contains("5c12984d-db0f-4300-a0e0-9791775cc40f|88bdbadf-db8a-490f-881e-c8effcbc3f66"));
			assertTrue(model4.getDependentModelIds().contains("82194af1-3c2c-485a-8f44-420e22a9eaa4|46b92144-923a-4d20-b85a-3cbd847668a9"));
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

	@Test
	public void testParseModelFileInvalidArtifact() throws Exception {
		final String MODEL_FILE = "src/test/resources/models/invalid-model.xml";

		try {
			byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));		

			ModelArtifactParser parser = new ModelArtifactParser();
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

			ModelArtifactParser parser = new ModelArtifactParser();
			List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

			assertTrue(modelList == null || modelList.isEmpty());
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void testParseModelFileIncompleteArtifacts() throws Exception {
		final String MODEL_FILE = "src/test/resources/models/incomplete-models.xml";

		try {
			byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));		

			ModelArtifactParser parser = new ModelArtifactParser();
			List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

			assertTrue(modelList == null || modelList.isEmpty());
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

}
