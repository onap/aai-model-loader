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

package org.openecomp.modelloader.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JsonXmlConverterTest {

  @Test
  public void testConversion() throws Exception {
    final String XML_MODEL_FILE = "src/test/resources/models/vnf-model.xml";
    final String JSON_MODEL_FILE = "src/test/resources/models/vnf-model.json";

    try {
      byte[] encoded = Files.readAllBytes(Paths.get(XML_MODEL_FILE));
      String originalXML = new String(encoded);

      assertFalse(JsonXmlConverter.isValidJson(originalXML));

      encoded = Files.readAllBytes(Paths.get(JSON_MODEL_FILE));
      String originalJSON = new String(encoded);

      assertTrue(JsonXmlConverter.isValidJson(originalJSON));

      String xmlFromJson = JsonXmlConverter.convertJsonToXml(originalJSON);

      // Spot check one of the attributes
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(new ByteArrayInputStream(xmlFromJson.getBytes()));
      NodeList nodeList = doc.getDocumentElement().getChildNodes();

      String modelVid = "notFound";
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node currentNode = nodeList.item(i);
        if (currentNode.getNodeName().equals("model-name-version-id")) {
          modelVid = currentNode.getTextContent();
          break;
        }
      }

      assertTrue(modelVid.equals("model-vid"));
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
}
