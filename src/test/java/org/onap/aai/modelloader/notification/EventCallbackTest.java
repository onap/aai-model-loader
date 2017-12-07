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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.consumer.INotificationCallback;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.INotificationData;
import org.openecomp.sdc.api.notification.IResourceInstance;
import org.openecomp.sdc.api.results.IDistributionClientDownloadResult;
import org.openecomp.sdc.api.results.IDistributionClientResult;
import org.openecomp.sdc.impl.DistributionClientFactory;
import org.openecomp.sdc.impl.DistributionClientImpl;
import org.openecomp.sdc.utils.DistributionActionResultEnum;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EventCallbackTest {

    ModelLoaderConfig config;
    DistributionClientImpl client;
    EventCallback callBack;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        Properties props = new Properties();
        props.setProperty("ml.distribution.ARTIFACT_TYPES",
                "MODEL_INVENTORY_PROFILE,MODEL_QUERY_SPEC,VNF_CATALOG");
        config = new ModelLoaderConfig(props, null);
        client = Mockito.spy(DistributionClientImpl.class);
        callBack = new EventCallback(client, config);
    }

    @Test
    public void testActivateCallBack_PublishFailure(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        NotificationDataInstance notification = gson.fromJson(getNotificationWithMultipleResources(),
                NotificationDataInstance.class);

        TestConfiguration testConfig = new TestConfiguration();
        Mockito.when(client.getConfiguration()).thenReturn(testConfig);
        callBack.activateCallback(notification);
    }

    @Test
    public void testActivateCallBack_PublishSuccess(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        NotificationDataInstance notification = gson.fromJson(getNotificationWithMultipleResources(),
                NotificationDataInstance.class);

        TestConfiguration testConfig = new TestConfiguration();
        Mockito.when(client.download(Mockito.any(IArtifactInfo.class))).thenReturn(buildSuccessResult());
        Mockito.when(client.getConfiguration()).thenReturn(testConfig);
        callBack.activateCallback(notification);
    }

    private static IDistributionClientDownloadResult buildSuccessResult() {
        return new IDistributionClientDownloadResult() {

            @Override
            public byte[] getArtifactPayload() {
                return new byte[0];
            }

            @Override
            public String getArtifactName() {
                return "";
            }

            @Override
            public String getArtifactFilename() {
                return "";
            }

            @Override
            public String getDistributionMessageResult() {
                return "";
            }

            @Override
            public DistributionActionResultEnum getDistributionActionResult() {
                return DistributionActionResultEnum.SUCCESS;
            }
        };
    }

    private String getNotificationWithMultipleResources(){
        return "{\"distributionID\" : \"bcc7a72e-90b1-4c5f-9a37-28dc3cd86416\",\r\n" +
                "	\"serviceName\" : \"Testnotificationser1\",\r\n" +
                "	\"serviceVersion\" : \"1.0\",\r\n" +
                "	\"serviceUUID\" : \"7f7f94f4-373a-4b71-a0e3-80ae2ba4eb5d\",\r\n" +
                "	\"serviceDescription\" : \"TestNotificationVF1\",\r\n" +
                "	\"resources\" : [{\r\n" +
                "			\"resourceInstanceName\" : \"testnotificationvf11\",\r\n" +
                "			\"resourceName\" : \"TestNotificationVF1\",\r\n" +
                "			\"resourceVersion\" : \"1.0\",\r\n" +
                "			\"resoucreType\" : \"VF\",\r\n" +
                "			\"resourceUUID\" : \"907e1746-9f69-40f5-9f2a-313654092a2d\",\r\n" +
                "			\"artifacts\" : [{\r\n" +
                "					\"artifactName\" : \"sample-xml-alldata-1-1.xml\",\r\n" +
                "					\"artifactType\" : \"YANG_XML\",\r\n" +
                "					\"artifactURL\" : \"/sdc/v1/catalog/services/Testnotificationser1/1.0/" +
                "                                       resourceInstances/testnotificationvf11/artifacts/" +
                "                                       sample-xml-alldata-1-1.xml\",\r\n" +
                "					\"artifactChecksum\" : \"MTUxODFkMmRlOTNhNjYxMGYyYTI1ZjA5Y2QyNWQyYTk\\u003d\",\r\n" +
                "					\"artifactDescription\" : \"MyYang\",\r\n" +
                "					\"artifactTimeout\" : 0,\r\n" +
                "					\"artifactUUID\" : \"0005bc4a-2c19-452e-be6d-d574a56be4d0\",\r\n" +
                "					\"artifactVersion\" : \"1\"\r\n" +
                "				}" +
                "			]\r\n" +
                "		},\r\n" +
                "       {\r\n" +
                "			\"resourceInstanceName\" : \"testnotificationvf12\",\r\n" +
                "			\"resourceName\" : \"TestNotificationVF1\",\r\n" +
                "			\"resourceVersion\" : \"1.0\",\r\n" +
                "			\"resoucreType\" : \"VF\",\r\n" +
                "			\"resourceUUID\" : \"907e1746-9f69-40f5-9f2a-313654092a2e\",\r\n" +
                "			\"artifacts\" : [{\r\n" +
                "					\"artifactName\" : \"heat.yaml\",\r\n" +
                "					\"artifactType\" : \"HEAT\",\r\n" +
                "					\"artifactURL\" : \"/sdc/v1/catalog/services/Testnotificationser1/1.0/" +
                "                                       resourceInstances/testnotificationvf11/artifacts/" +
                "                                       heat.yaml\",\r\n" +
                "					\"artifactChecksum\" : \"ODEyNjE4YTMzYzRmMTk2ODVhNTU2NTg3YWEyNmIxMTM\\u003d\",\r\n" +
                "					\"artifactDescription\" : \"heat\",\r\n" +
                "					\"artifactTimeout\" : 60,\r\n" +
                "					\"artifactUUID\" : \"8df6123c-f368-47d3-93be-1972cefbcc35\",\r\n" +
                "					\"artifactVersion\" : \"1\"\r\n" +
                "				}" +
                "			]\r\n" +
                "		}\r\n" +
                "	]}";
    }
}

