package org.onap.aai.modelloader.entity.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes the model returned by aai-resources
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Model {

  @JsonProperty("model-invariant-id")
  private String modelInvariantId;

  @JsonProperty("model-role")
  private String modelRole;

  @JsonProperty("data-owner")
  private String dataOwner;

  @JsonProperty("data-source")
  private String dataSource;

  @JsonProperty("data-source-version")
  private String dataSourceVersion;

  @JsonProperty("resource-version")
  private String resourceVersion;
}
