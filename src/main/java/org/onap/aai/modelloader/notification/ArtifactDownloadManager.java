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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.cl.mdc.MdcOverride;
import org.onap.aai.modelloader.babel.BabelArtifactService;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactType;
import org.onap.aai.modelloader.entity.catalog.VnfCatalogArtifact;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.IModelParser;
import org.onap.aai.modelloader.entity.model.NamedQueryArtifactParser;
import org.onap.aai.modelloader.extraction.InvalidArchiveException;
import org.onap.aai.modelloader.extraction.VnfCatalogExtractor;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.results.IDistributionClientDownloadResult;
import org.onap.sdc.utils.ArtifactTypeEnum;
import org.onap.sdc.utils.DistributionActionResultEnum;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for downloading the artifacts from the ASDC.
 *
 * The downloads can be TOSCA_CSAR files or VNF_CATALOG files.
 *
 * The status of the download is published. The status of the extraction of yml files from a TOSCA_CSAR file is also
 * published as a deployment event.
 *
 * TOSCA_CSAR file artifacts will be converted into XML and returned as model artifacts.
 */
@Component
public class ArtifactDownloadManager {

    private static Logger logger = LoggerFactory.getInstance().getLogger(ArtifactDownloadManager.class);

    private final IDistributionClient client;
    private final NotificationPublisher notificationPublisher;
    private final VnfCatalogExtractor vnfCatalogExtractor;
    private final BabelArtifactService babelArtifactService;

    public ArtifactDownloadManager(IDistributionClient client,
            NotificationPublisher notificationPublisher, VnfCatalogExtractor vnfCatalogExtractor, BabelArtifactService babelArtifactService) {
        this.client = client;
        this.notificationPublisher = notificationPublisher;
        this.vnfCatalogExtractor = vnfCatalogExtractor;
        this.babelArtifactService = babelArtifactService;
    }

    /**
     * This method downloads the artifacts from the ASDC.
     *
     * @param data data about the notification that is being processed
     * @param artifacts the specific artifacts found in the data.
     * @param modelArtifacts collection of artifacts for model query specs
     * @param catalogArtifacts collection of artifacts that represent vnf catalog files
     * @return boolean <code>true</code> if the download process was successful otherwise <code>false</code>
     * @throws Exception 
     */
    List<Artifact> downloadArtifacts(INotificationData data, List<IArtifactInfo> artifacts) throws Exception {

        List<Artifact> allArtifacts = new ArrayList<>();
        for (IArtifactInfo artifact : artifacts) {
            try {
                IDistributionClientDownloadResult downloadResult = downloadIndividualArtifacts(data, artifact);
                List<Artifact> processedArtifacts = processDownloadedArtifacts(artifact, downloadResult, data);
                allArtifacts.addAll(processedArtifacts);
            } catch (DownloadFailureException e) {
                notificationPublisher.publishDownloadFailure(client, data, artifact, e.getMessage());
                throw e;
            } catch (ProcessToscaArtifactsException | InvalidArchiveException | BabelArtifactParsingException e) {
                notificationPublisher.publishDeployFailure(client, data, artifact);
                throw e;
            }
        }
        return allArtifacts;
    }

    private IDistributionClientDownloadResult downloadIndividualArtifacts(INotificationData data,
            IArtifactInfo artifact) throws DownloadFailureException {
        // Grab the current time so we can measure the download time for the metrics log
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        MdcOverride override = new MdcOverride();
        override.addAttribute(MdcContext.MDC_START_TIME, ZonedDateTime.now().format(formatter));

        IDistributionClientDownloadResult downloadResult = client.download(artifact);

        logger.info(ModelLoaderMsgs.DOWNLOAD_COMPLETE, downloadResult.getDistributionActionResult().toString(),
                downloadResult.getArtifactPayload() == null ? "null"
                        : Base64.getEncoder().encodeToString(downloadResult.getArtifactPayload()));

        if (DistributionActionResultEnum.SUCCESS.equals(downloadResult.getDistributionActionResult())) {
            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Downloaded artifact: " + artifact.getArtifactName());
            notificationPublisher.publishDownloadSuccess(client, data, artifact);
        } else {
            throw new DownloadFailureException(downloadResult.getDistributionMessageResult());
        }

        return downloadResult;
    }

