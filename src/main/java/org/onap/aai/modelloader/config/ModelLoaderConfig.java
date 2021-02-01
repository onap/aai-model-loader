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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    public static final String PREFIX_AAI = PREFIX_MODEL_LOADER_CONFIG + ".aai.";
    public static final String PREFIX_BABEL = PREFIX_MODEL_LOADER_CONFIG + ".babel.";
    public static final String PREFIX_DEBUG = PREFIX_MODEL_LOADER_CONFIG + ".debug.";

    private static final String SUFFIX_KEYSTORE_FILE = "KEYSTORE_FILE";
    private static final String SUFFIX_KEYSTORE_PASS = "KEYSTORE_PASSWORD";

    private static final String SUFFIX_TRUSTSTORE_FILE = "TRUSTSTORE_FILE";
    private static final String SUFFIX_TRUSTSTORE_PASS = "TRUSTSTORE_PASSWORD";

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
    protected static final String PROP_ML_DISTRIBUTION_MSG_BUS_ADDRESSES =
            PREFIX_DISTRIBUTION_CLIENT + "MSG_BUS_ADDRESSES";
    protected static final String PROP_ML_DISTRIBUTION_HTTPS_WITH_DMAAP =
            PREFIX_DISTRIBUTION_CLIENT + "USE_HTTPS_WITH_DMAAP";

    protected static final String PROP_AAI_BASE_URL = PREFIX_AAI + "BASE_URL";
    protected static final String PROP_AAI_KEYSTORE_FILE = PREFIX_AAI + SUFFIX_KEYSTORE_FILE;
    protected static final String PROP_AAI_KEYSTORE_PASSWORD = PREFIX_AAI + SUFFIX_KEYSTORE_PASS;
    protected static final String PROP_AAI_MODEL_RESOURCE_URL = PREFIX_AAI + "MODEL_URL";
    protected static final String PROP_AAI_NAMED_QUERY_RESOURCE_URL = PREFIX_AAI + "NAMED_QUERY_URL";
    protected static final String PROP_AAI_VNF_IMAGE_RESOURCE_URL = PREFIX_AAI + "VNF_IMAGE_URL";
    protected static final String PROP_AAI_AUTHENTICATION_USER = PREFIX_AAI + "AUTH_USER";
    protected static final String PROP_AAI_AUTHENTICATION_PASSWORD = PREFIX_AAI + "AUTH_PASSWORD";
    protected static final String PROP_AAI_USE_GIZMO = PREFIX_AAI + "USE_GIZMO";

    protected static final String PROP_BABEL_BASE_URL = PREFIX_BABEL + "BASE_URL";
    protected static final String PROP_BABEL_KEYSTORE_FILE = PREFIX_BABEL + SUFFIX_KEYSTORE_FILE;
    protected static final String PROP_BABEL_KEYSTORE_PASSWORD = PREFIX_BABEL + SUFFIX_KEYSTORE_PASS;
    protected static final String PROP_BABEL_TRUSTSTORE_FILE = PREFIX_BABEL + SUFFIX_TRUSTSTORE_FILE;
    protected static final String PROP_BABEL_TRUSTSTORE_PASSWORD = PREFIX_BABEL + SUFFIX_TRUSTSTORE_PASS;
    protected static final String PROP_BABEL_GENERATE_RESOURCE_URL = PREFIX_BABEL + "GENERATE_ARTIFACTS_URL";

    protected static final String PROP_DEBUG_INGEST_SIMULATOR = PREFIX_DEBUG + "INGEST_SIMULATOR";
    protected static final String FILESEP =
            (System.getProperty("file.separator") == null) ? "/" : System.getProperty("file.separator");

    private static String configHome;
    private Properties modelLoaderProperties = null;
    private String certLocation = ".";
    private List<String> artifactTypes = new ArrayList<>();
    private List<String> msgBusAddrs = new ArrayList<>();
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
            for (String artType : types.split(",")) {
                artifactTypes.add(artType);
            }
        }

        // Get list of message bus addresses
        String addresses = get(PROP_ML_DISTRIBUTION_MSG_BUS_ADDRESSES);
        if (addresses != null) {
            for (String addr : addresses.split(",")) {
                msgBusAddrs.add(addr);
            }
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
        return value != null && Boolean.parseBoolean(value);
    }

    @Override
    public String getAsdcAddress() {
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
    public Boolean isUseHttpsWithDmaap() {
        String useHTTPS = get(PROP_ML_DISTRIBUTION_HTTPS_WITH_DMAAP);
        return useHTTPS != null && Boolean.valueOf(useHTTPS);
    }

    @Override
    public List<String> getMsgBusAddress() {
        return msgBusAddrs;
    }

    public String getAaiKeyStorePath() {
        return certLocation + File.separator + modelLoaderProperties.getProperty(PROP_AAI_KEYSTORE_FILE);
    }

    public String getBabelKeyStorePath() {
        String filename = get(PROP_BABEL_KEYSTORE_FILE);
        if (filename == null) {
            return null;
        } else {
            return certLocation + File.separator + filename;
        }
    }

    public String getAaiKeyStorePassword() {
        return getDeobfuscatedValue(get(PROP_AAI_KEYSTORE_PASSWORD));
    }

    public String getBabelKeyStorePassword() {
        return getDeobfuscatedValue(get(PROP_BABEL_KEYSTORE_PASSWORD));
    }

    public String getBabelTrustStorePath() {
        String filename = get(PROP_BABEL_TRUSTSTORE_FILE);
        if (filename == null) {
            return null;
        } else {
            return certLocation + File.separator + filename;
        }
    }

    public String getBabelTrustStorePassword() {
        return getDeobfuscatedValue(get(PROP_BABEL_TRUSTSTORE_PASSWORD));
    }

    public String getAaiBaseUrl() {
        return get(PROP_AAI_BASE_URL);
    }

    public String getBabelBaseUrl() {
        return get(PROP_BABEL_BASE_URL);
    }

    public String getBabelGenerateArtifactsUrl() {
        return get(PROP_BABEL_GENERATE_RESOURCE_URL);
    }

    public String getAaiModelUrl(String version) {
        setModelVersion(version);
        return updatePropertyOXMVersion(PROP_AAI_MODEL_RESOURCE_URL, version);
    }

    public String getAaiNamedQueryUrl(String version) {
        return updatePropertyOXMVersion(PROP_AAI_NAMED_QUERY_RESOURCE_URL, version);
    }

    public String getAaiVnfImageUrl() {
        return updatePropertyOXMVersion(PROP_AAI_VNF_IMAGE_RESOURCE_URL, getModelVersion());
    }

    public String getAaiAuthenticationUser() {
        return get(PROP_AAI_AUTHENTICATION_USER);
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public boolean useGizmo() {
        String useGizmo = get(PROP_AAI_USE_GIZMO);
        return useGizmo != null && useGizmo.equalsIgnoreCase("true");
    }

    /**
     * @return password for AAI authentication that has been reverse-engineered from its obfuscated form.
     */
    public String getAaiAuthenticationPassword() {
        String password = getDeobfuscatedValue(get(PROP_AAI_AUTHENTICATION_PASSWORD));

        if (password != null && password.isEmpty()) {
            password = null;
        }

        return password;
    }

    /**
     * @return a boolean value indicating whether the simulator is enabled.
     */
    public boolean getIngestSimulatorEnabled() {
        String propValue = get(PROP_DEBUG_INGEST_SIMULATOR);
        return propValue != null && "enabled".equalsIgnoreCase(propValue);
    }

    /**
     * Read the value of the property and replace any wildcard OXM version "v*" with the supplied default OXM version
     *
     * @param propertyName
     *            the name of the property storing the OXM version (possibly containing v*)
     * @param version
     *            the default OXM version
     * @return the String value of the defined property (with any wildcard OXM version defaulted)
     */
    private String updatePropertyOXMVersion(String propertyName, String version) {
        String value = get(propertyName);
        if (version != null && value != null) {
            value = value.replace("v*", version);
        }
        return value;
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
}