class NotificationDataInstance implements INotificationData{

    private String distributionID;
    private String serviceName;
    private String serviceVersion;
    private String serviceUUID;
    private String serviceDescription;
    private String serviceInvariantUUID;
    private List<JsonContainerResourceInstance> resources;
    private List<ArtifactInfoImpl> serviceArtifacts;
    private String workloadContext;

    @Override
    public String getDistributionID() {
        return distributionID;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getServiceVersion() {
        return serviceVersion;
    }

    @Override
    public String getServiceUUID() {
        return serviceUUID;
    }

    public void setDistributionID(String distributionID) {
        this.distributionID = distributionID;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
    }



    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getWorkloadContext() {
        return workloadContext;
    }

    public void setWorkloadContext(String workloadContext) {
        this.workloadContext = workloadContext;
    }

    @Override
    public String toString() {
        return "NotificationDataImpl [distributionID=" + distributionID + ", serviceName=" + serviceName
                + ", serviceVersion=" + serviceVersion + ", serviceUUID=" + serviceUUID + ", serviceDescription="
                + serviceDescription + ", serviceInvariantUUID=" + serviceInvariantUUID + ", resources=" + resources
                + ", serviceArtifacts=" + serviceArtifacts + ", workloadContext=" + workloadContext + "]";
    }

    @Override
    public List<IResourceInstance> getResources() {
        List<IResourceInstance> ret = new ArrayList<IResourceInstance>();
        if( resources != null ){
            ret.addAll(resources);
        }
        return ret;
    }

    public void setResources(List<IResourceInstance> resources){
        this.resources = JsonContainerResourceInstance.convertToJsonContainer(resources);
    }

    public List<JsonContainerResourceInstance> getResourcesImpl(){
        return resources;
    }

    List<ArtifactInfoImpl> getServiceArtifactsImpl(){
        return serviceArtifacts;
    }

    @Override
    public List<IArtifactInfo> getServiceArtifacts() {

        List<IArtifactInfo> temp = new ArrayList<IArtifactInfo>();
        if( serviceArtifacts != null ){
            temp.addAll(serviceArtifacts);
        }
        return temp;
    }

    void setServiceArtifacts(List<ArtifactInfoImpl> relevantServiceArtifacts) {
        serviceArtifacts = relevantServiceArtifacts;

    }

    @Override
    public String getServiceInvariantUUID() {
        return serviceInvariantUUID;
    }


    public void setServiceInvariantUUID(String serviceInvariantUUID) {
        this.serviceInvariantUUID = serviceInvariantUUID;
    }
    @Override
    public IArtifactInfo getArtifactMetadataByUUID(String artifactUUID){
        IArtifactInfo ret = findArtifactInfoByUUID(artifactUUID, serviceArtifacts);
        if( ret == null && resources != null ){
            for( JsonContainerResourceInstance currResourceInstance : resources ){
                ret = findArtifactInfoByUUID(artifactUUID, currResourceInstance.getArtifactsImpl());
                if( ret != null ){
                    break;
                }
            }
        }
        return ret;

    }

    private IArtifactInfo findArtifactInfoByUUID(String artifactUUID, List<ArtifactInfoImpl> listToCheck) {
        IArtifactInfo ret = null;
        if( listToCheck != null ){
            for(IArtifactInfo curr: listToCheck ){
                if(curr.getArtifactUUID().equals(artifactUUID) ){
                    ret = curr;
                    break;
                }
            }
        }
        return ret;
    }
}

class ArtifactInfoImpl implements IArtifactInfo{

