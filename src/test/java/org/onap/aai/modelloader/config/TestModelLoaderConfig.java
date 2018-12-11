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
package org.onap.aai.modelloader.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.eclipse.jetty.util.security.Password;
import org.junit.Test;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.sdc.utils.ArtifactTypeEnum;

/**
 * Tests for ModelLoaderConfig class.
 *
 */
public class TestModelLoaderConfig {

    @Test
    public void testYangModelArtifactType() {
        Properties props = new Properties();
        props.setProperty("ml.distribution.ARTIFACT_TYPES", "MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);

        List<String> types = config.getRelevantArtifactTypes();

        System.out.println("ArtifactType: " + types.get(0));
        assertEquals(0, types.get(0).compareToIgnoreCase(ArtifactTypeEnum.MODEL_INVENTORY_PROFILE.toString()));

        System.out.println("ArtifactType: " + types.get(1));
        assertEquals(0, types.get(1).compareToIgnoreCase(ArtifactTypeEnum.MODEL_QUERY_SPEC.toString()));

        System.out.println("ArtifactType: " + types.get(2));
        assertEquals(0, types.get(2).compareToIgnoreCase(ArtifactTypeEnum.VNF_CATALOG.toString()));

        assertEquals(3, types.size());
    }

    @Test
    public void testMsgBusAddrs() {
        Properties props = new Properties();
        props.setProperty("ml.distribution.MSG_BUS_ADDRESSES", "host1.onap.com:3904,host2.onap.com:3904");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);

        List<String> addrs = config.getMsgBusAddress();

        assertEquals(2, addrs.size());
        assertEquals(0, addrs.get(0).compareToIgnoreCase("host1.onap.com:3904"));
        assertEquals(0, addrs.get(1).compareToIgnoreCase("host2.onap.com:3904"));
    }

    @Test
    public void testDecryptPassword() {
        String password = "youshallnotpass";
        ModelLoaderConfig config =
                createObfuscatedTestConfig(ModelLoaderConfig.PROP_ML_DISTRIBUTION_PASSWORD, password);
        assertEquals(password, config.getPassword());
        
        config = createUnobfuscatedTestConfig(ModelLoaderConfig.PROP_ML_DISTRIBUTION_PASSWORD, password);
        assertEquals(password, config.getPassword());
    }

    @Test
    public void testDecryptKeystorePassword() {
        String password = "youshallnotpass";
        ModelLoaderConfig config =
                createObfuscatedTestConfig(ModelLoaderConfig.PROP_ML_DISTRIBUTION_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getKeyStorePassword());
        
        config = createUnobfuscatedTestConfig(ModelLoaderConfig.PROP_ML_DISTRIBUTION_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getKeyStorePassword());
    }

    @Test
    public void testDecryptAaiAuthenticationPassword() {
        String password = "myvoiceismypassword";
        ModelLoaderConfig config =
                createObfuscatedTestConfig(ModelLoaderConfig.PROP_AAI_AUTHENTICATION_PASSWORD, password);
        assertEquals(password, config.getAaiAuthenticationPassword());
        
        config = createUnobfuscatedTestConfig(ModelLoaderConfig.PROP_AAI_AUTHENTICATION_PASSWORD, password);
        assertEquals(password, config.getAaiAuthenticationPassword());
    }

    @Test
    public void testDecryptAaiKeystorePassword() {
        String password = "myvoiceismypassword";
        ModelLoaderConfig config = createObfuscatedTestConfig(ModelLoaderConfig.PROP_AAI_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getAaiKeyStorePassword());
        
        config = createUnobfuscatedTestConfig(ModelLoaderConfig.PROP_AAI_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getAaiKeyStorePassword());
    }

    @Test
    public void testAaiBaseUrl() {
        String url = "http://localhost:1234/";
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_AAI_BASE_URL, url);
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        assertEquals(url, config.getAaiBaseUrl());
    }

    @Test
    public void testDecryptBabelKeystorePassword() {
        String password = "babelpassword";
        ModelLoaderConfig config = createObfuscatedTestConfig(ModelLoaderConfig.PROP_BABEL_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getBabelKeyStorePassword());
        
        config = createUnobfuscatedTestConfig(ModelLoaderConfig.PROP_BABEL_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getBabelKeyStorePassword());
    }

    @Test
    public void testBabelKeystorePath() {
        String root = "path_to_keystore";
        String path = "relative_keystore_path";
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_BABEL_KEYSTORE_FILE, path);
        ModelLoaderConfig config = new ModelLoaderConfig(props, root);
        assertEquals(root + File.separator + path, config.getBabelKeyStorePath());
    }

    @Test
    public void testBabelBaseUrl() {
        String url = "http://localhost/";
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_BABEL_BASE_URL, url);
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        assertEquals(url, config.getBabelBaseUrl());
    }

    @Test
    public void testBabelGenerateArtifactsUrl() {
        String url = "/path/to/the/resource";
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_BABEL_GENERATE_RESOURCE_URL, url);
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        assertEquals(url, config.getBabelGenerateArtifactsUrl());
    }

    @Test
    public void testMissingAuthenticationProperties() throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("src/test/resources/model-loader-empty-auth-password.properties"));

        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        AaiRestClient aaiClient = new AaiRestClient(config);

        assertFalse("Empty AAI Password should result in no basic authentication", aaiClient.useBasicAuth());

        props.load(new FileInputStream("src/test/resources/model-loader-no-auth-password.properties"));
        config = new ModelLoaderConfig(props, null);
        aaiClient = new AaiRestClient(config);

        assertFalse("No AAI Password should result in no basic authentication", aaiClient.useBasicAuth());
    }

    @Test
    public void testGetUrls() {
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_AAI_MODEL_RESOURCE_URL, "/aai/v*/service-design-and-creation/models/model/");
        props.put(ModelLoaderConfig.PROP_AAI_NAMED_QUERY_RESOURCE_URL,
                "/aai/v*/service-design-and-creation/named-queries/named-query/");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);

        assertEquals("/aai/v9/service-design-and-creation/models/model/", config.getAaiModelUrl("v9"));
        assertEquals("/aai/v10/service-design-and-creation/named-queries/named-query/",
                config.getAaiNamedQueryUrl("v10"));
    }


    /**
     * Create a Model Loader Configuration object from the supplied Property.
     * 
     * @param propertyName property key
     * @param propertyValue value of the property
     * @return a new ModelLoaderConfig object containing a single obfuscated property value
     */
    private ModelLoaderConfig createObfuscatedTestConfig(String propertyName, String propertyValue) {
        Properties props = new Properties();
        props.put(propertyName, Password.obfuscate(propertyValue));
        return new ModelLoaderConfig(props, null);
    }
    
    private ModelLoaderConfig createUnobfuscatedTestConfig(String propertyName, String propertyValue) {
        Properties props = new Properties();
        props.put(propertyName, propertyValue);
        return new ModelLoaderConfig(props, null);
    }
}
