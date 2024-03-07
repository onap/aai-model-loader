/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright (c) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (c) 2017-2019 European Software Marketing Ltd.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.onap.sdc.api.consumer.IConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.NonNull;

/**
 * Properties for the Model Loader
 *
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "model-loader")
public class ModelLoaderConfig {

    @Nonnull private final DistributionProperties distributionProperties;
    @NonNull private final AaiProperties aaiProperties;
    @NonNull private final BabelProperties babelProperties;
    @NonNull private final DebugProperties debugProperties;

    @Data
    @ConstructorBinding
    @ConfigurationProperties(prefix = "model-loader.distribution")
    public static class DistributionProperties implements IConfiguration {
        private final boolean serverTlsAuthActive;
        private final boolean asdcConnectionDisabled;
        private final boolean filterInEmptyResources;
        private final String asdcAddress;
        private final String consumerGroup;
        private final String consumerID;
        private final String environmentName;
        private final String keyStorePassword;
        private final String keyStorePath;
        private final String password;
        private final int pollingInterval;
        private final int pollingTimeout;
        private final String user;
        private final List<String> artifactTypes;

        private final String httpProxyHost;
        private final int httpProxyPort;
        private final String httpsProxyHost;
        private final int httpsProxyPort;
        private final String sdcAddress;
        private final List<String> relevantArtifactTypes;

        public boolean activateServerTLSAuth() {
            return serverTlsAuthActive;
        }
        // public String getConsumerID() {
        //     return consumerId;
        // }
    }

    @Data
    @ConfigurationProperties(prefix = "model-loader.aai")
    public static class AaiProperties {
        private final String baseUrl;
        private final String keystoreFile;
        private final String keystorePassword;
        private final String modelResourceUrl;
        private final String namedQueryResourceUrl;
        private final String vnfImageUrl;
        private final String authenticationUser;
        private final String authenticationPassword;
        private final boolean useGizmo;
        private final boolean useHttps;
    }

    @Data
    @ConfigurationProperties(prefix = "model-loader.babel")
    public static class BabelProperties {
        private final String baseUrl;
        private final String keystoreFile;
        private final String keystorePassword;
        private final String truststoreFile;
        private final String truststorePassword;
        private final String generateResourceUrl;
        private final boolean useHttps;
    }

    @Data
    @ConfigurationProperties(prefix = "model-loader.debug")
    public static class DebugProperties {
        private boolean ingestSimulatorEnabled;
    }

    private Properties modelLoaderProperties = null;
    private String certLocation = ".";
    private final List<String> artifactTypes = new ArrayList<>();
    private String modelVersion = null;
}