    private List<Artifact> processDownloadedArtifacts(
            IArtifactInfo artifactInfo, IDistributionClientDownloadResult downloadResult, INotificationData data)
            throws ProcessToscaArtifactsException, InvalidArchiveException, BabelArtifactParsingException {
        List<Artifact> artifacts = new ArrayList<>();
        List<Artifact> querySpecArtifacts = new ArrayList<>();
        if ("TOSCA_CSAR".equalsIgnoreCase(artifactInfo.getArtifactType())) {
            artifacts = processToscaArtifacts(downloadResult.getArtifactPayload(), artifactInfo,
                    data.getDistributionID(), data.getServiceVersion());
            
        } else if (ArtifactTypeEnum.MODEL_QUERY_SPEC.toString().equalsIgnoreCase(artifactInfo.getArtifactType())) {
            querySpecArtifacts = processModelQuerySpecArtifact(downloadResult);
        } else {
            logger.info(ModelLoaderMsgs.UNSUPPORTED_ARTIFACT_TYPE, artifactInfo.getArtifactName(),
                    artifactInfo.getArtifactType());
            throw new InvalidArchiveException("Unsupported artifact type: " + artifactInfo.getArtifactType());
        }
        return Stream
            .concat(artifacts.stream(), querySpecArtifacts.stream())
            .collect(Collectors.toList());
    }

    public List<Artifact> processToscaArtifacts(byte[] payload, IArtifactInfo artifactInfo, String distributionId, String serviceVersion)
            throws ProcessToscaArtifactsException, InvalidArchiveException {
        // Get translated artifacts from Babel Service
        BabelRequest babelRequest = new BabelRequest();
        babelRequest.setArtifactName(artifactInfo.getArtifactName());
        babelRequest.setCsar(Base64.getEncoder().encodeToString(payload));
        babelRequest.setArtifactVersion(serviceVersion);
        List<Artifact> artifacts = babelArtifactService.invokeBabelService(babelRequest, distributionId);

        // Get VNF Catalog artifacts directly from CSAR
        List<Artifact> csarCatalogArtifacts = vnfCatalogExtractor.extract(payload, artifactInfo.getArtifactName());

        // Throw an error if VNF Catalog data is present in the Babel payload and directly in the CSAR
        if (isDuplicateVnfCatalogData(artifacts, csarCatalogArtifacts)) {
            logger.error(ModelLoaderMsgs.DUPLICATE_VNFC_DATA_ERROR, artifactInfo.getArtifactName());
            throw new InvalidArchiveException("CSAR: " + artifactInfo.getArtifactName()
                    + " contains VNF Catalog data in the format of both TOSCA and XML files. Only one format can be used for each CSAR file.");
        }
        return Stream
                .concat(artifacts.stream(), csarCatalogArtifacts.stream())
                .collect(Collectors.toList());

    }

    private boolean isDuplicateVnfCatalogData(List<Artifact> babelArtifacts, List<Artifact> csarCatalogArtifacts) {
        boolean babelIsEmpty = babelArtifacts.stream()
            .filter(VnfCatalogArtifact.class::isInstance)
            .findAny().isEmpty();
        return !csarCatalogArtifacts.isEmpty() && !babelIsEmpty;
    }

    private List<Artifact> processModelQuerySpecArtifact(IDistributionClientDownloadResult downloadResult) throws BabelArtifactParsingException {
        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Processing named query artifact.");

        IModelParser parser = new NamedQueryArtifactParser();

        List<Artifact> parsedArtifacts =
                parser.parse(new String(downloadResult.getArtifactPayload()), downloadResult.getArtifactFilename());

        if (parsedArtifacts != null && !parsedArtifacts.isEmpty()) {
            return parsedArtifacts;
        } else {
            throw new BabelArtifactParsingException(
                    "Could not parse generated XML: " + new String(downloadResult.getArtifactPayload()));
        }
    }
}
