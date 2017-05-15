/**
 * ============LICENSE_START=======================================================
 * Model Loader
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP and OpenECOMP are trademarks
 * and service marks of AT&T Intellectual Property.
 */
package org.openecomp.modelloader.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.util.security.Password;
import org.junit.Test;
import org.openecomp.modelloader.restclient.AaiRestClient;

import org.openecomp.sdc.utils.ArtifactTypeEnum;

public class ModelLoaderConfigTest {

  @Test
  public void testYangModelArtifactType() {
    Properties props = new Properties();
    props.setProperty("ml.distribution.ARTIFACT_TYPES",
        "MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG");
    ModelLoaderConfig config = new ModelLoaderConfig(props, null);

    List<String> types = config.getRelevantArtifactTypes();

    System.out.println("ArtifactType: " + types.get(0));
    assertEquals(0,
        types.get(0).compareToIgnoreCase(ArtifactTypeEnum.MODEL_INVENTORY_PROFILE.toString()));

    System.out.println("ArtifactType: " + types.get(1));
    assertEquals(0, types.get(1).compareToIgnoreCase(ArtifactTypeEnum.MODEL_QUERY_SPEC.toString()));

    System.out.println("ArtifactType: " + types.get(2));
    assertEquals(0, types.get(2).compareToIgnoreCase(ArtifactTypeEnum.VNF_CATALOG.toString()));

    assertEquals(3, types.size());
  }

  @Test
  public void testDecryptPassword() {
    Properties props = new Properties();
    String testPass = "youshallnotpass";
    String encryptedTestPass = Password.obfuscate(testPass);

    System.out.println("Encrypt " + testPass + " ==> " + encryptedTestPass);

    props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_PASSWORD, encryptedTestPass);
    ModelLoaderConfig config = new ModelLoaderConfig(props, null);

    assertEquals(testPass, config.getPassword());
  }

  @Test
  public void testDecryptKeystorePassword() {
    Properties props = new Properties();
    String testPass = "youshallnotpass";
    String encryptedTestPass = Password.obfuscate(testPass);

    System.out.println("Encrypt " + testPass + " ==> " + encryptedTestPass);

    props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_KEYSTORE_PASSWORD, encryptedTestPass);
    ModelLoaderConfig config = new ModelLoaderConfig(props, null);

    assertEquals(testPass, config.getKeyStorePassword());
  }

  @Test
  public void testDecryptAAIPassword() {

    Properties props = new Properties();
    String testPassword = "myvoiceismypassword";
    String encryptedTestPassword = Password.obfuscate(testPassword);

    props.put(ModelLoaderConfig.PROP_AAI_AUTHENTICATION_PASSWORD, encryptedTestPassword);
    ModelLoaderConfig config = new ModelLoaderConfig(props, null);

    assertEquals(testPassword, config.getAaiAuthenticationPassword());
  }

  @Test
  public void testNoAAIAuth() throws IOException {

    Properties props = new Properties();
    props.load(
        new FileInputStream("src/test/resources/model-loader-empty-auth-password.properties"));

    ModelLoaderConfig config = new ModelLoaderConfig(props, null);
    AaiRestClient aaiClient = new AaiRestClient(config);

    assertFalse("Empty AAI Password should result in no basic authentication",
        aaiClient.useBasicAuth());

    props.load(new FileInputStream("src/test/resources/model-loader-no-auth-password.properties"));
    config = new ModelLoaderConfig(props, null);
    aaiClient = new AaiRestClient(config);

    assertFalse("No AAI Password should result in no basic authentication",
        aaiClient.useBasicAuth());
  }
  
  @Test
  public void testGetUrls() { 
    Properties props = new Properties();
    props.put(ModelLoaderConfig.PROP_AAI_MODEL_RESOURCE_URL, "/aai/v*/service-design-and-creation/models/model/");
    props.put(ModelLoaderConfig.PROP_AAI_NAMED_QUERY_RESOURCE_URL, "/aai/v*/service-design-and-creation/named-queries/named-query/");
    ModelLoaderConfig config = new ModelLoaderConfig(props, null);

    assertEquals("/aai/v9/service-design-and-creation/models/model/", config.getAaiModelUrl("v9"));
    assertEquals("/aai/v10/service-design-and-creation/named-queries/named-query/", config.getAaiNamedQueryUrl("v10"));
  }
}
