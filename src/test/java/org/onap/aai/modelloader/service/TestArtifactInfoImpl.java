/**
 * ﻿============LICENSE_START=======================================================
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
package org.onap.aai.modelloader.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for NotificationDataImpl class
 *
 */
public class TestArtifactInfoImpl {

    @Test
    public void testGettersAndSetters() {
        ArtifactInfoImpl info = new ArtifactInfoImpl();
        String artifactName = "testname";
        String artifactType = "test-type";
        String artifactVersion = "v1";
        String artifactDescription = "test description";

        info.setArtifactName(artifactName);
        assertThat(info.getArtifactName(), is(equalTo(artifactName)));

        info.setArtifactType(artifactType);
        assertThat(info.getArtifactType(), is(equalTo(artifactType)));

        info.setArtifactVersion(artifactVersion);
        assertThat(info.getArtifactVersion(), is(equalTo(artifactVersion)));

        info.setArtifactDescription(artifactDescription);
        assertThat(info.getArtifactDescription(), is(equalTo(artifactDescription)));

        assertThat(info.getArtifactChecksum(), is(nullValue()));
        assertThat(info.getArtifactTimeout(), is(nullValue()));
        assertThat(info.getArtifactURL(), is(nullValue()));
        assertThat(info.getArtifactUUID(), is(nullValue()));
        assertThat(info.getGeneratedArtifact(), is(nullValue()));
        assertThat(info.getRelatedArtifacts(), is(empty()));
    }


    @Test
    public void testEquality() {
        ArtifactInfoImpl info = new ArtifactInfoImpl();
        assertThat(info, is(not(equalTo(null))));
        assertThat(info, is(not(equalTo("")))); // NOSONAR
        assertThat(info, is(equalTo(info)));

        ArtifactInfoImpl other = new ArtifactInfoImpl();
        assertThat(info, is(equalTo(other)));
        assertThat(info.hashCode(), is(equalTo(other.hashCode())));

        // Artifact Name
        other.setArtifactName("");
        assertThat(info, is(not(equalTo(other))));

        info.setArtifactName("1234");
        assertThat(info, is(not(equalTo(other))));

        other.setArtifactName("1234");
        assertThat(info, is(equalTo(other)));
        assertThat(info.hashCode(), is(equalTo(other.hashCode())));

        // Artifact Type
        other.setArtifactType("");
        assertThat(info, is(not(equalTo(other))));

        info.setArtifactType("type");
        assertThat(info, is(not(equalTo(other))));

        other.setArtifactType("type");
        assertThat(info, is(equalTo(other)));
        assertThat(info.hashCode(), is(equalTo(other.hashCode())));

        // Artifact Description
        other.setArtifactDescription("");
        assertThat(info, is(not(equalTo(other))));

        info.setArtifactDescription("type");
        assertThat(info, is(not(equalTo(other))));

        other.setArtifactDescription("type");
        assertThat(info, is(equalTo(other)));
        assertThat(info.hashCode(), is(equalTo(other.hashCode())));

        // Artifact Version
        other.setArtifactVersion("");
        assertThat(info, is(not(equalTo(other))));

        info.setArtifactVersion("v1");
        assertThat(info, is(not(equalTo(other))));

        other.setArtifactVersion("v1");
        assertThat(info, is(equalTo(other)));
        assertThat(info.hashCode(), is(equalTo(other.hashCode())));
    }

}
