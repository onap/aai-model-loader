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

import java.util.List;
import java.util.Properties;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.Test;
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
