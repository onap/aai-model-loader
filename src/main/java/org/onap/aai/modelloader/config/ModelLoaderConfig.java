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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.security.Password;
import org.onap.sdc.api.consumer.IConfiguration;
/**
 * Properties for the Model Loader
 *
 */
public class ModelLoaderConfig implements IConfiguration {

    // Configuration file structure
    public static final String PREFIX_MODEL_LOADER_CONFIG = "ml";
    public static final String PREFIX_DISTRIBUTION_CLIENT = PREFIX_MODEL_LOADER_CONFIG + ".distribution.";
    public static final String PREFIX_DEBUG = PREFIX_MODEL_LOADER_CONFIG + ".debug.";

    private static final String SUFFIX_KEYSTORE_FILE = "KEYSTORE_FILE";
    private static final String SUFFIX_KEYSTORE_PASS = "KEYSTORE_PASSWORD";

    // Configuration file properties
    protected static final String PROP_ML_DISTRIBUTION_ACTIVE_SERVER_TLS_AUTH =
            PREFIX_DISTRIBUTION_CLIENT + "ACTIVE_SERVER_TLS_AUTH";
    protected static final String PROP_ML_DISTRIBUTION_ASDC_CONNECTION_DISABLED =
            PREFIX_DISTRIBUTION_CLIENT + "ASDC_CONNECTION_DISABLE";
    protected static final String PROP_ML_DISTRIBUTION_ASDC_ADDRESS = PREFIX_DISTRIBUTION_CLIENT + "ASDC_ADDRESS";
    protected static final String PROP_ML_DISTRIBUTION_ASDC_USE_HTTPS = PREFIX_DISTRIBUTION_CLIENT + "ASDC_USE_HTTPS";
    protected static final String PROP_ML_DISTRIBUTION_CONSUMER_GROUP = PREFIX_DISTRIBUTION_CLIENT + "CONSUMER_GROUP";
    protected static final String PROP_ML_DISTRIBUTION_CONSUMER_ID = PREFIX_DISTRIBUTION_CLIENT + "CONSUMER_ID";
    protected static final String PROP_ML_DISTRIBUTION_ENVIRONMENT_NAME =
            PREFIX_DISTRIBUTION_CLIENT + "ENVIRONMENT_NAME";
    protected static final String PROP_ML_DISTRIBUTION_KEYSTORE_PASSWORD =
            PREFIX_DISTRIBUTION_CLIENT + SUFFIX_KEYSTORE_PASS;
    protected static final String PROP_ML_DISTRIBUTION_KEYSTORE_FILE =
            PREFIX_DISTRIBUTION_CLIENT + SUFFIX_KEYSTORE_FILE;
    protected static final String PROP_ML_DISTRIBUTION_PASSWORD = PREFIX_DISTRIBUTION_CLIENT + "PASSWORD";
    protected static final String PROP_ML_DISTRIBUTION_POLLING_INTERVAL =
            PREFIX_DISTRIBUTION_CLIENT + "POLLING_INTERVAL";
    protected static final String PROP_ML_DISTRIBUTION_POLLING_TIMEOUT = PREFIX_DISTRIBUTION_CLIENT + "POLLING_TIMEOUT";
    protected static final String PROP_ML_DISTRIBUTION_USER = PREFIX_DISTRIBUTION_CLIENT + "USER";
    protected static final String PROP_ML_DISTRIBUTION_ARTIFACT_TYPES = PREFIX_DISTRIBUTION_CLIENT + "ARTIFACT_TYPES";
    protected static final String PROP_ML_DISTRIBUTION_HTTP_PROXY_HOST = PREFIX_DISTRIBUTION_CLIENT + "HTTP_PROXY_HOST";
    protected static final String PROP_ML_DISTRIBUTION_HTTP_PROXY_PORT = PREFIX_DISTRIBUTION_CLIENT + "HTTP_PROXY_PORT";
    protected static final String PROP_ML_DISTRIBUTION_HTTPS_PROXY_HOST = PREFIX_DISTRIBUTION_CLIENT + "HTTPS_PROXY_HOST";
    protected static final String PROP_ML_DISTRIBUTION_HTTPS_PROXY_PORT = PREFIX_DISTRIBUTION_CLIENT + "HTTPS_PROXY_PORT";
    protected static final String PROP_ML_DISTRIBUTION_SASL_JAAS_CONFIG = PREFIX_DISTRIBUTION_CLIENT + "SASL_JAAS_CONFIG";
    protected static final String PROP_ML_DISTRIBUTION_SASL_MECHANISM = PREFIX_DISTRIBUTION_CLIENT + "SASL_MECHANISM";
    protected static final String PROP_ML_DISTRIBUTION_SECURITY_PROTOCOL = PREFIX_DISTRIBUTION_CLIENT + "SECURITY_PROTOCOL";
    protected static final String PROP_DEBUG_INGEST_SIMULATOR = PREFIX_DEBUG + "INGEST_SIMULATOR";
    protected static final String FILESEP =
            (System.getProperty("file.separator") == null) ? "/" : System.getProperty("file.separator");
    private static String configHome;
    private Properties modelLoaderProperties = null;
    private String certLocation = ".";
    private final List<String> artifactTypes = new ArrayList<>();
    private String modelVersion = null;

