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
package org.onap.aai.modelloader.notification;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests for NotificationDataImpl class
 *
 */
public class TestNotificationDataImpl {

    @Test
    public void testGettersAndSetters() {
        NotificationDataImpl data = new NotificationDataImpl();
        String distributionId = "testid";

        data.setDistributionID(distributionId);
        assertThat(data.getDistributionID(), is(equalTo(distributionId)));

        // Getters return empty data
        assertThat(data.getArtifactMetadataByUUID(null), is(equalTo(null)));
        assertThat(data.getServiceDescription(), is(equalTo(null)));
        assertThat(data.getServiceInvariantUUID(), is(equalTo(null)));
        assertThat(data.getServiceName(), is(equalTo(null)));
        assertThat(data.getServiceUUID(), is(equalTo(null)));
        assertThat(data.getServiceVersion(), is(equalTo(null)));
        assertThat(data.getResources().size(), is(0));
        assertThat(data.getServiceArtifacts().size(), is(0));

        // Unsupported method!
        String context = "testcontext";
        data.setWorkloadContext(context);
        assertThat(data.getWorkloadContext(), is(equalTo(null)));
    }


    @Test
    public void testEquality() {
        NotificationDataImpl data = new NotificationDataImpl();
        assertThat(data, is(not(equalTo(null))));
        assertThat(data, is(not(equalTo("")))); // NOSONAR
        assertThat(data, is(equalTo(data)));

        NotificationDataImpl other = new NotificationDataImpl();
        assertThat(data, is(equalTo(other)));
        assertThat(data.hashCode(), is(equalTo(other.hashCode())));

        other.setDistributionID("");
        assertThat(data, is(not(equalTo(other))));

        data.setDistributionID("1234");
        assertThat(data, is(not(equalTo(other))));

        other.setDistributionID("1234");
        assertThat(data, is(equalTo(other)));
        assertThat(data.hashCode(), is(equalTo(other.hashCode())));
    }

}
