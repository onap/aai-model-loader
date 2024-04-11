package org.onap.aai.modelloader.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.service.ArtifactInfoImpl;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

@SpringBootTest(properties = { "ml.distribution.connection.enabled=true" })
@AutoConfigureWireMock(port = 0)
public class ArtifactDownloadManagerTest {

  @Value("${wiremock.server.port}")
  private int wiremockPort;

  @Autowired ArtifactDownloadManager artifactDownloadManager;

  @Test
  @Disabled
  public void downloadArtifacts() {

    // requestHeaders.put("X-ECOMP-RequestID", requestId);
    // requestHeaders.put("X-ECOMP-InstanceID", this.configuration.getConsumerID());
    // requestHeaders.put("Accept", ContentType.APPLICATION_OCTET_STREAM.toString());

    NotificationDataImpl notificationData = new NotificationDataImpl();
    notificationData.setDistributionID("distributionID");
    ArtifactInfoImpl artifactInfo = new ArtifactInfoImpl();
    artifactInfo.setArtifactVersion("1.0");
    List<IArtifactInfo> artifacts = new ArrayList<>();
    artifacts.add(artifactInfo);
    List<Artifact> modelArtifacts = new ArrayList<>(); // processed artifacts will be written to this list
    boolean result = artifactDownloadManager.downloadArtifacts(notificationData, artifacts, modelArtifacts, null);
    assertTrue(result);
  }

}
