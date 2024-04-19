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
package org.onap.aai.modelloader.entity.catalog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.utils.URIBuilder;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.restclient.client.OperationResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * VNF Catalog specific handling
 */
@Component
public class VnfCatalogArtifactHandler extends ArtifactHandler {

    private static Logger logger = LoggerFactory.getInstance().getLogger(VnfCatalogArtifactHandler.class.getName());

    public static final String ATTR_UUID = "uuid";

    public VnfCatalogArtifactHandler(ModelLoaderConfig config) {
        super(config);
    }

    @Override
    public boolean pushArtifacts(List<Artifact> artifacts, String distributionId, List<Artifact> completedArtifacts,
            RestTemplate restTemplate) {
        // TODO Migrate to this method, away from the deprecated AaiRestClient one
        throw new UnsupportedOperationException("Unimplemented method 'pushArtifacts'");
    }

    @Override
    public void rollback(List<Artifact> completedArtifacts, String distributionId, RestTemplate restTemplate) {
        // TODO Migrate to this method, away from the deprecated AaiRestClient one
        throw new UnsupportedOperationException("Unimplemented method 'rollback'");
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

    /*
     * If something fails in the middle of ingesting the catalog we want to roll back any changes to the DB
     */
    @Override
    public void rollback(List<Artifact> completedArtifacts, String distributionId, AaiRestClient aaiClient) {
        for (Artifact completedArtifact : completedArtifacts) {
            Map<String, String> data = new Gson().fromJson(completedArtifact.getPayload(),
                    new TypeToken<Map<String, String>>() {}.getType());
            String url = config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "/vnf-image/" + data.get(ATTR_UUID);
            // Try to delete the image. If something goes wrong we can't really do anything here
            aaiClient.getAndDeleteResource(url, distributionId);
        }
    }

    private void distributeVnfcData(AaiRestClient restClient, String distributionId, Artifact vnfcArtifact,
            List<Artifact> completedArtifacts) throws VnfImageException {
        List<Map<String, String>> vnfcData;
        switch (vnfcArtifact.getType()) {
            case VNF_CATALOG:
                vnfcData = unmarshallVnfcData(vnfcArtifact);
                break;
            case VNF_CATALOG_XML:
                vnfcData = parseXmlVnfcData(vnfcArtifact);
                break;
            default:
                throw new VnfImageException("Unsupported type " + vnfcArtifact.getType());
        }
        distributeVnfcData(restClient, distributionId, completedArtifacts, vnfcData);
    }

    /**
     * Build a VNF image from each of the supplied data items, and distribute to AAI
     * 
     * @param restClient
     * @param distributionId
     * @param completedArtifacts
     * @param vnfcData
     * @throws VnfImageException
     */
    private void distributeVnfcData(AaiRestClient restClient, String distributionId, List<Artifact> completedArtifacts,
            List<Map<String, String>> vnfcData) throws VnfImageException {
        for (Map<String, String> dataItem : vnfcData) {
            // If an empty dataItem is supplied, do nothing.
            if (dataItem.isEmpty()) {
                logger.warn(ModelLoaderMsgs.DISTRIBUTION_EVENT, "Empty image data supplied, skipping ingestion.");
                continue;
            }

            StringBuilder imageIdBuilder = new StringBuilder("vnf image");
            for (Entry<String, String> entry : dataItem.entrySet()) {
                imageIdBuilder.append(" ").append(entry.getValue());
            }
            String imageId = imageIdBuilder.toString();
            int resultCode = getVnfImage(restClient, distributionId, imageId, dataItem);

            if (resultCode == Response.Status.NOT_FOUND.getStatusCode()) {
                // This vnf-image is missing, so add it
                boolean success = putVnfImage(restClient, dataItem, distributionId);
                if (success) {
                    completedArtifacts.add(new VnfCatalogArtifact(new Gson().toJson(dataItem)));
                    logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, imageId + " successfully ingested.");
                } else {
                    throw new VnfImageException(imageId);
                }
            } else if (resultCode == Response.Status.OK.getStatusCode()) {
                logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, imageId + " already exists. Skipping ingestion.");
            } else {
                // if other than 404 or 200, something went wrong
                throw new VnfImageException(imageId, resultCode);
            }
        }
    }

    private int getVnfImage(AaiRestClient restClient, String distributionId, String imageId,
            Map<String, String> dataItem) throws VnfImageException {
        try {
            URIBuilder b = new URIBuilder(config.getAaiBaseUrl() + config.getAaiVnfImageUrl());
            for (Entry<String, String> entry : dataItem.entrySet()) {
                b.addParameter(entry.getKey(), entry.getValue());
            }
            OperationResult tryGet =
                    restClient.getResource(b.build().toString(), distributionId, MediaType.APPLICATION_JSON_TYPE);
            if (tryGet == null) {
                throw new VnfImageException(imageId);
            }
            return tryGet.getResultCode();
        } catch (URISyntaxException ex) {
            throw new VnfImageException(ex);
        }
    }

    private boolean putVnfImage(AaiRestClient restClient, Map<String, String> dataItem, String distributionId) {
        // Generate a new UUID for the image data item
        String uuid = UUID.randomUUID().toString();
        dataItem.put(ATTR_UUID, uuid);

        String payload = new Gson().toJson(dataItem);
        String putUrl = config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "/vnf-image/" + uuid;
        OperationResult putResp =
                restClient.putResource(putUrl, payload, distributionId, MediaType.APPLICATION_JSON_TYPE);
        return putResp != null && putResp.getResultCode() == Response.Status.CREATED.getStatusCode();
    }

    private List<Map<String, String>> unmarshallVnfcData(Artifact vnfcArtifact) {
        // Unmarshall Babel JSON payload into a List of Maps of JSON attribute name/values.
        return new Gson().fromJson(StringEscapeUtils.unescapeJson(vnfcArtifact.getPayload()),
                new TypeToken<List<Map<String, String>>>() {}.getType());
    }

    /**
     * Parse the VNF Catalog XML and transform into Key/Value pairs.
     * 
     * @param vnfcArtifact
     * @return VNF Image data in Map form
     * @throws VnfImageException
     */
    private List<Map<String, String>> parseXmlVnfcData(Artifact vnfcArtifact) throws VnfImageException {
        List<Map<String, String>> vnfcData = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(vnfcArtifact.getPayload()));
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList pnl = doc.getElementsByTagName("part-number-list");
            for (int i = 0; i < pnl.getLength(); i++) {
                Node partNumber = pnl.item(i);
                if (partNumber.getNodeType() == Node.ELEMENT_NODE) {
                    Element vendorInfo = getFirstChildNodeByName(partNumber, "vendor-info");
                    if (vendorInfo != null) {
                        Map<String, String> application = new HashMap<>();
                        application.put("application",
                                vendorInfo.getElementsByTagName("vendor-model").item(0).getTextContent());
                        application.put("application-vendor",
                                vendorInfo.getElementsByTagName("vendor-name").item(0).getTextContent());
                        populateSoftwareVersions(vnfcData, application, partNumber);
                    }
                }
            }
        } catch (Exception ex) {
            throw new VnfImageException(ex);
        }
        return vnfcData;
    }

    /**
     * @param vnfcData to populate
     * @param applicationData
     * @param partNumber
     */
    private void populateSoftwareVersions(List<Map<String, String>> vnfcData, Map<String, String> applicationData,
            Node partNumber) {
        NodeList nodes = partNumber.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node childNode = nodes.item(i);
            if (childNode.getNodeName().equalsIgnoreCase("software-version-list")) {
                Element softwareVersion = getFirstChildNodeByName(childNode, "software-version");
                if (softwareVersion != null) {
                    HashMap<String, String> vnfImageData = new HashMap<>(applicationData);
                    vnfImageData.put("application-version", softwareVersion.getTextContent());
                    vnfcData.add(vnfImageData);
                }
            }
        }
    }

    /**
     * @param node
     * @param childNodeName
     * @return the first child node matching the given name
     */
    private Element getFirstChildNodeByName(Node node, String childNodeName) {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node childNode = nodes.item(i);
            if (childNode.getNodeName().equalsIgnoreCase(childNodeName)) {
                return (Element) childNode;
            }
        }
        return null;
    }

}