    public ModelLoaderConfig(Properties configProperties) {
        this(configProperties, ModelLoaderConfig.configHome + FILESEP + "auth" + FILESEP);
    }

    /**
     * Original constructor
     *
     * @param modelLoaderProperties
     *            properties needed to be configured for the model loader
     * @param certLocation
     *            location of the certificate
     */
    public ModelLoaderConfig(Properties modelLoaderProperties, String certLocation) {
        this.modelLoaderProperties = modelLoaderProperties;
        this.certLocation = certLocation;

        // Get list of artifact types
        String types = get(PROP_ML_DISTRIBUTION_ARTIFACT_TYPES);
        if (types != null) {
            artifactTypes.addAll(Arrays.asList(types.split(",")));
        }
    }

    public static void setConfigHome(String configHome) {
        ModelLoaderConfig.configHome = configHome;
    }

    public static Path propertiesFile() {
        return Paths.get(configHome, "model-loader.properties");
    }

    @Override
    public boolean activateServerTLSAuth() {
        String value = get(PROP_ML_DISTRIBUTION_ACTIVE_SERVER_TLS_AUTH);
        return Boolean.parseBoolean(value);
    }

    @Override
    public String getSdcAddress() {
        return get(PROP_ML_DISTRIBUTION_ASDC_ADDRESS);
    }

