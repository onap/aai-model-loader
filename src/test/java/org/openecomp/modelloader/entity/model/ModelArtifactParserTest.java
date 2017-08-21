/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
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
			assertTrue(model.getModelNamespace().equalsIgnoreCase("http://org.openecomp.aai.inventory/v9"));
			assertTrue(model.getModelNamespaceVersion().equalsIgnoreCase("v9"));
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
}
