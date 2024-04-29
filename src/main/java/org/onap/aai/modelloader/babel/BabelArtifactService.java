/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2024 Deutsche Telekom AG Intellectual Property. All rights reserved.
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
package org.onap.aai.modelloader.babel;

import java.util.ArrayList;
import java.util.List;

import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.notification.BabelArtifactConverter;
import org.onap.aai.modelloader.notification.ProcessToscaArtifactsException;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.springframework.stereotype.Service;

@Service
public class BabelArtifactService {

    private static Logger logger = LoggerFactory.getInstance().getLogger(BabelArtifactService.class);

    private final BabelServiceClient babelServiceClient;
    private final BabelArtifactConverter babelArtifactConverter;

    public BabelArtifactService(BabelServiceClient babelServiceClient, BabelArtifactConverter babelArtifactConverter) {
        this.babelServiceClient = babelServiceClient;
        this.babelArtifactConverter = babelArtifactConverter;
    }

    public List<Artifact> invokeBabelService(BabelRequest babelRequest, String distributionId)
            throws ProcessToscaArtifactsException {
        try {
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                    "Posting artifact: " + babelRequest.getArtifactName() + ", service version: "
                            + babelRequest.getArtifactVersion()
                            + ", artifact version: " + babelRequest.getArtifactVersion());

            List<BabelArtifact> babelArtifacts = babelServiceClient.postArtifact(babelRequest, distributionId);

            List<Artifact> convertedArtifacts = new ArrayList<>();
            for(BabelArtifact babelArtifact : babelArtifacts) {
                if(!isUnknownType(babelArtifact)) {
                    if(babelArtifact.getType() == ArtifactType.MODEL) {
                        convertedArtifacts.addAll(babelArtifactConverter.convertToModel(babelArtifact));
                    } else {
                        convertedArtifacts.add(babelArtifactConverter.convertToCatalog(babelArtifact));
                    }
                }
            }

            return convertedArtifacts;

        } catch (BabelArtifactParsingException e) {
            logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR,
                    "Error for artifact " + babelRequest.getArtifactName() + " " + babelRequest.getArtifactVersion()
                            + " " + e);
            throw new ProcessToscaArtifactsException(
                    "An error occurred while trying to parse the Babel artifacts: " + e.getLocalizedMessage());
        } catch (Exception e) {
            logger.error(ModelLoaderMsgs.BABEL_REST_REQUEST_ERROR, e, "POST",
                    "Error posting artifact " + babelRequest.getArtifactName() + " " + babelRequest.getArtifactVersion()
                            + " to Babel: "
                            + e.getLocalizedMessage());
            throw new ProcessToscaArtifactsException(
                    "An error occurred while calling the Babel service: " + e.getLocalizedMessage());
        }
    }

    private boolean isUnknownType(BabelArtifact babelArtifact) {
        if (babelArtifact.getType() == ArtifactType.MODEL || babelArtifact.getType() == ArtifactType.VNFCATALOG) {
            return false;
        } else {
            logger.warn(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR,
                    babelArtifact.getName() + " " + babelArtifact.getType()
                            + ". Unexpected artifact types returned by the babel service: "
                            + babelArtifact.getPayload());
            return true;
        }
    }

}