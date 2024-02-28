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
package org.onap.aai.modelloader.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getEmptyNotificationData;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneResource;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneService;
import static org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder.getNotificationDataWithOneServiceAndResources;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.fixture.ArtifactInfoBuilder;
import org.onap.aai.modelloader.fixture.MockNotificationDataImpl;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;

/**
 * Tests {@link ArtifactInfoExtractor}.
 */
public class TestArtifactInfoExtractor {

    private ArtifactInfoExtractor extractor;

    @BeforeEach
    public void setup() {
        extractor = new ArtifactInfoExtractor();
    }

    @AfterEach
    public void tearDown() {
        extractor = null;
    }

    @Test
    public void extract_nullDataSupplied() {
        doEmptyArtifactsTest(null);
    }

    private void doEmptyArtifactsTest(INotificationData notificationData) {
        assertTrue(extractor.extract(notificationData).isEmpty(), "The list returned should have been empty");
    }

    @Test
    public void extract_dataHasNullArtifacts() {
        doEmptyArtifactsTest(new MockNotificationDataImpl());
    }

    @Test
    public void extract_dataHasNoArtifacts() {
        doEmptyArtifactsTest(getEmptyNotificationData());
    }

    @Test
    public void dataHasOneServiceArtifact() {
        IArtifactInfo expected = ArtifactInfoBuilder.build("S", "service", "description of service", "s1.0");

        List<IArtifactInfo> artifacts = extractor.extract(getNotificationDataWithOneService());

        assertEquals(1, artifacts.size(), "One artifact should have been returned");
        assertEquals(expected, artifacts.get(0), "The actual artifact did not match the expected one");
    }

    @Test
    public void dataHasOneResourceArtifact() {
        List<IArtifactInfo> expectedArtifacts = new ArrayList<>();
        expectedArtifacts.add(ArtifactInfoBuilder.build("R", "resource", "description of resource", "r1.0"));

        List<IArtifactInfo> artifacts = extractor.extract(getNotificationDataWithOneResource());

        assertEquals(1, artifacts.size(), "One artifact should have been returned");
        assertEquals(expectedArtifacts, artifacts, "The actual artifact did not match the expected one");
    }

    @Test
    public void dataHasOneServiceAndTwoResources() {
        List<IArtifactInfo> expectedArtifacts = new ArrayList<>();
        expectedArtifacts.add(ArtifactInfoBuilder.build("TOSCA_CSAR", "service", "description of service", "s1.0"));
        expectedArtifacts.add(ArtifactInfoBuilder.build("TOSCA_CSAR", "resource", "description of resource", "r1.0"));

        List<IArtifactInfo> artifacts = extractor.extract(getNotificationDataWithOneServiceAndResources());

        assertEquals(2, artifacts.size(), "Two artifact should have been returned");
        assertEquals(expectedArtifacts, artifacts, "The actual artifact did not match the expected one");
    }
}
