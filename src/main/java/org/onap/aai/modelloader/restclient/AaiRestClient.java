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

import com.sun.jersey.core.util.MultivaluedMapImpl; // NOSONAR
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.aai.restclient.client.RestClient;
import org.onap.aai.restclient.enums.RestAuthenticationMode;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Wrapper around the standard A&AI Rest Client interface. This currently uses Jersey client 1.x
 *
 */
@Component
public class AaiRestClient {

    public static final String HEADER_TRANS_ID = "X-TransactionId";
    public static final String HEADER_FROM_APP_ID = "X-FromAppId";
    public static final String ML_APP_NAME = "ModelLoader";
    private static final String RESOURCE_VERSION_PARAM = "resource-version";

    private static Logger logger = LoggerFactory.getInstance().getLogger(AaiRestClient.class.getName());

    private ModelLoaderConfig config = null;

    public AaiRestClient(ModelLoaderConfig config) {
        this.config = config;
    }


    /**
     * Send a GET request to the A&AI for a resource.
     *
     * @param url
     * @param transId
     * @param mediaType
     * @return
     */
    public OperationResult getResource(String url, String transId, MediaType mediaType) {
        return setupClient().get(url, buildHeaders(transId), mediaType);
    }

    /**
     * Send a PUT request to the A&AI.
     *
     * @param url - the url
     * @param payload - the XML or JSON payload for the request
     * @param transId - transaction ID
     * @param mediaType - the content type (XML or JSON)
     * @return operation result
     */
    public OperationResult putResource(String url, String payload, String transId, MediaType mediaType) {
        logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_PAYLOAD, payload);
        return setupClient().put(url, payload, buildHeaders(transId), mediaType, mediaType);
    }


    /**
     * Send a POST request to the A&AI.
     *
     * @param url - the url
     * @param transId - transaction ID
     * @param payload - the XML or JSON payload for the request
     * @param mimeType - the content type (XML or JSON)
     * @return ClientResponse
     */
    public OperationResult postResource(String url, String payload, String transId, MediaType mediaType) {
        logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_PAYLOAD, payload);
        return setupClient().post(url, payload, buildHeaders(transId), mediaType, mediaType);
    }


    /**
     * Send a DELETE request to the A&AI.
     *
     * @param url - the url
     * @param resourceVersion - the resource-version of the model to delete
     * @param transId - transaction ID
     * @return ClientResponse
     */
    public OperationResult deleteResource(String url, String resourceVersion, String transId) {
        URI uri = UriBuilder.fromUri(url).queryParam(RESOURCE_VERSION_PARAM, resourceVersion).build();
        return setupClient().delete(uri.toString(), buildHeaders(transId), null);
    }

    /**
     * Does a GET on a resource to retrieve the resource version, and then DELETE that version.
     *
     * @param url - the url
     * @param transId - transaction ID
     * @return ClientResponse
     */
    public OperationResult getAndDeleteResource(String url, String transId) {
        // First, GET the model
        OperationResult getResponse = getResource(url, transId, MediaType.APPLICATION_XML_TYPE);
        if (getResponse == null || getResponse.getResultCode() != Response.Status.OK.getStatusCode()) {
            return getResponse;
        }

        // Delete the model using the resource version in the response
        String resVersion = null;
        try {
            resVersion = getResourceVersion(getResponse);
        } catch (Exception e) {
            logger.error(ModelLoaderMsgs.AAI_REST_REQUEST_ERROR, "GET", url, e.getLocalizedMessage());
            return null;
        }

        return deleteResource(url, resVersion, transId);
    }


    public boolean useBasicAuth() {
        return config.getAaiAuthenticationUser() != null && config.getAaiAuthenticationPassword() != null;
    }

    private RestClient setupClient() {
        RestClient restClient = new RestClient();
        restClient.validateServerHostname(false)
                .validateServerCertChain(false)
                .connectTimeoutMs(config.getClientConnectTimeoutMs())
                .readTimeoutMs(config.getClientReadTimeoutMs());

        //Use certs only if SSL is enabled
        if (config.useHttpsWithAAI())
        {// @formatter:off
            restClient
                .clientCertFile(config.getAaiKeyStorePath())
                .clientCertPassword(config.getAaiKeyStorePassword());
            // @formatter:on
        }

        if (useBasicAuth()) {
            restClient.authenticationMode(RestAuthenticationMode.SSL_BASIC);
            restClient.basicAuthUsername(config.getAaiAuthenticationUser());
            restClient.basicAuthPassword(config.getAaiAuthenticationPassword());
        }

        return restClient;
    }

    /**
     * Create the HTTP headers required for an A&AI operation (GET/POST/PUT/DELETE)
     * 
     * @param transId
     * @return map of headers
     */
    private Map<String, List<String>> buildHeaders(String transId) {
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
        headers.put(HEADER_TRANS_ID, Collections.singletonList(transId));
        headers.put(HEADER_FROM_APP_ID, Collections.singletonList(ML_APP_NAME));
        return headers;
    }

    private String getResourceVersion(OperationResult getResponse)
            throws ParserConfigurationException, SAXException, IOException {
        String respData = getResponse.getResult();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(respData));
        Document doc = builder.parse(is);

        NodeList nodesList = doc.getDocumentElement().getChildNodes();

        // @formatter:off
        return IntStream.range(0, nodesList.getLength()).mapToObj(nodesList::item)
                .filter(childNode -> childNode.getNodeName().equals(RESOURCE_VERSION_PARAM))
                .findFirst()
                .map(Node::getTextContent)
                .orElse(null);
        // @formatter:on
    }
}
