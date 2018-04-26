/**
 * ﻿============LICENSE_START=======================================================
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.json.JsonSanitizer;
import com.sun.jersey.api.client.Client; // NOSONAR
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import org.json.JSONObject;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;

/**
 * Initial version for testing End to End scenarios
 *
 */
public class BabelServiceClient {

    private static Logger logger = LoggerFactory.getInstance().getLogger(BabelServiceClient.class);

    private static final String SSL_PROTOCOL = "TLS";
    private static final String KEYSTORE_ALGORITHM = "SunX509";
    private static final String KEYSTORE_TYPE = "PKCS12";

    private ModelLoaderConfig config;
    private Client client;

    private TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null; // NOSONAR
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
            // Do nothing
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
            // Do nothing
        }
    }};

    public class BabelServiceException extends Exception {

        /**
         * Babel Service error response
         */
        private static final long serialVersionUID = 1L;

        public BabelServiceException(String message) {
            super(message);
        }

    }

    /**
     * @param config
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     */
    public BabelServiceClient(ModelLoaderConfig config) throws NoSuchAlgorithmException, KeyStoreException,
            CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
        this.config = config;

        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Creating Babel Service client");

        SSLContext ctx = SSLContext.getInstance(SSL_PROTOCOL);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEYSTORE_ALGORITHM);
        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);

        String clientCertPassword = config.getBabelKeyStorePassword();

        char[] pwd = null;
        if (clientCertPassword != null) {
            pwd = clientCertPassword.toCharArray();
        }

        String clientCertFileName = config.getBabelKeyStorePath();
        if (clientCertFileName != null) {
            FileInputStream fin = new FileInputStream(clientCertFileName);
            ks.load(fin, pwd);
            kmf.init(ks, pwd);
            ctx.init(kmf.getKeyManagers(), trustAllCerts, null);
        } else {
            ctx.init(null, trustAllCerts, null);
        }

        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Initialised context");

        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);

        client = Client.create(new DefaultClientConfig());

        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Jersey client created");
    }

    /**
     * @param artifactPayload
     * @param artifactName
     * @param artifactVersion
     * @param transactionId
     * @return
     * @throws BabelServiceException
     */
    public List<BabelArtifact> postArtifact(byte[] artifactPayload, String artifactName, String artifactVersion,
            String transactionId) throws BabelServiceException {
        Objects.requireNonNull(artifactPayload);

        String encodedPayload = Base64.getEncoder().encodeToString(artifactPayload);

        JSONObject obj = new JSONObject();
        obj.put("csar", encodedPayload);
        obj.put("artifactVersion", artifactVersion);
        obj.put("artifactName", artifactName);

        logger.info(ModelLoaderMsgs.BABEL_REST_REQUEST_PAYLOAD, " Artifact Name: " + artifactName
                + " Artifact version: " + artifactVersion + " Artifact payload: " + encodedPayload);

        WebResource webResource = client.resource(config.getBabelBaseUrl() + config.getBabelGenerateArtifactsUrl());
        ClientResponse response = webResource.type("application/json")
                .header(AaiRestClient.HEADER_TRANS_ID, Collections.singletonList(transactionId))
                .header(AaiRestClient.HEADER_FROM_APP_ID, Collections.singletonList(AaiRestClient.ML_APP_NAME))
                .post(ClientResponse.class, obj.toString());
        String sanitizedJson = JsonSanitizer.sanitize(response.getEntity(String.class));

        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                "Babel response " + response.getStatus() + " " + sanitizedJson);

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new BabelServiceException(sanitizedJson);
        }

        return new Gson().fromJson(sanitizedJson, new TypeToken<List<BabelArtifact>>() {}.getType());
    }
}
