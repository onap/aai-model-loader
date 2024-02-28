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
package org.onap.aai.modelloader.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Base64;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests for the ModelLoaderService class.
 *
 */
@SpringBootTest(classes = {ModelLoaderService.class, HttpsBabelServiceClientFactory.class})
@TestPropertySource(properties = {"CONFIG_HOME=src/test/resources",})
public class TestModelLoaderServiceWithSdc {

    @Autowired
    private ModelLoaderService service;

    @AfterEach
    public void shutdown() {
        service.preShutdownOperations();
    }

    @Test
    public void testIngestModel() throws IOException {
        byte[] csarPayload = new ArtifactTestUtils().loadResource("compressedArtifacts/service-VscpaasTest-csar.csar");
        Response response = service.ingestModel("model-name", "", Base64.getEncoder().encodeToString(csarPayload));
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }


}
