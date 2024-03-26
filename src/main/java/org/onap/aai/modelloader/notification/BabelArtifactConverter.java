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
package org.onap.aai.modelloader.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.ModelArtifactParser;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for converting TOSCA artifacts into instances of {@link ModelArtifact} ready for pushing
 * the converted artifacts .
 */
@Component
public class BabelArtifactConverter {

    /**
     * This method converts BabelArtifacts into instances of {@link ModelArtifact}.
     *
     * @param xmlArtifacts xml artifacts to be parsed
     * @return List<org.openecomp.modelloader.entity.Artifact> list of converted model artifacts
     * @throws BabelArtifactParsingException if an error occurs trying to parse the generated XML files that were
     *         converted from tosca artifacts
     */
    List<Artifact> convertToModel(List<BabelArtifact> xmlArtifacts) throws BabelArtifactParsingException {
        Objects.requireNonNull(xmlArtifacts);
        List<Artifact> modelArtifacts = new ArrayList<>();
        ModelArtifactParser modelArtParser = new ModelArtifactParser();

        // Parse TOSCA payloads
        for (BabelArtifact xmlArtifact : xmlArtifacts) {

            List<Artifact> parsedArtifacts = modelArtParser.parse(xmlArtifact.getPayload(), xmlArtifact.getName());

            if (parsedArtifacts == null || parsedArtifacts.isEmpty()) {
                throw new BabelArtifactParsingException("Could not parse generated XML: " + xmlArtifact.getPayload());
            }

            modelArtifacts.addAll(parsedArtifacts);
        }

        return modelArtifacts;
    }

    /**
     * This method converts BabelArtifacts into instances of {@link VnfCatalogArtifact}.
     *
     * @param xmlArtifacts xml artifacts to be parsed
     * @return List<org.openecomp.modelloader.entity.Artifact> list of converted catalog artifacts
     */
    List<Artifact> convertToCatalog(List<BabelArtifact> xmlArtifacts) {
        Objects.requireNonNull(xmlArtifacts);
        List<Artifact> catalogArtifacts = new ArrayList<>();

        for (BabelArtifact xmlArtifact : xmlArtifacts) {
            catalogArtifacts.add(new VnfCatalogArtifact(xmlArtifact.getPayload()));
        }

        return catalogArtifacts;
    }
}
