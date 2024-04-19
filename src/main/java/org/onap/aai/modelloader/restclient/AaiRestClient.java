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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.AaiResourcesObject;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.aai.restclient.client.RestClient;
import org.onap.aai.restclient.enums.RestAuthenticationMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriBuilder;
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

    private static Logger logger = LoggerFactory.getInstance().getLogger(AaiRestClient.class.getName());
    public static final String HEADER_TRANS_ID = "X-TransactionId";
    public static final String HEADER_FROM_APP_ID = "X-FromAppId";
    public static final String ML_APP_NAME = "ModelLoader";
    private static final String RESOURCE_VERSION_PARAM = "resource-version";
    private final ModelLoaderConfig config;
    private final RestTemplate restTemplate;

    public AaiRestClient(ModelLoaderConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    /**
     * Send a GET request to the A&AI for a resource.
     * @param <T>
     *
     * @param url
     * @param transId
     * @param mediaType
     * @return
     */
    public <T> ResponseEntity<T> getResource(String url, String transId, MediaType mediaType, Class<T> responseType) {
        HttpHeaders headers = defaultHeaders(transId);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
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
    public <T> ResponseEntity<T> putResource(String url, T payload, String transId, MediaType mediaType, Class<T> responseType) {
        logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_PAYLOAD, payload.toString());
        HttpHeaders headers = defaultHeaders(transId);
        headers.setAccept(Collections.singletonList(mediaType));
        headers.setContentType(mediaType);
        HttpEntity<T> entity = new HttpEntity<>(payload, headers);

        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
    }

    public <T> ResponseEntity<T> postResource(String url, T payload, String transId, MediaType mediaType, Class<T> responseType) {
        logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_PAYLOAD, payload.toString());
        HttpHeaders headers = defaultHeaders(transId);
        headers.setAccept(Collections.singletonList(mediaType));
        headers.setContentType(mediaType);
        HttpEntity<T> entity = new HttpEntity<>(payload, headers);

        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    /**
     * Send a DELETE request to the A&AI.
     *
     * @param url - the url
     * @param resourceVersion - the resource-version of the model to delete
     * @param transId - transaction ID
     * @return ClientResponse
     */
    public ResponseEntity<String> deleteResource(String url, String resourceVersion, String transId) {
        HttpHeaders headers = defaultHeaders(transId);
        String uri = url + "?" + RESOURCE_VERSION_PARAM + "=" + resourceVersion;
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
    }

    /**
     * Does a GET on a resource to retrieve the resource version, and then DELETE that version.
     *
     * @param url - the url
     * @param transId - transaction ID
     * @return ClientResponse
     */
    public ResponseEntity<?> getAndDeleteResource(String url, String transId) {
        ResponseEntity<AaiResourcesObject> response = getResource(url, transId, MediaType.APPLICATION_XML, AaiResourcesObject.class);
        // First, GET the model
        if (response == null || response.getStatusCode() != HttpStatus.OK) {
            return response;
        }

        return deleteResource(url, response.getBody().getResourceVersion(), transId);
    }


    private boolean useBasicAuth() {
        return config.getAaiAuthenticationUser() != null && config.getAaiAuthenticationPassword() != null;
    }

    /**
     * Create the HTTP headers required for an A&AI operation (GET/POST/PUT/DELETE)
     * 
     * @param transId
     * @return map of headers
     */
    private HttpHeaders defaultHeaders(String transId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(AaiRestClient.HEADER_TRANS_ID, transId);
        headers.set(AaiRestClient.HEADER_FROM_APP_ID, AaiRestClient.ML_APP_NAME);
        if (useBasicAuth()) {
            headers.setBasicAuth(config.getAaiAuthenticationUser(), config.getAaiAuthenticationPassword());
        }
        return headers;
    }
}
