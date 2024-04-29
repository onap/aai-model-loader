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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.ModelArtifactParser;
import org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;

/**
 * Tests {@link BabelArtifactConverter}.
 */
public class TestBabelArtifactConverter {

    @Test
    public void convert_nullToscaFiles() throws BabelArtifactParsingException {
        assertThrows(NullPointerException.class, () -> {
            new BabelArtifactConverter(new ModelArtifactParser()).convertToModel(null);
            fail("An instance of ArtifactGenerationException should have been thrown");
        });
    }

    @Test
    public void testInvalidXml() throws IOException, BabelArtifactParsingException {
        assertThrows(BabelArtifactParsingException.class, () -> {
            byte[] problemXml =
                    "<model xmlns=\"http://org.openecomp.aai.inventory/v10\"><rubbish>This is some xml that should cause the model artifact parser to throw an erorr</rubbish></model>"
                            .getBytes();

            INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

            BabelArtifact toscaArtifact = setupTest(problemXml, data);

            new BabelArtifactConverter(new ModelArtifactParser()).convertToModel(toscaArtifact);
            fail("An instance of ModelArtifactParsingException should have been thrown");
        });
    }

    private BabelArtifact setupTest(byte[] xml, INotificationData data) throws IOException {
        IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        BabelArtifact xmlArtifact =
                new BabelArtifact(artifactInfo.getArtifactName(), BabelArtifact.ArtifactType.MODEL, new String(xml));

        return xmlArtifact;
    }

    @Test
    public void convert_singleResourceFile() throws BabelArtifactParsingException, IOException {
        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

        byte[] xml = new ArtifactTestUtils().loadResource("convertedYmls/AAI-SCP-Test-VSP-resource-1.0.xml");
        BabelArtifact toscaArtifact = setupTest(xml, data);

        List<Artifact> modelArtifacts = new BabelArtifactConverter(new ModelArtifactParser()).convertToModel(toscaArtifact);

        assertEquals(1, modelArtifacts.size(), "There should have been 1 artifact");
        assertEquals(new String(xml), modelArtifacts.get(0).getPayload());
        assertEquals(ArtifactType.MODEL, modelArtifacts.get(0).getType());
    }
}
