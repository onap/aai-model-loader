/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */
package org.onap.aai.modelloader.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.fixture.NotificationDataFixtureBuilder;
import org.onap.aai.modelloader.util.ArtifactTestUtils;
import org.openecomp.sdc.api.notification.INotificationData;

/**
 * Tests {@link BabelArtifactConverter}
 */
public class BabelArtifactConverterTest {

    @Test(expected = NullPointerException.class)
    public void convert_nullToscaFiles() throws BabelArtifactParsingException {
        new BabelArtifactConverter().convertToModel(null);
        fail("An instance of ArtifactGenerationException should have been thrown");
    }

    @Test
    public void convert_emptyToscaFiles() throws BabelArtifactParsingException {
        assertTrue("Nothing should have been returned",
                new BabelArtifactConverter().convertToModel(new ArrayList<>()).isEmpty());
    }

    @Test(expected = BabelArtifactParsingException.class)
    public void convert_problemWithConvertedXML() throws IOException, BabelArtifactParsingException {
        byte[] problemXML =
                "<model xmlns=\"http://org.openecomp.aai.inventory/v10\"><rubbish>This is some xml that should cause the model artifact parser to throw an erorr</rubbish></model>"
                        .getBytes();

        INotificationData data = NotificationDataFixtureBuilder.getNotificationDataWithToscaCsarFile();

        List<BabelArtifact> toscaArtifacts = setupTest(problemXML, data);

        new BabelArtifactConverter().convertToModel(toscaArtifacts);
        fail("An instance of ModelArtifactParsingException should have been thrown");
    }

    private List<BabelArtifact> setupTest(byte[] xml, INotificationData data) throws IOException {
        List<BabelArtifact> toscaArtifacts = new ArrayList<>();
        org.openecomp.sdc.api.notification.IArtifactInfo artifactInfo = data.getServiceArtifacts().get(0);

        BabelArtifact xmlArtifact =
                new BabelArtifact(artifactInfo.getArtifactName(), BabelArtifact.ArtifactType.MODEL, new String(xml));
        toscaArtifacts.add(xmlArtifact);

        return toscaArtifacts;
    }

}
