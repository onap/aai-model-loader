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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.springframework.stereotype.Component;


/**
 * The purpose of this class is to process a .csar file in the form of a byte array and extract VNF Catalog XML files
 * from it.
 *
 * A .csar file is a compressed archive like a zip file and this class will treat the byte array as it if were a zip
 * file.
 */
@Component
public class VnfCatalogExtractor {
    private static final Logger logger = LoggerFactory.getInstance().getLogger(VnfCatalogExtractor.class.getName());

    private static final Pattern VNFCFILE_EXTENSION_REGEX =
            Pattern.compile("(?i)artifacts([\\\\/])deployment([\\\\/])vnf_catalog([\\\\/]).*\\.xml$");

    /**
     * This method is responsible for filtering the contents of the supplied archive and returning a collection of
     * {@link Artifact}s that represent the VNF Catalog files that have been found in the archive.<br>
     * <br>
     * If the archive contains no VNF Catalog files it will return an empty list.<br>
     *
     * @param archive the zip file in the form of a byte array containing zero or more VNF Catalog files
     * @param name the name of the archive file
     * @return List<Artifact> collection of VNF Catalog XML files found in the archive
     * @throws InvalidArchiveException if an error occurs trying to extract the VNFC files from the archive or if the
     *         archive is not a zip file
     */
    public List<Artifact> extract(byte[] archive, String name) throws InvalidArchiveException {
        validateRequest(archive, name);

        logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Extracting CSAR archive: " + name);

        List<Artifact> vnfcFiles = new ArrayList<>();
        try (SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(archive);
                ZipFile zipFile = new ZipFile(inMemoryByteChannel)) {
            for (Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries(); enumeration.hasMoreElements();) {
                ZipArchiveEntry entry = enumeration.nextElement();
                if (fileShouldBeExtracted(entry)) {
                    vnfcFiles.add(new VnfCatalogArtifact(ArtifactType.VNF_CATALOG_XML,
                            IOUtils.toString(zipFile.getInputStream(entry), Charset.defaultCharset())));
                }
            }
        } catch (IOException e) {
            throw new InvalidArchiveException(
                    "An error occurred trying to create a ZipFile. Is the content being converted really a csar file?",
                    e);
        }

        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, vnfcFiles.size() + " VNF Catalog files extracted.");

        return vnfcFiles;
    }

    private static void validateRequest(byte[] archive, String name) throws InvalidArchiveException {
        if (archive == null || archive.length == 0) {
            throw new InvalidArchiveException("An archive must be supplied for processing.");
        } else if (StringUtils.isBlank(name)) {
            throw new InvalidArchiveException("The name of the archive must be supplied for processing.");
        }
    }

    private static boolean fileShouldBeExtracted(ZipArchiveEntry entry) {
        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Checking if " + entry.getName() + " should be extracted...");
        boolean extractFile = VNFCFILE_EXTENSION_REGEX.matcher(entry.getName()).matches();
        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Keeping file: " + entry.getName() + "? : " + extractFile);
        return extractFile;
    }
}

