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

public class NamedQueryArtifactParserTest {

	@Test
	public void testParseNamedQuery() throws Exception {
		final String MODEL_FILE = "src/test/resources/models/named-query-wan-connector.xml";

		try {
			byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE));

			NamedQueryArtifactParser parser = new NamedQueryArtifactParser();
			List<Artifact> modelList = parser.parse(xmlBytes, "test-artifact");

			assertTrue(modelList.size() == 1);

			NamedQueryArtifact model = (NamedQueryArtifact) modelList.get(0);
			System.out.println(model.toString());

			assertTrue(model.getNamedQueryUuid().equalsIgnoreCase("94cac189-8d88-4d63-a194-f44214e080ff"));
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
