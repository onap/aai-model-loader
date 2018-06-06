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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.ModelArtifactParser;
import org.onap.aai.restclient.client.OperationResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestAaiRestClient {

    private static final String MODEL_FILE = "src/test/resources/models/l3-network-widget.xml";

    // This test requires a running A&AI system. To test locally, annotate with org.junit.Test
    public void testRestClient() throws Exception {
        Properties props = new Properties();
        props.setProperty("ml.distribution.ARTIFACT_TYPES", "MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG");
        props.setProperty("ml.aai.BASE_URL", "https://localhost:8443");
        props.setProperty("ml.aai.MODEL_URL", "/aai/v9/service-design-and-creation/models/model/");
        props.setProperty("ml.aai.KEYSTORE_FILE", "aai-client-cert.p12");
        props.setProperty("ml.aai.KEYSTORE_PASSWORD", "OBF:1i9a1u2a1unz1lr61wn51wn11lss1unz1u301i6o");

        ModelLoaderConfig config = new ModelLoaderConfig(props, ".");

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

            AaiRestClient aaiClient = new AaiRestClient(config);

            // GET model
            OperationResult opResult =
                    aaiClient.getResource(getUrl(model, config), "example-trans-id-0", MediaType.APPLICATION_XML_TYPE);
            assertTrue(opResult.getResultCode() == Response.Status.NOT_FOUND.getStatusCode());

            // PUT the model
            opResult = aaiClient.putResource(getUrl(model, config), model.getPayload(), "example-trans-id-1",
                    MediaType.APPLICATION_XML_TYPE);
            assertTrue(opResult.getResultCode() == Response.Status.CREATED.getStatusCode());

            // DELETE the model
            opResult = aaiClient.getAndDeleteResource(getUrl(model, config), "example-trans-id-3");
            assertTrue(opResult.getResultCode() == Response.Status.NO_CONTENT.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Stream<Node> getNodesStream(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
    }

    static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

    private String getUrl(ModelArtifact model, ModelLoaderConfig config) {
        String subUrl;
        if (model.getType().equals(ArtifactType.MODEL)) {
            subUrl = config.getAaiModelUrl(model.getModelNamespaceVersion()).trim();
        } else {
            subUrl = config.getAaiNamedQueryUrl(model.getModelNamespaceVersion()).trim();
        }

        String baseUrl = config.getAaiBaseUrl().trim();
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
