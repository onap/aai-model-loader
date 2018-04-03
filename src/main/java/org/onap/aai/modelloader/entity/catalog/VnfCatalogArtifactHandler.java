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
package org.onap.aai.modelloader.entity.catalog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.text.StringEscapeUtils;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.restclient.client.OperationResult;
import org.springframework.web.util.UriUtils;

/**
 * VNF Catalog specific handling
 */
public class VnfCatalogArtifactHandler extends ArtifactHandler {

    private static Logger logger = LoggerFactory.getInstance().getLogger(VnfCatalogArtifactHandler.class.getName());

    public static final String ATTR_UUID = "uuid";

    public VnfCatalogArtifactHandler(ModelLoaderConfig config) {
        super(config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openecomp.modelloader.entity.ArtifactHandler#pushArtifacts(java.util.List, java.lang.String)
     */
    @Override
    public boolean pushArtifacts(List<Artifact> artifacts, String distributionId, List<Artifact> completedArtifacts,
            AaiRestClient aaiClient) {
        for (Artifact artifact : artifacts) {
            try {
                distributeVnfcData(aaiClient, distributionId, artifact, completedArtifacts);
            } catch (VnfImageException e) {
                if (e.getResultCode().isPresent()) {
                    logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
                            "Ingestion failed on vnf-image " + e.getImageId() + " with status "
                                    + e.getResultCode().orElse(0) + ". Rolling back distribution.");
                } else {
                    logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
                            "Ingestion failed on " + e.getImageId() + ". Rolling back distribution.");
                }
                return false;
            }
        }

        return true;
    }

    private void distributeVnfcData(AaiRestClient restClient, String distributionId, Artifact vnfcArtifact,
            List<Artifact> completedArtifacts) throws VnfImageException {

        List<Map<String, String>> vnfcData = unmarshallVnfcData(vnfcArtifact);

        for (Map<String, String> dataItem : vnfcData) {
            // If an empty dataItem is supplied, do nothing.
            if (dataItem.isEmpty()) {
                logger.warn(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Empty image data supplied, skipping ingestion.");
                return;
            }

            String urlParams;
            StringBuilder imageId = new StringBuilder("vnf image");

            try {
                urlParams = buildUrlImgIdStrings(imageId, dataItem);
            } catch (UnsupportedEncodingException e) {
                throw new VnfImageException(e);
            }

            OperationResult tryGet =
                    restClient.getResource(config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "?" + urlParams,
                            distributionId, MediaType.APPLICATION_JSON_TYPE);

            if (tryGet == null) {
                throw new VnfImageException(imageId.toString());
            }

            int resultCode = tryGet.getResultCode();
            if (resultCode == Response.Status.NOT_FOUND.getStatusCode()) {
                // This vnf-image is missing, so add it
                boolean success = putVnfImage(restClient, dataItem, distributionId);
                if (!success) {
                    throw new VnfImageException(imageId.toString());
                }
                completedArtifacts.add(vnfcArtifact);
                logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, imageId + " successfully ingested.");
            } else if (resultCode == Response.Status.OK.getStatusCode()) {
                logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, imageId + " already exists. Skipping ingestion.");
            } else {
                // if other than 404 or 200, something went wrong
                throw new VnfImageException(imageId.toString(), resultCode);
            }
        }
    }

    private String buildUrlImgIdStrings(StringBuilder imageId, Map<String, String> dataItem)
            throws UnsupportedEncodingException {
        StringBuilder urlParams = new StringBuilder();
        for (Entry<String, String> entry : dataItem.entrySet()) {
            urlParams.append(entry.getKey()).append("=").append(UriUtils.encode(entry.getValue(), "UTF-8")).append("&");
            imageId.append(" ").append(entry.getValue());
        }
        return urlParams.deleteCharAt(urlParams.length() - 1).toString();
    }

    private boolean putVnfImage(AaiRestClient restClient, Map<String, String> dataItem, String distributionId) {
        // Generate a new UUID for the image data item
        String uuid = UUID.randomUUID().toString();
        dataItem.put(ATTR_UUID, uuid);

        String payload = createVnfImagePayload(dataItem);
        String putUrl = config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "/vnf-image/" + uuid;
        OperationResult putResp =
                restClient.putResource(putUrl, payload, distributionId, MediaType.APPLICATION_JSON_TYPE);
        return putResp != null && putResp.getResultCode() == Response.Status.CREATED.getStatusCode();
    }

    private String createVnfImagePayload(Map<String, String> dataItem) {
        dataItem.put(ATTR_UUID, UUID.randomUUID().toString());
        return new Gson().toJson(dataItem);
    }

    private List<Map<String, String>> unmarshallVnfcData(Artifact vnfcArtifact) {
        // Unmarshall Babel JSON payload into a List of Maps of JSON attribute name/values.
        return new Gson().fromJson(StringEscapeUtils.unescapeJson(vnfcArtifact.getPayload()),
                new TypeToken<List<Map<String, String>>>() {}.getType());
    }

    /*
     * If something fails in the middle of ingesting the catalog we want to roll back any changes to the DB
     */
    @Override
    public void rollback(List<Artifact> completedArtifacts, String distributionId, AaiRestClient aaiClient) {
        for (Artifact completedArtifact : completedArtifacts) {
            List<Map<String, String>> completedImageData = unmarshallVnfcData(completedArtifact);
            for (Map<String, String> data : completedImageData) {
                String url = config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "/vnf-image/" + data.get(ATTR_UUID);
                // Try to delete the image. If something goes wrong we can't really do anything here
                aaiClient.getAndDeleteResource(url, distributionId);
            }
        }
    }

}
