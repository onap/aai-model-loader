package org.onap.aai.modelloader.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ActuatorTest {
  
  @Autowired RestTemplate restTemplate;
  @LocalServerPort
  private int serverPort;

  @Test
  public void thatLivenessEndpointReturnsOk() {
    String url = String.format("http://localhost:%s/actuator/health", serverPort);
    ResponseEntity<String> entity = restTemplate.getForEntity(url, String.class);
    assertEquals(entity.getStatusCode(), HttpStatus.OK);
    assertEquals(entity.getBody(), "{\"status\":\"UP\"}");
  }
}
