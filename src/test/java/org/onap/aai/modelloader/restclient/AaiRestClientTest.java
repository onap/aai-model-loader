/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.modelloader.restclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.model.ModelArtifact;

public class AaiRestClientTest {

  // This test requires a running A&AI system. Uncomment to test locally.
  /*
   * @Test public void testRestClient() throws Exception { final String
   * MODEL_FILE = "src/test/resources/models/vnf-model.xml";
   * 
   * Properties props = new Properties();
   * props.setProperty("ml.distribution.ARTIFACT_TYPES",
   * "MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG");
   * props.setProperty("ml.aai.BASE_URL", "https://127.0.0.1:4321");
   * props.setProperty("ml.aai.MODEL_URL",
   * "/aai/v8/service-design-and-creation/models/model/");
   * props.setProperty("ml.aai.KEYSTORE_FILE", "aai-client-cert.p12");
   * props.setProperty("ml.aai.KEYSTORE_PASSWORD",
   * "OBF:1i9a1u2a1unz1lr61wn51wn11lss1unz1u301i6o");
   * 
   * ModelLoaderConfig config = new ModelLoaderConfig(props, "");
   * 
   * String payload = readFile(MODEL_FILE); System.out.println("FILE:" +
   * payload);
   * 
   * File xmlFile = new File(MODEL_FILE); DocumentBuilderFactory dbFactory =
   * DocumentBuilderFactory.newInstance(); DocumentBuilder dBuilder =
   * dbFactory.newDocumentBuilder(); Document doc = dBuilder.parse(xmlFile);
   * 
   * // Get the ID of the model String modelId = null; NodeList nodeList =
   * doc.getDocumentElement().getChildNodes(); for (int i = 0; i <
   * nodeList.getLength(); i++) { Node currentNode = nodeList.item(i); if
   * (currentNode.getNodeName().equals("model-name-version-id")) { modelId =
   * currentNode.getTextContent(); break; } }
   * 
   * // Add the model try { ModelArtifact model = new ModelArtifact();
   * model.setNameVersionId(modelId); model.setType(ArtifactType.MODEL);
   * model.setPayload(payload);
   * 
   * AAIRestClient aaiClient = new AAIRestClient(config);
   * 
   * // GET model System.out.println("Calling GET API ..."); ClientResponse
   * getResponse = aaiClient.getResource(getURL(model, config),
   * "example-trans-id-0", AAIRestClient.MimeType.XML); System.out.println(
   * "GET result: " + getResponse.getStatus());
   * assertTrue(getResponse.getStatus() ==
   * Response.Status.NOT_FOUND.getStatusCode());
   * 
   * // Add the model System.out.println("Calling PUT API ..."); ClientResponse
   * res = aaiClient.putResource(getURL(model, config), model.getPayload(),
   * "example-trans-id-1", AAIRestClient.MimeType.XML); System.out.println(
   * "PUT result: " + res.getStatus()); assertTrue(res.getStatus() ==
   * Response.Status.CREATED.getStatusCode());
   * 
   * // Delete the model System.out.println("Calling DELETE API ..."); res =
   * aaiClient.getAndDeleteResource(getURL(model, config),
   * "example-trans-id-3"); System.out.println("DELETE result: " +
   * res.getStatus()); assertTrue(res.getStatus() ==
   * Response.Status.NO_CONTENT.getStatusCode()); } catch (Exception e) {
   * e.printStackTrace(); } }
   */

  static String readFile(String path) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded);
  }

  private String getURL(ModelArtifact model, ModelLoaderConfig config) {
    String baseURL = config.getAaiBaseUrl().trim();
    String subURL = null;
    if (model.getType().equals(ArtifactType.MODEL)) {
      subURL = config.getAaiModelUrl(model.getModelNamespaceVersion()).trim();
    } else {
      subURL = config.getAaiNamedQueryUrl(model.getModelNamespaceVersion()).trim();
    }

    if ((!baseURL.endsWith("/")) && (!subURL.startsWith("/"))) {
      baseURL = baseURL + "/";
    }

    if (baseURL.endsWith("/") && subURL.startsWith("/")) {
      baseURL = baseURL.substring(0, baseURL.length() - 1);
    }

    if (!subURL.endsWith("/")) {
      subURL = subURL + "/";
    }

    String url = baseURL + subURL + model.getUniqueIdentifier();
    return url;
  }
}
