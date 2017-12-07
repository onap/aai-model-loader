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
package org.onap.aai.modelloader.notification;

import org.openecomp.sdc.api.consumer.IConfiguration;

import java.util.ArrayList;
import java.util.List;

public class TestConfiguration implements IConfiguration{


    private String asdcAddress;
    private String user;
    private String password;
    private int pollingInterval = 15;
    private int pollingTimeout = 15;
    private List<String> relevantArtifactTypes;
    private String consumerGroup;
    private String environmentName;
    private String comsumerID;
    private String keyStorePath;
    private String keyStorePassword;
    private boolean activateServerTLSAuth;
    private boolean isFilterInEmptyResources;
    private boolean useHttpsWithDmaap;

    public TestConfiguration(IConfiguration other) {
        this.asdcAddress = other.getAsdcAddress();
        this.comsumerID = other.getConsumerID();
        this.consumerGroup = other.getConsumerGroup();
        this.environmentName = other.getEnvironmentName();
        this.password = other.getPassword();
        this.pollingInterval = other.getPollingInterval();
        this.pollingTimeout = other.getPollingTimeout();
        this.relevantArtifactTypes = other.getRelevantArtifactTypes();
        this.user = other.getUser();
        this.keyStorePath = other.getKeyStorePath();
        this.keyStorePassword = other.getKeyStorePassword();
        this.activateServerTLSAuth = other.activateServerTLSAuth();
        this.isFilterInEmptyResources = other.isFilterInEmptyResources();
    }

    public TestConfiguration() {
        this.asdcAddress = "localhost:8443";
        this.comsumerID = "mso-123456";
        this.consumerGroup = "mso-group";
        this.environmentName = "PROD";
        this.password = "password";
        this.pollingInterval = 20;
        this.pollingTimeout = 20;
        this.relevantArtifactTypes = new ArrayList<String>();
        this.relevantArtifactTypes.add("HEAT");
        this.user = "mso-user";
        this.keyStorePath = "etc/asdc-client.jks";
        this.keyStorePassword = "Aa123456";
        this.activateServerTLSAuth = false;
        this.isFilterInEmptyResources = false;
    }

    @Override
    public String getAsdcAddress() {
        return asdcAddress;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public int getPollingInterval() {
        return pollingInterval;
    }

    @Override
    public int getPollingTimeout() {
        return pollingTimeout;
    }

    @Override
    public List<String> getRelevantArtifactTypes() {
        return relevantArtifactTypes;
    }

    @Override
    public String getConsumerGroup() {
        return consumerGroup;
    }

    @Override
    public String getEnvironmentName() {
        return environmentName;
    }

    @Override
    public String getConsumerID() {
        return comsumerID;
    }

    @Override
    public String getKeyStorePath() {
        return keyStorePath;
    }

    @Override
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getComsumerID() {
        return comsumerID;
    }

    public void setComsumerID(String comsumerID) {
        this.comsumerID = comsumerID;
    }

    public void setAsdcAddress(String asdcAddress) {
        this.asdcAddress = asdcAddress;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPollingInterval(int pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public void setPollingTimeout(int pollingTimeout) {
        this.pollingTimeout = pollingTimeout;
    }

    public void setRelevantArtifactTypes(List<String> relevantArtifactTypes) {
        this.relevantArtifactTypes = relevantArtifactTypes;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((asdcAddress == null) ? 0 : asdcAddress.hashCode());
        result = prime * result + ((comsumerID == null) ? 0 : comsumerID.hashCode());
        result = prime * result + ((consumerGroup == null) ? 0 : consumerGroup.hashCode());
        result = prime * result + ((environmentName == null) ? 0 : environmentName.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + pollingInterval;
        result = prime * result + pollingTimeout;
        result = prime * result + ((relevantArtifactTypes == null) ? 0 : relevantArtifactTypes.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean activateServerTLSAuth() {

        return activateServerTLSAuth;
    }

    public void setactivateServerTLSAuth(boolean activateServerTLSAuth) {
        this.activateServerTLSAuth = activateServerTLSAuth;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestConfiguration other = (TestConfiguration) obj;
        if (asdcAddress == null) {
            if (other.asdcAddress != null)
                return false;
        } else if (!asdcAddress.equals(other.asdcAddress))
            return false;
        if (comsumerID == null) {
            if (other.comsumerID != null)
                return false;
        } else if (!comsumerID.equals(other.comsumerID))
            return false;
        if (consumerGroup == null) {
            if (other.consumerGroup != null)
                return false;
        } else if (!consumerGroup.equals(other.consumerGroup))
            return false;
        if (environmentName == null) {
            if (other.environmentName != null)
                return false;
        } else if (!environmentName.equals(other.environmentName))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (pollingInterval != other.pollingInterval)
            return false;
        if (pollingTimeout != other.pollingTimeout)
            return false;
        if (relevantArtifactTypes == null) {
            if (other.relevantArtifactTypes != null)
                return false;
        } else if (!relevantArtifactTypes.equals(other.relevantArtifactTypes))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (keyStorePath == null) {
            if (other.keyStorePath != null)
                return false;
        } else if (!keyStorePath.equals(other.keyStorePath))
            return false;
        if (keyStorePassword == null) {
            if (other.keyStorePassword != null)
                return false;
        } else if (!keyStorePassword.equals(other.keyStorePassword))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "TestConfiguration [asdcAddress=" + asdcAddress + ", user=" + user + ", password=" + password + ", pollingInterval=" + pollingInterval + ", pollingTimeout=" + pollingTimeout + ", relevantArtifactTypes=" + relevantArtifactTypes
                + ", consumerGroup=" + consumerGroup + ", environmentName=" + environmentName + ", comsumerID=" + comsumerID + "]";
    }

    @Override
    public boolean isFilterInEmptyResources() {
        return isFilterInEmptyResources;
    }


    public void setFilterInEmptyResources(boolean isFilterInEmptyResources) {
        this.isFilterInEmptyResources = isFilterInEmptyResources;
    }

    @Override
    public Boolean isUseHttpsWithDmaap() {
        return this.useHttpsWithDmaap;
    }
}
