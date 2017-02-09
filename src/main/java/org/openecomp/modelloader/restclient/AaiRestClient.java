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

package org.openecomp.modelloader.restclient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import org.openecomp.cl.api.LogFields;
import org.openecomp.cl.api.LogLine;
import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.cl.mdc.MdcContext;
import org.openecomp.cl.mdc.MdcOverride;
import org.openecomp.modelloader.config.ModelLoaderConfig;
import org.openecomp.modelloader.service.ModelLoaderMsgs;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class AaiRestClient {
  public enum MimeType {
    XML("application/xml"), JSON("application/json");

    private String httpType;

    MimeType(String httpType) {
      this.httpType = httpType;
    }

    String getHttpHeaderType() {
      return httpType;
    }
  }

  private static String HEADER_TRANS_ID = "X-TransactionId";
  private static String HEADER_FROM_APP_ID = "X-FromAppId";
  private static String HEADER_AUTHORIZATION = "Authorization";
  private static String ML_APP_NAME = "ModelLoader";
  private static String RESOURCE_VERSION_PARAM = "resource-version";

  private static SimpleDateFormat dateFormatter = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  private static Logger logger = LoggerFactory.getInstance()
      .getLogger(AaiRestClient.class.getName());
  private static Logger metricsLogger = LoggerFactory.getInstance()
      .getMetricsLogger(AaiRestClient.class.getName());

  private ModelLoaderConfig config = null;

  public AaiRestClient(ModelLoaderConfig config) {
    this.config = config;
  }

  /**
   * Send a PUT request to the A&AI.
   *
   * @param url
   *          - the url
   * @param transId
   *          - transaction ID
   * @param payload
   *          - the XML or JSON payload for the request
   * @param mimeType
   *          - the content type (XML or JSON)
   * @return ClientResponse
   */
  public ClientResponse putResource(String url, String payload, String transId, MimeType mimeType) {
    ClientResponse result = null;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    long startTimeInMs = 0;
    MdcOverride override = new MdcOverride();

    try {
      Client client = setupClient();

      baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      if (logger.isDebugEnabled()) {
        client.addFilter(new LoggingFilter(ps));
      }

      // Grab the current time so that we can use it for metrics purposes later.
      startTimeInMs = System.currentTimeMillis();
      override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

      if (useBasicAuth()) {
        result = client.resource(url).header(HEADER_TRANS_ID, transId)
            .header(HEADER_FROM_APP_ID, ML_APP_NAME)
            .header(HEADER_AUTHORIZATION, getAuthenticationCredentials())
            .type(mimeType.getHttpHeaderType()).put(ClientResponse.class, payload);
      } else {
        result = client.resource(url).header(HEADER_TRANS_ID, transId)
            .header(HEADER_FROM_APP_ID, ML_APP_NAME).type(mimeType.getHttpHeaderType())
            .put(ClientResponse.class, payload);
      }
    } catch (Exception ex) {
      logger.error(ModelLoaderMsgs.AAI_REST_REQUEST_ERROR, "PUT", url, ex.getLocalizedMessage());
      return null;
    } finally {
      if (logger.isDebugEnabled()) {
        logger.debug(baos.toString());
      }
    }

    if ((result != null) && ((result.getStatus() == Response.Status.CREATED.getStatusCode())
        || (result.getStatus() == Response.Status.OK.getStatusCode()))) {
      logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_SUCCESS, "PUT", url,
          Integer.toString(result.getStatus()));
      metricsLogger.info(ModelLoaderMsgs.AAI_REST_REQUEST_SUCCESS,
          new LogFields().setField(LogLine.DefinedFields.RESPONSE_CODE, result.getStatus())
              .setField(LogLine.DefinedFields.RESPONSE_DESCRIPTION,
                  result.getResponseStatus().toString()),
          override, "PUT", url, Integer.toString(result.getStatus()));
    } else {
      // If response is not 200 OK, then additionally log the reason
      String respMsg = result.getEntity(String.class);
      if (respMsg == null) {
        respMsg = result.getStatusInfo().getReasonPhrase();
      }
      logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_UNSUCCESSFUL, "PUT", url,
          Integer.toString(result.getStatus()), respMsg);
      metricsLogger.info(ModelLoaderMsgs.AAI_REST_REQUEST_UNSUCCESSFUL,
          new LogFields().setField(LogLine.DefinedFields.RESPONSE_CODE, result.getStatus())
              .setField(LogLine.DefinedFields.RESPONSE_DESCRIPTION,
                  result.getResponseStatus().toString()),
          override, "PUT", url, Integer.toString(result.getStatus()), respMsg);
    }

    return result;
  }

  /**
   * Send a DELETE request to the A&AI.
   *
   * @param url
   *          - the url
   * @param resourceVersion
   *          - the resource-version of the model to delete
   * @param transId
   *          - transaction ID
   * @return ClientResponse
   */
  public ClientResponse deleteResource(String url, String resourceVersion, String transId) {
    ClientResponse result = null;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    long startTimeInMs = 0;
    MdcOverride override = new MdcOverride();

    try {
      Client client = setupClient();

      baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      if (logger.isDebugEnabled()) {
        client.addFilter(new LoggingFilter(ps));
      }

      // Grab the current time so that we can use it for metrics purposes later.
      startTimeInMs = System.currentTimeMillis();
      override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

      if (useBasicAuth()) {
        result = client.resource(url).queryParam(RESOURCE_VERSION_PARAM, resourceVersion)
            .header(HEADER_TRANS_ID, transId).header(HEADER_FROM_APP_ID, ML_APP_NAME)
            .header(HEADER_AUTHORIZATION, getAuthenticationCredentials())
            .delete(ClientResponse.class);
      } else {
        result = client.resource(url).queryParam(RESOURCE_VERSION_PARAM, resourceVersion)
            .header(HEADER_TRANS_ID, transId).header(HEADER_FROM_APP_ID, ML_APP_NAME)
            .delete(ClientResponse.class);
      }
    } catch (Exception ex) {
      logger.error(ModelLoaderMsgs.AAI_REST_REQUEST_ERROR, "DELETE", url, ex.getLocalizedMessage());
      return null;
    } finally {
      if (logger.isDebugEnabled()) {
        logger.debug(baos.toString());
      }
    }

    logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_SUCCESS, "DELETE", url,
        Integer.toString(result.getStatus()));
    metricsLogger.info(ModelLoaderMsgs.AAI_REST_REQUEST_SUCCESS,
        new LogFields().setField(LogLine.DefinedFields.RESPONSE_CODE, result.getStatus()).setField(
            LogLine.DefinedFields.RESPONSE_DESCRIPTION, result.getResponseStatus().toString()),
        override, "DELETE", url, Integer.toString(result.getStatus()));

    return result;
  }

  /**
   * Send a GET request to the A&AI for a resource.
   *
   * @param url
   *          - the url to use
   * @param transId
   *          - transaction ID
   * @return ClientResponse
   */
  public ClientResponse getResource(String url, String transId, MimeType mimeType) {
    ClientResponse result = null;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    long startTimeInMs = 0;
    MdcOverride override = new MdcOverride();

    try {
      Client client = setupClient();

      baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      if (logger.isDebugEnabled()) {
        client.addFilter(new LoggingFilter(ps));
      }

      // Grab the current time so that we can use it for metrics purposes later.
      startTimeInMs = System.currentTimeMillis();
      override.addAttribute(MdcContext.MDC_START_TIME, dateFormatter.format(startTimeInMs));

      if (useBasicAuth()) {
        result = client.resource(url).header(HEADER_TRANS_ID, transId)
            .header(HEADER_FROM_APP_ID, ML_APP_NAME).accept(mimeType.getHttpHeaderType())
            .header(HEADER_AUTHORIZATION, getAuthenticationCredentials()).get(ClientResponse.class);
      } else {
        result = client.resource(url).header(HEADER_TRANS_ID, transId)
            .header(HEADER_FROM_APP_ID, ML_APP_NAME).accept(mimeType.getHttpHeaderType())
            .get(ClientResponse.class);

      }
    } catch (Exception ex) {
      logger.error(ModelLoaderMsgs.AAI_REST_REQUEST_ERROR, "GET", url, ex.getLocalizedMessage());
      return null;
    } finally {
      if (logger.isDebugEnabled()) {
        logger.debug(baos.toString());
      }
    }

    logger.info(ModelLoaderMsgs.AAI_REST_REQUEST_SUCCESS, "GET", url,
        Integer.toString(result.getStatus()));
    metricsLogger.info(ModelLoaderMsgs.AAI_REST_REQUEST_SUCCESS,
        new LogFields().setField(LogLine.DefinedFields.RESPONSE_CODE, result.getStatus()).setField(
            LogLine.DefinedFields.RESPONSE_DESCRIPTION, result.getResponseStatus().toString()),
        override, "GET", url, Integer.toString(result.getStatus()));

    return result;
  }

  /**
   * Does a GET on a resource to retrieve the resource version, and then DELETE
   * that version.
   *
   * @param url
   *          - the url
   * @param transId
   *          - transaction ID
   * @return ClientResponse
   */
  public ClientResponse getAndDeleteResource(String url, String transId) {
    // First, GET the model
    ClientResponse getResponse = getResource(url, transId, MimeType.XML);
    if ((getResponse == null) || (getResponse.getStatus() != Response.Status.OK.getStatusCode())) {
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

  private Client setupClient() throws IOException, GeneralSecurityException {
    ClientConfig clientConfig = new DefaultClientConfig();

    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
      @Override
      public boolean verify(String string, SSLSession ssls) {
        return true;
      }
    });

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(X509Certificate[] certs, String authType) {}

      @Override
      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    } };

    SSLContext ctx = SSLContext.getInstance("TLS");
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    FileInputStream fin = new FileInputStream(config.getAaiKeyStorePath());
    KeyStore ks = KeyStore.getInstance("PKCS12");
    char[] pwd = config.getAaiKeyStorePassword().toCharArray();
    ks.load(fin, pwd);
    kmf.init(ks, pwd);

    ctx.init(kmf.getKeyManagers(), trustAllCerts, null);
    clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
        new HTTPSProperties(new HostnameVerifier() {
          @Override
          public boolean verify(String theString, SSLSession sslSession) {
            return true;
          }
        }, ctx));

    Client client = Client.create(clientConfig);

    return client;
  }

  private String getResourceVersion(ClientResponse response)
      throws ParserConfigurationException, SAXException, IOException {
    String respData = response.getEntity(String.class);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    InputSource is = new InputSource(new StringReader(respData));
    Document doc = builder.parse(is);

    NodeList nodeList = doc.getDocumentElement().getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node currentNode = nodeList.item(i);
      if (currentNode.getNodeName().equals(RESOURCE_VERSION_PARAM)) {
        return currentNode.getTextContent();
      }
    }

    return null;
  }

  private String getAuthenticationCredentials() {

    String usernameAndPassword = config.getAaiAuthenticationUser() + ":"
        + config.getAaiAuthenticationPassword();
    return "Basic " + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
  }

  public boolean useBasicAuth() {
    return (config.getAaiAuthenticationUser() != null)
        && (config.getAaiAuthenticationPassword() != null);
  }
}
