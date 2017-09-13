/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.modelloader.entity.catalog;

import com.sun.jersey.api.client.ClientResponse;

import generated.VnfCatalog;
import generated.VnfCatalog.PartNumberList;

import inventory.aai.openecomp.org.v8.VnfImage;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.ArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.onap.aai.modelloader.restclient.AaiRestClient.MimeType;
import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.springframework.web.util.UriUtils;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;


public class VnfCatalogArtifactHandler extends ArtifactHandler {

  private static Logger logger = LoggerFactory.getInstance()
      .getLogger(VnfCatalogArtifactHandler.class.getName());

  public VnfCatalogArtifactHandler(ModelLoaderConfig config) {
    super(config);
  }

  @Override
  public boolean pushArtifacts(List<Artifact> artifacts, String distributionId) {
    for (Artifact art : artifacts) {
      VnfCatalogArtifact vnfCatalog = (VnfCatalogArtifact) art;
      String artifactPayload = vnfCatalog.getPayload();

      AaiRestClient restClient = new AaiRestClient(this.config);
      List<VnfImage> putImages = new ArrayList<VnfImage>();

      try {
        JAXBContext inputContext = JAXBContext.newInstance(VnfCatalog.class);
        Unmarshaller unmarshaller = inputContext.createUnmarshaller();
        StringReader reader = new StringReader(artifactPayload);
        VnfCatalog cat = (VnfCatalog) unmarshaller.unmarshal(reader);

        int numParts = cat.getPartNumberList().size();

        for (int i = 0; i < numParts; i++) {

          PartNumberList pnl = cat.getPartNumberList().get(i);

          String application = pnl.getVendorInfo().getVendorModel();
          String applicationVendor = pnl.getVendorInfo().getVendorName();

          int numVersions = pnl.getSoftwareVersionList().size();

          for (int j = 0; j < numVersions; j++) {
            String applicationVersion = pnl.getSoftwareVersionList().get(j).getSoftwareVersion();

            String imageId = "vnf image " + applicationVendor + " " + application + " "
                + applicationVersion;

			String queryURI = "application-vendor=" + applicationVendor + "&application=" + application + "&application-version=" + applicationVersion;
			
			String getUrl = config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "?" + UriUtils.encodePath(queryURI, "UTF-8");

            ClientResponse tryGet = restClient.getResource(getUrl, distributionId, MimeType.JSON);
            if (tryGet == null) {
              logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
                  "Ingestion failed on " + imageId + ". Rolling back distribution.");
              failureCleanup(putImages, restClient, distributionId);
              return false;
            }
            if (tryGet.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
              // this vnf-image not already in the db, need to add
              // only do this on 404 bc other error responses could mean there
              // are problems that
              // you might not want to try to PUT against

              VnfImage image = new VnfImage();
              image.setApplication(application);
              image.setApplicationVendor(applicationVendor);
              image.setApplicationVersion(applicationVersion);
              String uuid = UUID.randomUUID().toString();
              image.setUuid(uuid); // need to create uuid

              System.setProperty("javax.xml.bind.context.factory",
                  "org.eclipse.persistence.jaxb.JAXBContextFactory");
              JAXBContext jaxbContext = JAXBContext.newInstance(VnfImage.class);
              Marshaller marshaller = jaxbContext.createMarshaller();
              marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
              marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
              marshaller.setProperty(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
              marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
              StringWriter writer = new StringWriter();
              marshaller.marshal(image, writer);
              String payload = writer.toString();

              String putUrl = config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "/vnf-image/"
                  + uuid;

              ClientResponse putResp = restClient.putResource(putUrl, payload, distributionId,
                  MimeType.JSON);
              if (putResp == null
                  || putResp.getStatus() != Response.Status.CREATED.getStatusCode()) {
                logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
                    "Ingestion failed on vnf-image " + imageId + ". Rolling back distribution.");
                failureCleanup(putImages, restClient, distributionId);
                return false;
              }
              putImages.add(image);
              logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT, imageId + " successfully ingested.");
            } else if (tryGet.getStatus() == Response.Status.OK.getStatusCode()) {
              logger.info(ModelLoaderMsgs.DISTRIBUTION_EVENT,
                  imageId + " already exists.  Skipping ingestion.");
            } else {
              // if other than 404 or 200, something went wrong
              logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
                  "Ingestion failed on vnf-image " + imageId + " with status " + tryGet.getStatus()
                      + ". Rolling back distribution.");
              failureCleanup(putImages, restClient, distributionId);
              return false;
            }
          }
        }

      } catch (JAXBException e) {
        logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR,
            "Ingestion failed. " + e.getMessage() + ". Rolling back distribution.");
        failureCleanup(putImages, restClient, distributionId);
        return false;
      } catch (UnsupportedEncodingException e) {
    	  logger.error(ModelLoaderMsgs.DISTRIBUTION_EVENT_ERROR, "Ingestion failed. " + e.getMessage() + ". Rolling back distribution.");
    	  failureCleanup(putImages, restClient, distributionId);
    	  return false;
      }
    }

    return true;
  }

  /*
   * if something fails in the middle of ingesting the catalog we want to
   * rollback any changes to the db
   */
  private void failureCleanup(List<VnfImage> putImages, AaiRestClient restClient, String transId) {
    for (VnfImage image : putImages) {
      String url = config.getAaiBaseUrl() + config.getAaiVnfImageUrl() + "/vnf-image/"
          + image.getUuid();
      restClient.getAndDeleteResource(url, transId); // try to delete the image,
                                                     // if something goes wrong
                                                     // we can't really do
                                                     // anything here
    }
  }

}
