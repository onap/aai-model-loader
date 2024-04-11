package org.onap.aai.modelloader.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.entity.Artifact;
import org.onap.aai.modelloader.entity.model.ModelArtifact;
import org.onap.aai.modelloader.entity.model.ModelArtifactHandler;
import org.onap.aai.modelloader.restclient.AaiRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.ArrayList;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
public class ModelArtifactHandlerTest {

  @Value("${wiremock.server.port}")
  private int wiremockPort;

  final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired AaiRestClient restClient;
  @Mock ModelLoaderConfig config;
  @InjectMocks ModelArtifactHandler modelArtifactHandler;
  // @Autowired ModelArtifactHandler modelArtifactHandler;

  @BeforeEach
  public void setUp() {
    when(config.getAaiBaseUrl()).thenReturn("http://localhost:" + wiremockPort);
    when(config.getAaiModelUrl(any())).thenReturn("/aai/v28/service-design-and-creation/models/model/");
  }
  
  @Test
  public void thatArtifactsCanBePushed() {
    WireMock.stubFor(
      WireMock.get(WireMock.urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId"))
          // .withHeader("some", new EqualToPattern("header"))
          .willReturn(
              WireMock.aResponse()
                  .withStatus(HttpStatus.NOT_FOUND.value())));
                  // .withHeader("Content-Type", "application/json")
                  // .withBody("{}")));
    WireMock.stubFor(
      WireMock.put(WireMock.urlEqualTo("/aai/v28/service-design-and-creation/models/model/modelInvariantId"))
          .withHeader("Content-Type", WireMock.equalTo("application/xml"))
          .withHeader("X-TransactionId", WireMock.equalTo("someId"))
          .withHeader("X-FromAppId", WireMock.equalTo("ModelLoader"))
          .willReturn(
              WireMock.aResponse()
                  .withStatus(HttpStatus.CREATED.value())));
                  // .withBody(objectMapper.writeValueAsString(artifacts))));
    ModelArtifact modelArtifact = new ModelArtifact();
    modelArtifact.setModelInvariantId("modelInvariantId");
    List<Artifact> artifacts = List.of(modelArtifact);
    List<Artifact> completedArtifacts = new ArrayList<>();

    boolean result = modelArtifactHandler.pushArtifacts(artifacts, "someId", completedArtifacts, restClient);
    assertTrue(result);
  }
  
}