    private String artifactName;
    private String artifactType;
    private String artifactURL;
    private String artifactChecksum;
    private String artifactDescription;
    private Integer artifactTimeout;
    private String artifactVersion;
    private String artifactUUID;
    private String generatedFromUUID;
    private IArtifactInfo generatedArtifact;
    private List<String> relatedArtifacts;
    private List<IArtifactInfo> relatedArtifactsInfo;
    ArtifactInfoImpl(){}

    private ArtifactInfoImpl(IArtifactInfo iArtifactInfo){
        artifactName = iArtifactInfo.getArtifactName();
        artifactType = iArtifactInfo.getArtifactType();
        artifactURL = iArtifactInfo.getArtifactURL();
        artifactChecksum = iArtifactInfo.getArtifactChecksum();
        artifactDescription = iArtifactInfo.getArtifactDescription();
        artifactTimeout = iArtifactInfo.getArtifactTimeout();
        artifactVersion = iArtifactInfo.getArtifactVersion();
        artifactUUID = iArtifactInfo.getArtifactUUID();
        generatedArtifact = iArtifactInfo.getGeneratedArtifact();
        relatedArtifactsInfo = iArtifactInfo.getRelatedArtifacts();
        relatedArtifacts = fillRelatedArtifactsUUID(relatedArtifactsInfo);

    }


    private List<String> fillRelatedArtifactsUUID(List<IArtifactInfo> relatedArtifactsInfo) {
        List<String> relatedArtifactsUUID = null;
        if( relatedArtifactsInfo != null && !relatedArtifactsInfo.isEmpty()){
            relatedArtifactsUUID = new ArrayList<>();
            for(IArtifactInfo curr: relatedArtifactsInfo){
                relatedArtifactsUUID.add(curr.getArtifactUUID());
            }
        }
        return relatedArtifactsUUID;
    }