    @Override
    public Boolean isUseHttpsWithSDC() {
        /* if PROP_ML_DISTRIBUTION_ASDC_USE_HTTPS is null, https will be used, as before */
        String value = get(PROP_ML_DISTRIBUTION_ASDC_USE_HTTPS);
        if (value == null) {
          return true;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public String getConsumerGroup() {
        return get(PROP_ML_DISTRIBUTION_CONSUMER_GROUP);
    }

    @Override
    public String getConsumerID() {
        return get(PROP_ML_DISTRIBUTION_CONSUMER_ID);
    }

    @Override
    public String getEnvironmentName() {
        return get(PROP_ML_DISTRIBUTION_ENVIRONMENT_NAME);
    }

    @Override
    public String getKeyStorePassword() {
        return getDeobfuscatedValue(get(PROP_ML_DISTRIBUTION_KEYSTORE_PASSWORD));
    }

    @Override
    public String getKeyStorePath() {
        return certLocation + get(PROP_ML_DISTRIBUTION_KEYSTORE_FILE);
    }

    @Override
    public String getPassword() {
        return getDeobfuscatedValue(get(PROP_ML_DISTRIBUTION_PASSWORD));
    }

    @Override
    public int getPollingInterval() {
        return Integer.parseInt(get(PROP_ML_DISTRIBUTION_POLLING_INTERVAL));
    }

    @Override
    public int getPollingTimeout() {
        return Integer.parseInt(get(PROP_ML_DISTRIBUTION_POLLING_TIMEOUT));
    }

    @Override
    public List<String> getRelevantArtifactTypes() {
        return artifactTypes;
    }

    @Override
    public String getUser() {
        return get(PROP_ML_DISTRIBUTION_USER);
    }

    @Override
    public boolean isFilterInEmptyResources() {
        return false;
    }

    @Override
    public String getHttpProxyHost() {
        return getPropertyOrNull(PROP_ML_DISTRIBUTION_HTTP_PROXY_HOST);
    }

    @Override
    public int getHttpProxyPort() {
        return getIntegerPropertyOrZero(PROP_ML_DISTRIBUTION_HTTP_PROXY_PORT);
    }

    @Override
    public String getHttpsProxyHost() {
        return getPropertyOrNull(PROP_ML_DISTRIBUTION_HTTPS_PROXY_HOST);
    }

    @Override
    public int getHttpsProxyPort() {
        return getIntegerPropertyOrZero(PROP_ML_DISTRIBUTION_HTTPS_PROXY_PORT);
    }


    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    /**
     * @return a boolean value indicating whether the simulator is enabled.
     */
    public boolean getIngestSimulatorEnabled() {
        String propValue = get(PROP_DEBUG_INGEST_SIMULATOR);
        return propValue != null && "enabled".equalsIgnoreCase(propValue);
    }

    /**
     * @return a boolean value indicating whether model loader is connected to ASDC.
     */
    public boolean getASDCConnectionDisabled() {
        String propValue = get(PROP_ML_DISTRIBUTION_ASDC_CONNECTION_DISABLED);
        return propValue != null && "true".equalsIgnoreCase(propValue);
    }

    private String getDeobfuscatedValue(String property) {
        if (property != null && property.startsWith("OBF:")) {
            return Password.deobfuscate(property);
        }
        return property;
    }

    private String get(String key) {
        String value = modelLoaderProperties.getProperty(key);

        if (value != null && value.startsWith("ENV:")) {
            value = System.getenv(StringUtils.removeStart(value, "ENV:"));
        }
        return value;
    }

    public String getPropertyOrNull(String propertyName) {
        String value = modelLoaderProperties.getProperty(propertyName);
        if (value == null || "NULL".equals(value) || value.isEmpty()) {
            return null;
        } else {
            return value;
        }
    }

    public int getIntegerPropertyOrZero(String propertyName) {
        String property = modelLoaderProperties.getProperty(propertyName);
        if (property == null || "NULL".equals(property) || property.isEmpty()) {
            return 0;
        } else {
            try {
                return Integer.parseInt(property);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    @Override
    public String getKafkaSaslJaasConfig() {
        String saslJaasConfFromEnv = System.getenv("SASL_JAAS_CONFIG");
        if(saslJaasConfFromEnv != null) {
            return saslJaasConfFromEnv;
        }
        if(get(PROP_ML_DISTRIBUTION_SASL_JAAS_CONFIG) != null) {
            return get(PROP_ML_DISTRIBUTION_SASL_JAAS_CONFIG);
        }
        return null;
    }

    @Override
    public String getKafkaSaslMechanism() {
        if(get(PROP_ML_DISTRIBUTION_SASL_MECHANISM) != null) {
            return get(PROP_ML_DISTRIBUTION_SASL_MECHANISM);
        }
        return System.getenv().getOrDefault("SASL_MECHANISM", "SCRAM-SHA-512");
    }

    /**
     * One of PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL
     */
    @Override
    public String getKafkaSecurityProtocolConfig() {
        if(get(PROP_ML_DISTRIBUTION_SECURITY_PROTOCOL) != null) {
            return get(PROP_ML_DISTRIBUTION_SECURITY_PROTOCOL);
        }
        return System.getenv().getOrDefault("SECURITY_PROTOCOL", "SASL_PLAINTEXT");
    }

}
