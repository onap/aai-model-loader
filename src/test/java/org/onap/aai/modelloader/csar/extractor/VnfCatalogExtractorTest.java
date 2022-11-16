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
package org.onap.aai.modelloader.csar.extractor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.extraction.InvalidArchiveException;
import org.onap.aai.modelloader.extraction.VnfCatalogExtractor;
import org.onap.aai.modelloader.util.ArtifactTestUtils;


/**
 * Tests {@link VnfCatalogExtractor}.
 */
public class VnfCatalogExtractorTest {

    private static final String FOO = "foo";
    private static final String SOME_BYTES = "just some bytes that will pass the firsts validation";
    private static final String SUPPLY_AN_ARCHIVE = "An archive must be supplied for processing.";
    private static final String SUPPLY_NAME = "The name of the archive must be supplied for processing.";

    @Test
    public void nullContentSupplied() {
        invalidArgumentsTest(null, FOO, SUPPLY_AN_ARCHIVE);
    }

    @Test
    public void emptyContentSupplied() {
        invalidArgumentsTest(new byte[0], FOO, SUPPLY_AN_ARCHIVE);
    }

    @Test
    public void nullNameSupplied() {
        invalidArgumentsTest(SOME_BYTES.getBytes(), null, SUPPLY_NAME);
    }

    @Test
    public void blankNameSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), "  \t  ", SUPPLY_NAME);
    }

    @Test
    public void emptyNameSupplied() {
        invalidArgumentsTest("just some bytes that will pass the firsts validation".getBytes(), "", SUPPLY_NAME);
    }

    @Test
    public void invalidContentSupplied() {
        invalidArgumentsTest("This is a piece of nonsense and not a zip file".getBytes(), FOO,
                "An error occurred trying to create a ZipFile. Is the content being converted really a csar file?");
    }

    @Test
    public void archiveContainsNoVnfcFiles() throws InvalidArchiveException, IOException {
        List<Artifact> vnfcArtifacts = new VnfCatalogExtractor().extract(
                new ArtifactTestUtils().loadResource("compressedArtifacts/noVnfcFilesArchive.csar"),
                "noVnfcFilesArchive.csar");
        assertTrue("No VNFC files should have been extracted, but " + vnfcArtifacts.size() + " were found.",
                vnfcArtifacts.isEmpty());
    }

    @Test
    public void archiveContainsThreeRelevantVnfcFiles() throws IOException, InvalidArchiveException {
        List<String> payloads = new ArrayList<>();
        payloads.add("xmlFiles/vnfcatalog-1.xml");
        payloads.add("xmlFiles/vnfcatalog-2.xml");
        payloads.add("xmlFiles/vnfcatalog-3.xml");
        String csarArchiveFile = "threeVnfcFilesArchive.csar";
        performVnfcAsserts(new VnfCatalogExtractor().extract(
                new ArtifactTestUtils().loadResource("compressedArtifacts/" + csarArchiveFile), csarArchiveFile),
                payloads);
    }

    public void performVnfcAsserts(List<Artifact> actualVnfcArtifacts, List<String> expectedVnfcPayloadsToLoad) {
        assertThat("An unexpected number of VNFC files have been extracted", actualVnfcArtifacts.size(),
                is(expectedVnfcPayloadsToLoad.size()));

        for (Artifact artifact : actualVnfcArtifacts) {
            assertThat("Unexpected artifact type found.", artifact.getType(), is(ArtifactType.VNF_CATALOG_XML));
        }

        Set<String> expectedVnfcPayloads = expectedVnfcPayloadsToLoad.stream().map(s -> {
            try {
                return ArtifactTestUtils.loadResourceAsString(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());

        Set<String> actualVnfcPayloads =
                actualVnfcArtifacts.stream().map(s -> s.getPayload()).collect(Collectors.toSet());

        assertThat("Unexpected VNF Catalog file(s) found.", expectedVnfcPayloads.containsAll(actualVnfcPayloads),
                is(true));
    }

    private void invalidArgumentsTest(byte[] archive, String name, String expectedErrorMessage) {
        try {
            new VnfCatalogExtractor().extract(archive, name);
            fail("An instance of InvalidArchiveException should have been thrown");
        } catch (Exception ex) {
            assertTrue(ex instanceof InvalidArchiveException);
            assertEquals(expectedErrorMessage, ex.getLocalizedMessage());
        }
    }

}

