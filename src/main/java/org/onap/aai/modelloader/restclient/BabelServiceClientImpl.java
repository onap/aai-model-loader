/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2019 European Software Marketing Ltd.
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

import java.util.Collections;
import java.util.List;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTPS Client for interfacing with Babel.
 *
 */
@Component
public class BabelServiceClientImpl implements BabelServiceClient {

    private static final Logger logger = LoggerFactory.getInstance().getLogger(BabelServiceClientImpl.class);
    private final ModelLoaderConfig config;
    private final RestTemplate restTemplate;

    public BabelServiceClientImpl(ModelLoaderConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<BabelArtifact> postArtifact(BabelRequest babelRequest, String transactionId) throws BabelServiceClientException {
        if (logger.isDebugEnabled()) {
            logger.debug(ModelLoaderMsgs.BABEL_REST_REQUEST_PAYLOAD, " Artifact Name: " + babelRequest.getArtifactName()
                    + " Artifact version: " + babelRequest.getArtifactVersion() + " Artifact payload: " + babelRequest.getCsar());
        }

        String resourceUrl = config.getBabelProperties().getBaseUrl() + config.getBabelProperties().getGenerateResourceUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(AaiRestClient.HEADER_TRANS_ID, transactionId);
        headers.set(AaiRestClient.HEADER_FROM_APP_ID, AaiRestClient.ML_APP_NAME);
        HttpEntity<BabelRequest> entity = new HttpEntity<>(babelRequest, headers);

        ResponseEntity<List<BabelArtifact>> artifactResponse = restTemplate.exchange(resourceUrl, HttpMethod.POST, entity, new ParameterizedTypeReference<List<BabelArtifact>>() {});

        if (logger.isDebugEnabled()) {
            logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                    "Babel response " + artifactResponse.getStatusCode() + " " + artifactResponse.getBody().toString());
        }

        if (!artifactResponse.getStatusCode().equals(HttpStatus.OK)) {
            throw new BabelServiceClientException(artifactResponse.getBody().toString());
        }

        return artifactResponse.getBody();
    }
}
