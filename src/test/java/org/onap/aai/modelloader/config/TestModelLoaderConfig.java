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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.Test;
import org.onap.sdc.utils.ArtifactTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for ModelLoaderConfig class.
 *
 */
@SpringBootTest
@TestPropertySource(
    properties = "model-loader.distribution.artifact-types=MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG")
public class TestModelLoaderConfig {

    @Autowired ModelLoaderConfig config;

    @Test
    public void testYangModelArtifactType() {
        List<String> types = config.getDistributionProperties().getArtifactTypes();

        System.out.println("ArtifactType: " + types.get(0));
        assertEquals(0, types.get(0).compareToIgnoreCase(ArtifactTypeEnum.MODEL_INVENTORY_PROFILE.toString()));

        System.out.println("ArtifactType: " + types.get(1));
        assertEquals(0, types.get(1).compareToIgnoreCase(ArtifactTypeEnum.MODEL_QUERY_SPEC.toString()));

        System.out.println("ArtifactType: " + types.get(2));
        assertEquals(0, types.get(2).compareToIgnoreCase(ArtifactTypeEnum.VNF_CATALOG.toString()));

        assertEquals(3, types.size());
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
        assertEquals(password, config.getAaiProperties().getAuthenticationPassword());
        
        config = createUnobfuscatedTestConfig(ModelLoaderConfig.PROP_AAI_AUTHENTICATION_PASSWORD, password);
        assertEquals(password, config.getAaiProperties().getAuthenticationPassword());
    }

    @Test
    public void testDecryptAaiKeystorePassword() {
        String password = "myvoiceismypassword";
        ModelLoaderConfig config = createObfuscatedTestConfig(ModelLoaderConfig.PROP_AAI_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getAaiProperties().getKeyStorePassword());
        
        config = createUnobfuscatedTestConfig(ModelLoaderConfig.PROP_AAI_KEYSTORE_PASSWORD, password);
        assertEquals(password, config.getAaiProperties().getKeyStorePassword());
    }

    @Test
    public void testAaiBaseUrl() {
        String url = "http://localhost:1234/";
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_AAI_BASE_URL, url);
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);
        assertEquals(url, config.getAaiProperties().getBaseUrl());
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
    public void testGetUrls() {
        Properties props = new Properties();
        props.put(ModelLoaderConfig.PROP_AAI_MODEL_RESOURCE_URL, "/aai/v*/service-design-and-creation/models/model/");
        props.put(ModelLoaderConfig.PROP_AAI_NAMED_QUERY_RESOURCE_URL,
                "/aai/v*/service-design-and-creation/named-queries/named-query/");
        ModelLoaderConfig config = new ModelLoaderConfig(props, null);

        assertEquals("/aai/v9/service-design-and-creation/models/model/", config.getAaiProperties().getModelUrl("v9"));
        assertEquals("/aai/v10/service-design-and-creation/named-queries/named-query/",
                config.getAaiProperties().getNamedQueryUrl("v10"));
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
