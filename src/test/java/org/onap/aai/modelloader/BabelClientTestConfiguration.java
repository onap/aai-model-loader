package org.onap.aai.modelloader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class BabelClientTestConfiguration {
  @Value("${CONFIG_HOME}")
  private String configDir;

  @Value("${wiremock.server.port}")
  private int wiremockPort;

  @Primary
  @Bean(name = "testProperties")
  public Properties configProperties() throws IOException {
    // Load model loader system configuration
    InputStream configInputStream = Files.newInputStream(Paths.get(configDir, "model-loader.properties"));
    Properties configProperties = new Properties();
    configProperties.load(configInputStream);

    setOverrides(configProperties);

    return configProperties;
  }

  private void setOverrides(Properties configProperties) {
    configProperties.setProperty("ml.babel.BASE_URL", "http://localhost:" + wiremockPort);
  }
}
