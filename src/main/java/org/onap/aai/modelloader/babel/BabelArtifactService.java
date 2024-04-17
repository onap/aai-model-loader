package org.onap.aai.modelloader.babel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.babel.service.data.BabelRequest;
import org.onap.aai.babel.service.data.BabelArtifact.ArtifactType;
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

  public void invokeBabelService(List<Artifact> modelArtifacts, List<Artifact> catalogArtifacts, BabelRequest babelRequest, String distributionId)
          throws ProcessToscaArtifactsException {
      try {
          logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                  "Posting artifact: " + babelRequest.getArtifactName() + ", service version: " + babelRequest.getArtifactVersion()
                          + ", artifact version: " + babelRequest.getArtifactVersion());
  
          List<BabelArtifact> babelArtifacts =
                babelServiceClient.postArtifact(babelRequest, distributionId);
  
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
                      babelRequest.getArtifactName() + " " + babelRequest.getArtifactVersion()
                              + ". Unexpected artifact types returned by the babel service: "
                              + artifactMap.keySet().toString());
          }
  
      } catch (BabelArtifactParsingException e) {
          logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR,
                  "Error for artifact " + babelRequest.getArtifactName() + " " + babelRequest.getArtifactVersion() + " " + e);
          throw new ProcessToscaArtifactsException(
                  "An error occurred while trying to parse the Babel artifacts: " + e.getLocalizedMessage());
      } catch (Exception e) {
          logger.error(ModelLoaderMsgs.BABEL_REST_REQUEST_ERROR, e, "POST",
                  "Error posting artifact " + babelRequest.getArtifactName() + " " + babelRequest.getArtifactVersion() + " to Babel: "
                          + e.getLocalizedMessage());
          throw new ProcessToscaArtifactsException(
                  "An error occurred while calling the Babel service: " + e.getLocalizedMessage());
      }
  }
  
}
