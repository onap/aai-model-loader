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
package org.onap.aai.modelloader.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.util.security.Password;
import org.junit.Test;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.restclient.AaiRestClient;
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

  @Test
  public void testActivateServerTLSAuth(){
    Properties props = new Properties();
    props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_ACTIVE_SERVER_TLS_AUTH, "true");
    ModelLoaderConfig config = new ModelLoaderConfig(props, null);
    boolean authValue = config.activateServerTLSAuth();
    assertTrue(authValue);

    props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_ACTIVE_SERVER_TLS_AUTH, "");
    ModelLoaderConfig config1 = new ModelLoaderConfig(props, null);
    boolean authValue1 = config.activateServerTLSAuth();
    assertFalse(authValue1);
  }

    @Test
    public void testGetAsdcAddress(){
      Properties props = new Properties();
      props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_ASDC_ADDRESS, "address-1");
      ModelLoaderConfig config = new ModelLoaderConfig(props, null);
      String asdcAddr = config.getAsdcAddress();
      assertEquals(asdcAddr, "address-1");
    }

    @Test
    public void testGetConsumerGroup(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_CONSUMER_GROUP, "group-1");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        String ret = config.getConsumerGroup();
        assertEquals(ret, "group-1");
    }

    @Test
    public void testGetConsumerID(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_CONSUMER_ID, "id-1");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        String ret = config.getConsumerID();
        assertEquals(ret, "id-1");
    }

    @Test
    public void testGetEnvironmentName(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_ENVIRONMENT_NAME, "local");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        String ret = config.getEnvironmentName();
        assertEquals(ret, "local");
    }

    @Test
    public void testGetKeyStorePath(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_KEYSTORE_FILE, "keystore-file");
        ModelLoaderConfig config = new ModelLoaderConfig(props, "local/");
        String ret = config.getKeyStorePath();
        assertEquals(ret, "local/keystore-file");
    }

    @Test
    public void testGetPollingInterval(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_POLLING_INTERVAL, "60");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        int ret = config.getPollingInterval();
        assertTrue(ret == 60);
    }

    @Test
    public void testGetPollingTimeout(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_POLLING_TIMEOUT, "30");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        int ret = config.getPollingTimeout();
        assertTrue(ret == 30);
    }

    @Test
    public void testGetUser(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_USER, "user-1");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        String ret = config.getUser();
        assertEquals(ret, "user-1");
    }

    @Test
    public void testIsFilterInEmptyResources(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_POLLING_TIMEOUT, "30");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        Boolean ret = config.isFilterInEmptyResources();
        assertFalse(ret);
    }

    @Test
    public void testIsUseHttpsWithDmaap(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_ML_DISTRIBUTION_HTTPS_WITH_DMAAP, "true");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        Boolean ret = config.isUseHttpsWithDmaap();
        assertTrue(ret);
    }

    @Test
    public void testGetAaiKeyStorePath(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_AAI_KEYSTORE_FILE, "keystore-file");
        ModelLoaderConfig config = new ModelLoaderConfig(props, "local");
        String ret = config.getAaiKeyStorePath();
        assertEquals(ret, "local/keystore-file");
    }

    @Test
    public void testGetAaiKeyStorePassword(){
        Properties props = new Properties();
        String testPass = "youshallnotpass";
        String encryptedTestPass = Password.obfuscate(testPass);

        props.put(ModelLoaderConfig.PROP_AAI_KEYSTORE_PASSWORD, encryptedTestPass);
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);

        assertEquals(testPass, config.getAaiKeyStorePassword());
    }

    @Test
    public void testGetIngestSimulatorEnabled(){
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_DEBUG_INGEST_SIMULATOR, "enabled");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        boolean ret = config.getIngestSimulatorEnabled();
        assertTrue(ret);

        props.put(ModelLoaderConfig.PROP_DEBUG_INGEST_SIMULATOR, "disabled");
        ModelLoaderConfig config1 = new ModelLoaderConfig(props, null);
        boolean ret1 = config.getIngestSimulatorEnabled();
        assertFalse(ret1);
    }
}
