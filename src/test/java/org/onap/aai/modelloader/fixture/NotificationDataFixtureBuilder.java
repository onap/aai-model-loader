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
package org.onap.aai.modelloader.fixture;

import java.util.ArrayList;
import java.util.List;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;

/**
 * This class is responsible for building NotificationData for use in test classes.
 */
public class NotificationDataFixtureBuilder {

    private static final String DESCRIPTION_OF_RESOURCE = "description of resource";
    private static final MockNotificationDataImpl EMPTY_NOTIFICATION_DATA = new MockNotificationDataImpl();
    private static final String MODEL_QUERY_SPEC = "MODEL_QUERY_SPEC";
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_CATALOG_FILE = new MockNotificationDataImpl();
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_MODEL_QUERY_SPEC =
            new MockNotificationDataImpl();
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_INVALID_TYPE = new MockNotificationDataImpl();
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_ONE_OF_EACH = new MockNotificationDataImpl();
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_ONE_RESOURCE = new MockNotificationDataImpl();
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_ONE_SERVICE = new MockNotificationDataImpl();
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_ONE_SERVICE_AND_RESOURCES =
            new MockNotificationDataImpl();
    private static final MockNotificationDataImpl NOTIFICATION_DATA_WITH_TOSCA_CSAR_FILE =
            new MockNotificationDataImpl();
    private static final String RESOURCE = "resource";
    private static final String TOSCA_CSAR = "TOSCA_CSAR";
    private static final String INVALID_TYPE = "INVALID_TYPE";
    private static final String VERSION = "r1.0";

    static {
        buildEmptyNotificationData();
        buildWithCatalogFile();
        buildWithModelQuerySpec();
        buildwithOneOfEach();
        buildWithOneResource();
        buildWithOneService();
        buildWithOneServiceAndResources();
        buildWithToscaCsarFile();
        buildWithInvalidType();
    }

    private static void buildEmptyNotificationData() {
        EMPTY_NOTIFICATION_DATA.setResources(new ArrayList<>());
        EMPTY_NOTIFICATION_DATA.setServiceArtifacts(new ArrayList<>());
    }

    private static void buildWithCatalogFile() {
        buildService(TOSCA_CSAR, NOTIFICATION_DATA_WITH_CATALOG_FILE);
    }

    private static void buildWithOneResource() {
        List<IResourceInstance> resources = new ArrayList<>();
        List<IArtifactInfo> artifacts =
                ArtifactInfoBuilder.buildArtifacts(new String[][] {{"R", RESOURCE, DESCRIPTION_OF_RESOURCE, VERSION}});
        resources.add(ResourceInstanceBuilder.build(artifacts));
        NOTIFICATION_DATA_WITH_ONE_RESOURCE.setResources(resources);
    }

    private static void buildWithModelQuerySpec() {
        buildService(MODEL_QUERY_SPEC, NOTIFICATION_DATA_WITH_MODEL_QUERY_SPEC);
    }

    private static void buildWithInvalidType() {
        buildService(INVALID_TYPE, NOTIFICATION_DATA_WITH_INVALID_TYPE);
    }

    private static void buildwithOneOfEach() {
        buildService(TOSCA_CSAR, NOTIFICATION_DATA_WITH_ONE_OF_EACH);

        List<IResourceInstance> resources = new ArrayList<>();
        List<IArtifactInfo> artifacts = ArtifactInfoBuilder
                .buildArtifacts(new String[][] {{TOSCA_CSAR, RESOURCE, "description of resource", VERSION}});
        resources.add(ResourceInstanceBuilder.build(artifacts));

        artifacts = ArtifactInfoBuilder
                .buildArtifacts(new String[][] {{MODEL_QUERY_SPEC, "resource2", "description of resource2", VERSION}});
        resources.add(ResourceInstanceBuilder.build(artifacts));
        NOTIFICATION_DATA_WITH_ONE_OF_EACH.setResources(resources);
    }

    private static void buildWithOneService() {
        buildService("S", NOTIFICATION_DATA_WITH_ONE_SERVICE);
    }

    private static void buildService(String type, MockNotificationDataImpl data) {
        List<IArtifactInfo> artifacts = new ArrayList<>();
        artifacts.add(ArtifactInfoBuilder.build(type, "service", "description of service", "s1.0"));
        data.setDistributionId("ID");
        data.setServiceArtifacts(artifacts);
    }

    private static void buildWithOneServiceAndResources() {
        buildService(TOSCA_CSAR, NOTIFICATION_DATA_WITH_ONE_SERVICE_AND_RESOURCES);

        List<IResourceInstance> resources = new ArrayList<>();
        List<IArtifactInfo> artifacts = ArtifactInfoBuilder
                .buildArtifacts(new String[][] {{TOSCA_CSAR, RESOURCE, "description of resource", VERSION}});
        resources.add(ResourceInstanceBuilder.build(artifacts));
        NOTIFICATION_DATA_WITH_ONE_SERVICE_AND_RESOURCES.setResources(resources);
    }

    private static void buildWithToscaCsarFile() {
        buildService(TOSCA_CSAR, NOTIFICATION_DATA_WITH_TOSCA_CSAR_FILE);
    }

    public static INotificationData getEmptyNotificationData() {
        return EMPTY_NOTIFICATION_DATA;
    }

    public static INotificationData getNotificationDataWithCatalogFile() {
        return NOTIFICATION_DATA_WITH_CATALOG_FILE;
    }

    public static INotificationData getNotificationDataWithModelQuerySpec() {
        return NOTIFICATION_DATA_WITH_MODEL_QUERY_SPEC;
    }

    public static INotificationData getNotificationDataWithInvalidType() {
        return NOTIFICATION_DATA_WITH_INVALID_TYPE;
    }

    public static INotificationData getNotificationDataWithOneOfEach() {
        return NOTIFICATION_DATA_WITH_ONE_OF_EACH;
    }

    public static INotificationData getNotificationDataWithOneResource() {
        return NOTIFICATION_DATA_WITH_ONE_RESOURCE;
    }

    public static INotificationData getNotificationDataWithOneService() {
        return NOTIFICATION_DATA_WITH_ONE_SERVICE;
    }

    public static INotificationData getNotificationDataWithOneServiceAndResources() {
        return NOTIFICATION_DATA_WITH_ONE_SERVICE_AND_RESOURCES;
    }

    public static INotificationData getNotificationDataWithToscaCsarFile() {
        return NOTIFICATION_DATA_WITH_TOSCA_CSAR_FILE;
    }
}
