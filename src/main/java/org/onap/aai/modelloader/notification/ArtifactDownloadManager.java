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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.cl.mdc.MdcOverride;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.model.BabelArtifactParsingException;
import org.onap.aai.modelloader.entity.model.IModelParser;
import org.onap.aai.modelloader.entity.model.NamedQueryArtifactParser;
import org.onap.aai.modelloader.extraction.InvalidArchiveException;
import org.onap.aai.modelloader.extraction.VnfCatalogExtractor;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.restclient.BabelServiceClientException;
import org.onap.aai.modelloader.service.BabelServiceClientFactory;
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
    private final BabelArtifactConverter babelArtifactConverter;
    private final ModelLoaderConfig config;
    private final BabelServiceClientFactory clientFactory;
    private final VnfCatalogExtractor vnfCatalogExtractor;

    public ArtifactDownloadManager(IDistributionClient client, ModelLoaderConfig config,
            BabelServiceClientFactory clientFactory, BabelArtifactConverter babelArtifactConverter, NotificationPublisher notificationPublisher, VnfCatalogExtractor vnfCatalogExtractor) {
        this.client = client;
        this.notificationPublisher = notificationPublisher;
        this.babelArtifactConverter = babelArtifactConverter;
        this.config = config;
        this.clientFactory = clientFactory;
        this.vnfCatalogExtractor = vnfCatalogExtractor;
    }

    /**
     * This method downloads the artifacts from the ASDC.
     *
     * @param data data about the notification that is being processed
     * @param artifacts the specific artifacts found in the data.
     * @param modelArtifacts collection of artifacts for model query specs
     * @param catalogArtifacts collection of artifacts that represent vnf catalog files
     * @return boolean <code>true</code> if the download process was successful otherwise <code>false</code>
     */
    boolean downloadArtifacts(INotificationData data, List<IArtifactInfo> artifacts, List<Artifact> modelArtifacts,
            List<Artifact> catalogArtifacts) {
        boolean success = true;

        for (IArtifactInfo artifact : artifacts) {
            try {
                IDistributionClientDownloadResult downloadResult = downloadIndividualArtifacts(data, artifact);
                processDownloadedArtifacts(modelArtifacts, catalogArtifacts, artifact, downloadResult, data);
            } catch (DownloadFailureException e) {
                notificationPublisher.publishDownloadFailure(client, data, artifact, e.getMessage());
                success = false;
            } catch (Exception e) {
                notificationPublisher.publishDeployFailure(client, data, artifact);
                success = false;
            }

            if (!success) {
                break;
            }
        }

        return success;
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

    private void processDownloadedArtifacts(List<Artifact> modelArtifacts, List<Artifact> catalogArtifacts,
            IArtifactInfo artifactInfo, IDistributionClientDownloadResult downloadResult, INotificationData data)
            throws ProcessToscaArtifactsException, InvalidArchiveException, BabelArtifactParsingException {
        if ("TOSCA_CSAR".equalsIgnoreCase(artifactInfo.getArtifactType())) {
            processToscaArtifacts(modelArtifacts, catalogArtifacts, downloadResult.getArtifactPayload(), artifactInfo,
                    data.getDistributionID(), data.getServiceVersion());
        } else if (ArtifactTypeEnum.MODEL_QUERY_SPEC.toString().equalsIgnoreCase(artifactInfo.getArtifactType())) {
            processModelQuerySpecArtifact(modelArtifacts, downloadResult);
        } else {
            logger.info(ModelLoaderMsgs.UNSUPPORTED_ARTIFACT_TYPE, artifactInfo.getArtifactName(),
                    artifactInfo.getArtifactType());
            throw new InvalidArchiveException("Unsupported artifact type: " + artifactInfo.getArtifactType());
        }
    }

    public void processToscaArtifacts(List<Artifact> modelArtifacts, List<Artifact> catalogArtifacts, byte[] payload,
            IArtifactInfo artifactInfo, String distributionId, String serviceVersion)
            throws ProcessToscaArtifactsException, InvalidArchiveException {
        // Get translated artifacts from Babel Service
        invokeBabelService(modelArtifacts, catalogArtifacts, payload, artifactInfo, distributionId, serviceVersion);

        // Get VNF Catalog artifacts directly from CSAR
        List<Artifact> csarCatalogArtifacts = vnfCatalogExtractor.extract(payload, artifactInfo.getArtifactName());

        // Throw an error if VNF Catalog data is present in the Babel payload and directly in the CSAR
        if (!catalogArtifacts.isEmpty() && !csarCatalogArtifacts.isEmpty()) {
            logger.error(ModelLoaderMsgs.DUPLICATE_VNFC_DATA_ERROR, artifactInfo.getArtifactName());
            throw new InvalidArchiveException("CSAR: " + artifactInfo.getArtifactName()
                    + " contains VNF Catalog data in the format of both TOSCA and XML files. Only one format can be used for each CSAR file.");
        } else if (!csarCatalogArtifacts.isEmpty()) {
            catalogArtifacts.addAll(csarCatalogArtifacts);
        }
    }

    public void invokeBabelService(List<Artifact> modelArtifacts, List<Artifact> catalogArtifacts, byte[] payload,
            IArtifactInfo artifactInfo, String distributionId, String serviceVersion)
            throws ProcessToscaArtifactsException {
        try {
            BabelServiceClient babelClient = createBabelServiceClient(artifactInfo, serviceVersion);

            logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                    "Posting artifact: " + artifactInfo.getArtifactName() + ", service version: " + serviceVersion
                            + ", artifact version: " + artifactInfo.getArtifactVersion());

            List<BabelArtifact> babelArtifacts =
                    babelClient.postArtifact(payload, artifactInfo.getArtifactName(), serviceVersion, distributionId);

            // Sort Babel artifacts based on type
            Map<ArtifactType, List<BabelArtifact>> artifactMap =
                    babelArtifacts.stream().collect(Collectors.groupingBy(BabelArtifact::getType));

            if (artifactMap.containsKey(BabelArtifact.ArtifactType.MODEL)) {
                modelArtifacts.addAll(
                        babelArtifactConverter.convertToModel(artifactMap.get(BabelArtifact.ArtifactType.MODEL)));
                artifactMap.remove(BabelArtifact.ArtifactType.MODEL);
            }

            if (artifactMap.containsKey(BabelArtifact.ArtifactType.VNFCATALOG)) {
                catalogArtifacts.addAll(babelArtifactConverter
                        .convertToCatalog(artifactMap.get(BabelArtifact.ArtifactType.VNFCATALOG)));
                artifactMap.remove(BabelArtifact.ArtifactType.VNFCATALOG);
            }

            // Log unexpected artifact types
            if (!artifactMap.isEmpty()) {
                logger.warn(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR,
                        artifactInfo.getArtifactName() + " " + serviceVersion
                                + ". Unexpected artifact types returned by the babel service: "
                                + artifactMap.keySet().toString());
            }

        } catch (BabelArtifactParsingException e) {
            logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR,
                    "Error for artifact " + artifactInfo.getArtifactName() + " " + serviceVersion + e);
            throw new ProcessToscaArtifactsException(
                    "An error occurred while trying to parse the Babel artifacts: " + e.getLocalizedMessage());
        } catch (Exception e) {
            logger.error(ModelLoaderMsgs.BABEL_REST_REQUEST_ERROR, e, "POST", config.getBabelProperties().getBaseUrl(),
                    "Error posting artifact " + artifactInfo.getArtifactName() + " " + serviceVersion + " to Babel: "
                            + e.getLocalizedMessage());
            throw new ProcessToscaArtifactsException(
                    "An error occurred while calling the Babel service: " + e.getLocalizedMessage());
        }
    }

    BabelServiceClient createBabelServiceClient(IArtifactInfo artifact, String serviceVersion)
            throws ProcessToscaArtifactsException {
        BabelServiceClient babelClient;
        try {
            logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Creating Babel client");
            babelClient = clientFactory.create(config);
        } catch (BabelServiceClientException e) {
            logger.error(ModelLoaderMsgs.BABEL_REST_REQUEST_ERROR, e, "POST", config.getBabelProperties().getBaseUrl(),
                    "Error posting artifact " + artifact.getArtifactName() + " " + serviceVersion + " to Babel: "
                            + e.getLocalizedMessage());
            throw new ProcessToscaArtifactsException(
                    "An error occurred tyring to convert the tosca artifacts to xml artifacts: "
                            + e.getLocalizedMessage());
        }

        return babelClient;
    }

    private void processModelQuerySpecArtifact(List<Artifact> modelArtifacts,
            IDistributionClientDownloadResult downloadResult) throws BabelArtifactParsingException {
        logger.debug(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Processing named query artifact.");

        IModelParser parser = new NamedQueryArtifactParser();

        List<Artifact> parsedArtifacts =
                parser.parse(new String(downloadResult.getArtifactPayload()), downloadResult.getArtifactFilename());

        if (parsedArtifactsExist(parsedArtifacts)) {
            modelArtifacts.addAll(parsedArtifacts);
        } else {
            throw new BabelArtifactParsingException(
                    "Could not parse generated XML: " + new String(downloadResult.getArtifactPayload()));
        }
    }

    private boolean parsedArtifactsExist(List<Artifact> parsedArtifacts) {
        return parsedArtifacts != null && !parsedArtifacts.isEmpty();
    }
}
