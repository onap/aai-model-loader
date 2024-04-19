package org.onap.aai.modelloader.entity.vnf;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VnfImage {
  @JsonProperty("vnf-image-uuid")
  private String vnfImageUuid;

  private String application;

  @JsonProperty("application-vendor")
  private String applicationVendor;

  @JsonProperty("application-version")
  private String applicationVersion;

  private String selfLink;

  @JsonProperty("data-owner")
  private String dataOwner;

  @JsonProperty("data-source")
  private String dataSource;

  @JsonProperty("data-source-version")
  private String dataSourceVersion;

  @JsonProperty("resource-version")
  private String resourceVersion;
}
