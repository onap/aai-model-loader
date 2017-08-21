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

import org.junit.Test;

public class ModelParserFactoryTest {

	@Test
	public void testParserFactory() throws Exception {
		final String MODEL_FILE_V8 = "src/test/resources/models/v8-wan-connector-model.xml";
		final String MODEL_FILE_V9 = "src/test/resources/models/AAI-VL-resource-1.xml";
		final String MODEL_FILE_NAMED_QUERY = "src/test/resources/models/named-query-wan-connector.xml";

		
		try {
			byte[] xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE_V8));
			IModelParser parser = ModelParserFactory.createModelParser(xmlBytes, "v8-wan-connector-model.xml");
			assertTrue(parser instanceof ModelV8ArtifactParser);
			
			xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE_V9));
      parser = ModelParserFactory.createModelParser(xmlBytes, "AAI-VL-resource-1.xml");
      assertTrue(parser instanceof ModelArtifactParser);
      
      xmlBytes = Files.readAllBytes(Paths.get(MODEL_FILE_NAMED_QUERY));
      parser = ModelParserFactory.createModelParser(xmlBytes, "named-query-wan-connector.xml");
      assertTrue(parser instanceof NamedQueryArtifactParser);
		} catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
	}
}
