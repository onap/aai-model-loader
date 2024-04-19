package org.onap.aai.modelloader.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AaiResourcesObject {
  @JsonProperty("resource-version")
  private String resourceVersion;
}
