/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
 * Copyright © 2023 Deutsche Telekom AG.
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

import static org.junit.Assert.assertEquals;

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

import java.time.Duration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.ModelArtifactParser;
import org.onap.aai.restclient.client.OperationResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestAaiRestClient {

    private static final String MODEL_FILE = "src/test/resources/models/l3-network-widget.xml";
    private ModelLoaderConfig config;
    private AaiRestClient aaiClient;
    private int containerPort;

    private final String image = "nexus3.onap.org:10001/onap/aai-resources:1.10.0";

    @Rule
    public GenericContainer<?> resourcesContainer = new GenericContainer<>(DockerImageName.parse(image))
            .withExposedPorts(8447)
            .withStartupTimeout(Duration.ofMinutes(3));

    @Before
    public void setup() {
        containerPort = resourcesContainer.getFirstMappedPort();
        Properties props = new Properties();
        props.setProperty("ml.distribution.ARTIFACT_TYPES", "MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG");
        props.setProperty("ml.aai.BASE_URL", "https://localhost:" + String.valueOf(containerPort));
        props.setProperty("ml.aai.MODEL_URL", "aai/v*/service-design-and-creation/models/model/");
        props.setProperty("ml.aai.AUTH_USER", "ModelLoader");
        props.setProperty("ml.aai.AUTH_PASSWORD", "OBF:1qvu1v2h1sov1sar1wfw1j7j1wg21saj1sov1v1x1qxw");

        config = new ModelLoaderConfig(props, ".");
        aaiClient = new AaiRestClient(config);
    }

    @Test
    public void testRestClient() throws Exception {

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

        // Build the model artifact
        ModelArtifact model = new ModelArtifact();
        model.setModelInvariantId(modelInvariantId);
        model.setModelVerId(modelId);
        model.setPayload(readFile(MODEL_FILE));
        model.setModelNamespace("http://org.onap.aai.inventory/v25");


        // GET model
        OperationResult opResult = aaiClient.getResource(getUrl(model, config), "example-trans-id-0",
                MediaType.APPLICATION_XML_TYPE);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), opResult.getResultCode());

        // PUT the model
        opResult = aaiClient.putResource(getUrl(model, config), model.getPayload(), "example-trans-id-1",
                MediaType.APPLICATION_XML_TYPE);
        assertEquals(Response.Status.CREATED.getStatusCode(), opResult.getResultCode());

        // DELETE the model
        opResult = aaiClient.getAndDeleteResource(getUrl(model, config), "example-trans-id-3");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), opResult.getResultCode());
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
