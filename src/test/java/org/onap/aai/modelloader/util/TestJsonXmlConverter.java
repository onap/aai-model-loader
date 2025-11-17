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
package org.onap.aai.modelloader.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestJsonXmlConverter {

    private static final String XML_MODEL_FILE = "src/test/resources/models/l3-network-widget.xml";
    private static final String JSON_MODEL_FILE = "src/test/resources/models/l3-network-widget.json";

    @Test
    public void testJsonIsValid() {
        assertThat(JsonXmlConverter.isValidJson("{}"), is(true));
        assertThat(JsonXmlConverter.isValidJson("[]"), is(true));

        assertThat(JsonXmlConverter.isValidJson("{"), is(false));
        assertThat(JsonXmlConverter.isValidJson("["), is(false));
    }

    @Test
    public void testConversion() throws Exception {
        byte[] encoded = Files.readAllBytes(Path.of(XML_MODEL_FILE));
        assertThat(JsonXmlConverter.isValidJson(new String(encoded)), is(false));
        encoded = Files.readAllBytes(Path.of(JSON_MODEL_FILE));
        String originalJson = new String(encoded);

        assertThat(JsonXmlConverter.isValidJson(originalJson), is(true));

        String xmlFromJson = JsonXmlConverter.convertJsonToXml(originalJson);

        // Spot check one of the attributes
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlFromJson.getBytes()));
        NodeList nodeList = doc.getDocumentElement().getChildNodes();

        String modelVid = "notFound";
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeName().equals("model-invariant-id")) {
                modelVid = currentNode.getTextContent();
                break;
            }
        }

        assertThat(modelVid.equals("3d560d81-57d0-438b-a2a1-5334dba0651a"), is(true));

        // Convert the XML form back into JSON
        JsonXmlConverter.convertXmlToJson(xmlFromJson);
    }
}
