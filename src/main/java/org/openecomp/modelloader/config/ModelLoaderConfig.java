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

import org.eclipse.jetty.util.security.Password;
import org.openecomp.sdc.api.consumer.IConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ModelLoaderConfig implements IConfiguration {
	
  // Configuration file structure
  public static final String PREFIX_MODEL_LOADER_CONFIG = "ml";
  public static final String PREFIX_DISTRIBUTION_CLIENT = 
      PREFIX_MODEL_LOADER_CONFIG + ".distribution.";
  public static final String PREFIX_AAI = PREFIX_MODEL_LOADER_CONFIG + ".aai.";
  public static final String PREFIX_DEBUG = PREFIX_MODEL_LOADER_CONFIG + ".debug.";

  // Configuration file properties
  protected static final String PROP_ML_DISTRIBUTION_ACTIVE_SERVER_TLS_AUTH = 
      PREFIX_DISTRIBUTION_CLIENT + "ACTIVE_SERVER_TLS_AUTH";
  protected static final String PROP_ML_DISTRIBUTION_ASDC_ADDRESS = PREFIX_DISTRIBUTION_CLIENT
      + "ASDC_ADDRESS";
  protected static final String PROP_ML_DISTRIBUTION_CONSUMER_GROUP = PREFIX_DISTRIBUTION_CLIENT
      + "CONSUMER_GROUP";
  protected static final String PROP_ML_DISTRIBUTION_CONSUMER_ID = PREFIX_DISTRIBUTION_CLIENT
      + "CONSUMER_ID";
  protected static final String PROP_ML_DISTRIBUTION_ENVIRONMENT_NAME = PREFIX_DISTRIBUTION_CLIENT
      + "ENVIRONMENT_NAME";
  protected static final String PROP_ML_DISTRIBUTION_KEYSTORE_PASSWORD = PREFIX_DISTRIBUTION_CLIENT
      + "KEYSTORE_PASSWORD";
  protected static final String PROP_ML_DISTRIBUTION_KEYSTORE_FILE = PREFIX_DISTRIBUTION_CLIENT
      + "KEYSTORE_FILE";
  protected static final String PROP_ML_DISTRIBUTION_PASSWORD = PREFIX_DISTRIBUTION_CLIENT
      + "PASSWORD";
  protected static final String PROP_ML_DISTRIBUTION_POLLING_INTERVAL = PREFIX_DISTRIBUTION_CLIENT
      + "POLLING_INTERVAL";
  protected static final String PROP_ML_DISTRIBUTION_POLLING_TIMEOUT = PREFIX_DISTRIBUTION_CLIENT
      + "POLLING_TIMEOUT";
  protected static final String PROP_ML_DISTRIBUTION_USER = PREFIX_DISTRIBUTION_CLIENT + "USER";
  protected static final String PROP_ML_DISTRIBUTION_ARTIFACT_TYPES = PREFIX_DISTRIBUTION_CLIENT
      + "ARTIFACT_TYPES";
  protected static final String PROP_ML_DISTRIBUTION_HTTPS_WITH_DMAAP =
          PREFIX_DISTRIBUTION_CLIENT + "USE_HTTPS_WITH_DMAAP";

  protected static final String PROP_AAI_BASE_URL = PREFIX_AAI + "BASE_URL";
  protected static final String PROP_AAI_KEYSTORE_FILE = PREFIX_AAI + "KEYSTORE_FILE";
  protected static final String PROP_AAI_KEYSTORE_PASSWORD = PREFIX_AAI + "KEYSTORE_PASSWORD";
  protected static final String PROP_AAI_MODEL_RESOURCE_URL = PREFIX_AAI + "MODEL_URL";
  protected static final String PROP_AAI_NAMED_QUERY_RESOURCE_URL = PREFIX_AAI + "NAMED_QUERY_URL";
  protected static final String PROP_AAI_VNF_IMAGE_RESOURCE_URL = PREFIX_AAI + "VNF_IMAGE_URL";
  protected static final String PROP_AAI_AUTHENTICATION_USER = PREFIX_AAI + "AUTH_USER";
  protected static final String PROP_AAI_AUTHENTICATION_PASSWORD = PREFIX_AAI + "AUTH_PASSWORD";

  protected static final String PROP_DEBUG_INGEST_SIMULATOR = PREFIX_DEBUG + "INGEST_SIMULATOR";

  private Properties modelLoaderProperties = null;

  private String certLocation = ".";

  private List<String> artifactTypes = null;

  /**
   * This is the class constructor.
   * 
   * @param modelLoaderProperties properties needed to be configured for the model loader
   * @param certLocation location of the certificate
   */
  public ModelLoaderConfig(Properties modelLoaderProperties, String certLocation) {
    this.modelLoaderProperties = modelLoaderProperties;
    this.certLocation = certLocation;

    // Get list of artifacts
    artifactTypes = new ArrayList<String>();
    if (modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_ARTIFACT_TYPES) != null) {
      String[] artTypeList = modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_ARTIFACT_TYPES)
          .split(",");
      for (String artType : artTypeList) {
        artifactTypes.add(artType);
      }
    }
  }

  @Override
  public boolean activateServerTLSAuth() {
    String value = modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_ACTIVE_SERVER_TLS_AUTH);
    return value == null ? false : Boolean.parseBoolean(value);
  }

  @Override
  public String getAsdcAddress() {
    return modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_ASDC_ADDRESS);
  }

  @Override
  public String getConsumerGroup() {
    return modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_CONSUMER_GROUP);
  }

  @Override
  public String getConsumerID() {
    return modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_CONSUMER_ID);
  }

  @Override
  public String getEnvironmentName() {
    return modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_ENVIRONMENT_NAME);
  }

  @Override
  public String getKeyStorePassword() {
    return Password
        .deobfuscate(modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_KEYSTORE_PASSWORD));
  }

  @Override
  public String getKeyStorePath() {
    return certLocation + modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_KEYSTORE_FILE);
  }

  @Override
  public String getPassword() {
    return Password.deobfuscate(modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_PASSWORD));
  }

  @Override
  public int getPollingInterval() {
    return Integer
        .parseInt(modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_POLLING_INTERVAL));
  }

  @Override
  public int getPollingTimeout() {
    return Integer
        .parseInt(modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_POLLING_TIMEOUT));
  }

  @Override
  public List<String> getRelevantArtifactTypes() {
    return artifactTypes;
  }

  @Override
  public String getUser() {
    return modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_USER);
  }

  @Override
  public boolean isFilterInEmptyResources() {
      return false;
  }

  @Override
  public Boolean isUseHttpsWithDmaap() {
      String useHTTPS = modelLoaderProperties.getProperty(PROP_ML_DISTRIBUTION_HTTPS_WITH_DMAAP);
      return useHTTPS == null ? false : Boolean.valueOf(useHTTPS);
  }
  
  public String getAaiKeyStorePath() {
    return certLocation + "/" + modelLoaderProperties.getProperty(PROP_AAI_KEYSTORE_FILE);
  }

  public String getAaiKeyStorePassword() {
    return Password.deobfuscate(modelLoaderProperties.getProperty(PROP_AAI_KEYSTORE_PASSWORD));
  }

  public String getAaiBaseUrl() {
    return modelLoaderProperties.getProperty(PROP_AAI_BASE_URL);
  }

  public String getAaiModelUrl(String version) {
    return modelLoaderProperties.getProperty(PROP_AAI_MODEL_RESOURCE_URL).replace("v*", version);
  }

  public String getAaiNamedQueryUrl(String version) {
    return modelLoaderProperties.getProperty(PROP_AAI_NAMED_QUERY_RESOURCE_URL).replace("v*", version);
  }

  public String getAaiVnfImageUrl() {
    return modelLoaderProperties.getProperty(PROP_AAI_VNF_IMAGE_RESOURCE_URL);
  }

  public String getAaiAuthenticationUser() {
    return modelLoaderProperties.getProperty(PROP_AAI_AUTHENTICATION_USER);
  }

  /**
   * @return password for AAI authentication that has been reverse-engineered
   *         from its obfuscated form.
   */
  public String getAaiAuthenticationPassword() {
    String password = Password
        .deobfuscate(modelLoaderProperties.getProperty(PROP_AAI_AUTHENTICATION_PASSWORD));

    if ((password != null) && (password.equals(""))) {
      return null;
    }

    return password;
  }

  /**
   * @return a boolean value indicating whether the simulator is enabled.
   */
  public boolean getIngestSimulatorEnabled() {
    String propValue = modelLoaderProperties.getProperty(PROP_DEBUG_INGEST_SIMULATOR);

    if (propValue == null) {
      return false;
    }

    if (propValue.compareToIgnoreCase("enabled") == 0) {
      return true;
    }

    return false;
  }
  
}
