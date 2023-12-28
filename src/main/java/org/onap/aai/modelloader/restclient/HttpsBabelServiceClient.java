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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.json.JsonSanitizer;
import com.sun.jersey.api.client.Client; // NOSONAR
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.cl.api.LogFields;
import org.onap.aai.cl.api.LogLine;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.cl.mdc.MdcOverride;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;

/**
 * HTTPS Client for interfacing with Babel.
 *
 */
public class HttpsBabelServiceClient implements BabelServiceClient {

    private static final Logger logger = LoggerFactory.getInstance().getLogger(HttpsBabelServiceClient.class);
    private static final Logger metricsLogger =
            LoggerFactory.getInstance().getMetricsLogger(HttpsBabelServiceClient.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final String SSL_PROTOCOL = "TLS";
    private static final String KEYSTORE_ALGORITHM = "SunX509";
    private static final String KEYSTORE_TYPE = "PKCS12";

    private final ModelLoaderConfig config;
    private final Client client;

    /**
     * @param config
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     * @throws BabelServiceClientException
     */
    public HttpsBabelServiceClient(ModelLoaderConfig config)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException, BabelServiceClientException {
        this.config = config;

        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Creating Babel Service client");
        //Initialize SSL Context only if SSL is enabled
        if (config.useHttpsWithBabel()) {
            SSLContext ctx = SSLContext.getInstance(SSL_PROTOCOL);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEYSTORE_ALGORITHM);
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

            String clientCertPassword = config.getBabelKeyStorePassword();

            char[] pwd = null;
            if (clientCertPassword != null) {
                pwd = clientCertPassword.toCharArray();
            }

            TrustManager[] trustManagers = getTrustManagers();

            String clientCertFileName = config.getBabelKeyStorePath();
            if (clientCertFileName == null) {
                ctx.init(null, trustManagers, null);
            } else {
                InputStream fin = Files.newInputStream(Paths.get(clientCertFileName));
                keyStore.load(fin, pwd);
                kmf.init(keyStore, pwd);
                ctx.init(kmf.getKeyManagers(), trustManagers, null);
            }

            logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Initialised context");

            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);
        }

        client = Client.create(new DefaultClientConfig());
        client.setConnectTimeout(config.getClientConnectTimeoutMs());
        client.setReadTimeout(config.getClientReadTimeoutMs());

        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Jersey client created");
    }

    private TrustManager[] getTrustManagers() throws NoSuchAlgorithmException, KeyStoreException, CertificateException,
            IOException, BabelServiceClientException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // Using null here initializes the TMF with the default trust store.
        tmf.init((KeyStore) null);

        // Create a new Trust Manager from the local trust store.
        String trustStoreFile = config.getBabelTrustStorePath();
        if (trustStoreFile == null) {
            throw new BabelServiceClientException("No Babel trust store defined");
        }
        try (InputStream myKeys = Files.newInputStream(Paths.get(trustStoreFile))) {
            KeyStore myTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            myTrustStore.load(myKeys, config.getBabelTrustStorePassword().toCharArray());
            tmf.init(myTrustStore);
        }
        X509TrustManager localTm = findX509TrustManager(tmf);

        // Create a custom trust manager that wraps both our trust store and the default.
        final X509TrustManager finalLocalTm = localTm;

        // Find the default trust manager.
        final X509TrustManager defaultTrustManager = findX509TrustManager(tmf);

        return new TrustManager[] {new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return defaultTrustManager.getAcceptedIssuers();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                try {
                    finalLocalTm.checkServerTrusted(chain, authType);
                } catch (CertificateException e) { // NOSONAR
                    defaultTrustManager.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                defaultTrustManager.checkClientTrusted(chain, authType);
            }
        }};
    }

    private X509TrustManager findX509TrustManager(TrustManagerFactory tmf) {
        X509TrustManager trustManager = null;
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                trustManager = (X509TrustManager) tm;
                break;
            }
        }
        return trustManager;
    }

    /**
     * @param artifactPayload
     * @param artifactName
     * @param artifactVersion
     * @param transactionId
     * @return
     * @throws BabelServiceClientException
     * @throws JSONException
     */
    @Override
    public List<BabelArtifact> postArtifact(byte[] artifactPayload, String artifactName, String artifactVersion,
            String transactionId) throws BabelServiceClientException {
        Objects.requireNonNull(artifactPayload);

        String encodedPayload = Base64.getEncoder().encodeToString(artifactPayload);

        JSONObject obj = new JSONObject();
        try {
            obj.put("csar", encodedPayload);
            obj.put("artifactVersion", artifactVersion);
            obj.put("artifactName", artifactName);
        } catch (JSONException ex) {
            throw new BabelServiceClientException(ex);
        }

        if (logger.isInfoEnabled()) {
            logger.info(ModelLoaderMsgs.BABEL_REST_REQUEST_PAYLOAD, " Artifact Name: " + artifactName
                    + " Artifact version: " + artifactVersion + " Artifact payload: " + encodedPayload);
        }

        MdcOverride override = new MdcOverride();
        override.addAttribute(MdcContext.MDC_START_TIME, ZonedDateTime.now().format(formatter));

        WebResource webResource = client.resource(config.getBabelBaseUrl() + config.getBabelGenerateArtifactsUrl());
        ClientResponse response = webResource.type("application/json")
                .header(AaiRestClient.HEADER_TRANS_ID, Collections.singletonList(transactionId))
                .header(AaiRestClient.HEADER_FROM_APP_ID, Collections.singletonList(AaiRestClient.ML_APP_NAME))
                .post(ClientResponse.class, obj.toString());
        String sanitizedJson = JsonSanitizer.sanitize(response.getEntity(String.class));

        if (logger.isDebugEnabled()) {
            logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                    "Babel response " + response.getStatus() + " " + sanitizedJson);
        }

        metricsLogger.info(ModelLoaderMsgs.BABEL_REST_REQUEST, new LogFields() //
                .setField(LogLine.DefinedFields.TARGET_ENTITY, "Babel")
                .setField(LogLine.DefinedFields.STATUS_CODE,
                        Response.Status.fromStatusCode(response.getStatus()).getFamily()
                                .equals(Response.Status.Family.SUCCESSFUL) ? "COMPLETE" : "ERROR")
                .setField(LogLine.DefinedFields.RESPONSE_CODE, response.getStatus())
                .setField(LogLine.DefinedFields.RESPONSE_DESCRIPTION, response.getStatusInfo().toString()), //
                override, response.toString());

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new BabelServiceClientException(sanitizedJson);
        }

        return new Gson().fromJson(sanitizedJson, new TypeToken<List<BabelArtifact>>() {}.getType());
    }
}
