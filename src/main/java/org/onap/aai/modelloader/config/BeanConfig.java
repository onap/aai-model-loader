/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom AG Intellectual Property. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.impl.DistributionClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BeanConfig {

    private static final Logger logger = LoggerFactory.getInstance().getLogger(BeanConfig.class);


    @Value("${CONFIG_HOME}")
    private String configDir;

    @Bean
    public Properties configProperties() throws IOException {
        // Load model loader system configuration
        logger.info(ModelLoaderMsgs.LOADING_CONFIGURATION);
        InputStream configInputStream = Files.newInputStream(Paths.get(configDir, "model-loader.properties"));
        Properties configProperties = new Properties();
        configProperties.load(configInputStream);
        return configProperties;
    }

    @Bean
    public ModelLoaderConfig modelLoaderConfig(Properties configProperties) {
        ModelLoaderConfig.setConfigHome(configDir);
        return new ModelLoaderConfig(configProperties);
    }
    
    @Bean
    public IDistributionClient iDistributionClient() {
        return DistributionClientFactory.createDistributionClient();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