    public static List<ArtifactInfoImpl> convertToArtifactInfoImpl(List<IArtifactInfo> list){
        List<ArtifactInfoImpl> ret = new ArrayList<ArtifactInfoImpl>();
        if( list != null ){
            for(IArtifactInfo artifactInfo : list  ){
                ret.add(new ArtifactInfoImpl(artifactInfo));
            }
        }
        return ret;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getArtifactURL() {
        return artifactURL;
    }

    public void setArtifactURL(String artifactURL) {
        this.artifactURL = artifactURL;
    }

    public String getArtifactChecksum() {
        return artifactChecksum;
    }

    public void setArtifactChecksum(String artifactChecksum) {
        this.artifactChecksum = artifactChecksum;
    }

    public String getArtifactDescription() {
        return artifactDescription;
    }

    public void setArtifactDescription(String artifactDescription) {
        this.artifactDescription = artifactDescription;
    }

    public Integer getArtifactTimeout() {
        return artifactTimeout;
    }

    public void setArtifactTimeout(Integer artifactTimeout) {
        this.artifactTimeout = artifactTimeout;
    }

    @Override
    public String toString() {
        return "BaseArtifactInfoImpl [artifactName=" + artifactName
                + ", artifactType=" + artifactType + ", artifactURL="
                + artifactURL + ", artifactChecksum=" + artifactChecksum
                + ", artifactDescription=" + artifactDescription
                + ", artifactVersion=" + artifactVersion
                + ", artifactUUID=" + artifactUUID
                + ", artifactTimeout=" + artifactTimeout + "]";
    }

    public String getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(String artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getArtifactUUID() {
        return artifactUUID;
    }

    public void setArtifactUUID(String artifactUUID) {
        this.artifactUUID = artifactUUID;
    }

    public String getGeneratedFromUUID() {
        return generatedFromUUID;
    }

    public void setGeneratedFromUUID(String generatedFromUUID) {
        this.generatedFromUUID = generatedFromUUID;
    }

    public IArtifactInfo getGeneratedArtifact() {
        return generatedArtifact;
    }

    public void setGeneratedArtifact(IArtifactInfo generatedArtifact) {
        this.generatedArtifact = generatedArtifact;
    }

    public List<IArtifactInfo> getRelatedArtifacts(){
        List<IArtifactInfo> temp = new ArrayList<IArtifactInfo>();
        if( relatedArtifactsInfo != null ){
            temp.addAll(relatedArtifactsInfo);
        }
        return temp;
    }

    public void setRelatedArtifacts(List<String> relatedArtifacts) {
        this.relatedArtifacts = relatedArtifacts;
    }

    public void setRelatedArtifactsInfo(List<IArtifactInfo> relatedArtifactsInfo) {
        this.relatedArtifactsInfo = relatedArtifactsInfo;
    }

    public List<String> getRelatedArtifactsUUID(){
        return relatedArtifacts;
    }
}

class JsonContainerResourceInstance implements IResourceInstance{
    JsonContainerResourceInstance (){}
    private String resourceInstanceName;
    private String resourceCustomizationUUID;
    private String resourceName;
    private String resourceVersion;
    private String resoucreType;
    private String resourceUUID;
    private String resourceInvariantUUID;
    private String category;
    private String subcategory;
    private List<ArtifactInfoImpl> artifacts;

    private JsonContainerResourceInstance(IResourceInstance resourceInstance){
        resourceInstanceName = resourceInstance.getResourceInstanceName();
        resourceCustomizationUUID = resourceInstance.getResourceCustomizationUUID();
        resourceName = resourceInstance.getResourceName();
        resourceVersion = resourceInstance.getResourceVersion();
        resoucreType = resourceInstance.getResourceType();
        resourceUUID = resourceInstance.getResourceUUID();
        resourceInvariantUUID = resourceInstance.getResourceInvariantUUID();
        category = resourceInstance.getCategory();
        subcategory = resourceInstance.getSubcategory();
        artifacts = ArtifactInfoImpl.convertToArtifactInfoImpl(resourceInstance.getArtifacts());
    }

    public static List<JsonContainerResourceInstance> convertToJsonContainer(List<IResourceInstance> resources){
        List<JsonContainerResourceInstance> buildResources = new ArrayList<JsonContainerResourceInstance>();
        if( resources != null ){
            for( IResourceInstance resourceInstance : resources ){
                buildResources.add(new JsonContainerResourceInstance(resourceInstance));
            }
        }
        return buildResources;
    }

    @Override
    public String getResourceInstanceName() {
        return resourceInstanceName;
    }

    public void setResourceInstanceName(String resourceInstanceName) {
        this.resourceInstanceName = resourceInstanceName;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    @Override
    public String getResourceType() {
        return resoucreType;
    }

    public void setResoucreType(String resoucreType) {
        this.resoucreType = resoucreType;
    }

    @Override
    public String getResourceUUID() {
        return resourceUUID;
    }

    public void setResourceUUID(String resourceUUID) {
        this.resourceUUID = resourceUUID;
    }

    @Override
    public List<IArtifactInfo> getArtifacts() {
        List<IArtifactInfo> temp = new ArrayList<IArtifactInfo>();
        if( artifacts != null ){
            temp.addAll(artifacts);
        }
        return temp;
    }

    public void setArtifacts(List<ArtifactInfoImpl> artifacts) {
        this.artifacts = artifacts;
    }

    public List<ArtifactInfoImpl> getArtifactsImpl(){
        return artifacts;
    }

    @Override
    public String getResourceInvariantUUID() {
        return resourceInvariantUUID;
    }

    public void setResourceInvariantUUID(String resourceInvariantUUID) {
        this.resourceInvariantUUID = resourceInvariantUUID;
    }
    public String getResourceCustomizationUUID() {
        return resourceCustomizationUUID;
    }

    public void setResourceCustomizationUUID(String resourceCustomizationUUID) {
        this.resourceCustomizationUUID = resourceCustomizationUUID;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }
}
