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
package org.onap.aai.modelloader.restclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.onap.aai.modelloader.config.AaiProperties;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.ModelArtifactParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestAaiRestClient {

    private static final String MODEL_FILE = "src/test/resources/models/l3-network-widget.xml";

    // This test requires a running A&AI system. To test locally, annotate with org.junit.Test
    public void testRestClient() throws Exception {
        AaiProperties aaiProperties = new AaiProperties();
        aaiProperties.setBaseUrl("http://aai.onap:80");
        aaiProperties.setModelUrl("/aai/%s/service-design-and-creation/models/model/");
        aaiProperties.setNamedQueryUrl("/aai/%s/service-design-and-creation/named-queries/named-query/");
        aaiProperties.setVnfImageUrl("/aai/%s/service-design-and-creation/vnf-images");

        File xmlFile = new File(MODEL_FILE);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbFactory.newDocumentBuilder().parse(xmlFile);

        NodeList nodesList = doc.getDocumentElement().getChildNodes();

        // Get the model IDs

        // @formatter:off
        String modelInvariantId =
                getNodesStream(nodesList)
                        .filter(childNode -> childNode.getNodeName().equals(ModelArtifactParser.MODEL_INVARIANT_ID))
                        .findFirst()
                        .map(Node::getTextContent)
                        .orElse(null);

        String modelId = getNodesStream(nodesList)
                .flatMap(n -> getNodesStream(n.getChildNodes()))
                .filter(childNode -> childNode.getNodeName().equals(ModelArtifactParser.MODEL_VER))
                .findFirst()
                .map(n -> n.getChildNodes().item(1).getTextContent())
                .orElse(null);
        // @formatter:on

        try {
            // Build the model artifact
            ModelArtifact model = new ModelArtifact();
            model.setModelInvariantId(modelInvariantId);
            model.setModelVerId(modelId);
            model.setPayload(readFile(MODEL_FILE));
            model.setModelNamespace("http://org.openecomp.aai.inventory/v9");

            AaiRestClient aaiClient = new AaiRestClient(aaiProperties, new RestTemplate());

            // GET model
            ResponseEntity opResult =
                    aaiClient.getResource(getUrl(model, aaiProperties), "example-trans-id-0", MediaType.APPLICATION_XML, String.class);
            assertEquals(opResult.getStatusCode(), HttpStatus.NOT_FOUND);

            // PUT the model
            opResult = aaiClient.putResource(getUrl(model, aaiProperties), model.getPayload(), "example-trans-id-1",
                    MediaType.APPLICATION_XML, String.class);
            assertEquals(opResult.getStatusCode(), HttpStatus.CREATED);

            // DELETE the model
            opResult = aaiClient.getAndDeleteResource(getUrl(model, aaiProperties), "example-trans-id-3");
            assertEquals(opResult.getStatusCode(), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Stream<Node> getNodesStream(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
    }

    static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Path.of(path));
        return new String(encoded);
    }

    private String getUrl(ModelArtifact model, AaiProperties aaiProperties) {
        String subUrl;
        if (model.getType().equals(ArtifactType.MODEL)) {
            subUrl = aaiProperties.getModelUrl().formatted(model.getModelNamespaceVersion()).trim();
        } else {
            subUrl = aaiProperties.getNamedQueryUrl().formatted(model.getModelNamespaceVersion()).trim();
        }

        String baseUrl = aaiProperties.getBaseUrl().trim();
        if (!baseUrl.endsWith("/") && !subUrl.startsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        if (baseUrl.endsWith("/") && subUrl.startsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (!subUrl.endsWith("/")) {
            subUrl = subUrl + "/";
        }

        return baseUrl + subUrl + model.getModelInvariantId();
    }
}
