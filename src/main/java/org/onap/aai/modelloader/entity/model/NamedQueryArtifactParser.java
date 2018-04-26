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
package org.onap.aai.modelloader.entity.model;

import java.util.List;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.w3c.dom.Node;

public class NamedQueryArtifactParser extends AbstractModelArtifactParser {

    private static final String NAMED_QUERY_VERSION_ID = "named-query-uuid";
    private static final String MODEL_ELEMENT_RELATIONSHIP_KEY = "model.model-invariant-id";

    private static Logger logger = LoggerFactory.getInstance().getLogger(NamedQueryArtifactParser.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    boolean processParsedModel(List<Artifact> modelList, String artifactName, IModelArtifact model) {
        boolean valid = false;

        if (model != null) {
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Named-Query parsed =====>>>> " + "Named-Query-UUID: "
                    + ((NamedQueryArtifact) model).getNamedQueryUuid());
            modelList.add((NamedQueryArtifact) model);

            valid = true;
        } else {
            logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse named-query artifact " + artifactName);
        }

        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String buildArtifactParseExceptionMessage(String artifactName, String localisedMessage) {
        return "Unable to parse named-query artifact " + artifactName + ": " + localisedMessage;
    }

    @Override
    String getModelElementRelationshipKey() {
        return MODEL_ELEMENT_RELATIONSHIP_KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getVersionIdNodeName() {
        return NAMED_QUERY_VERSION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setVersionId(IModelArtifact model, Node node) {
        ((NamedQueryArtifact) model).setNamedQueryUuid(node.getTextContent().trim());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    IModelArtifact createModelArtifactInstance() {
        return new NamedQueryArtifact();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean modelIsValid(IModelArtifact model) {
        return ((NamedQueryArtifact) model).getNamedQueryUuid() != null;
    }
}
