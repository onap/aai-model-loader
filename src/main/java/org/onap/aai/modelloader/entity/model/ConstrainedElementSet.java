package org.onap.aai.modelloader.entity.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConstrainedElementSet {
  @JsonProperty("constrained-element-set-uuid")
  private String constrainedElementSetUuid;

  @JsonProperty("data-owner")
  private String dataOwner;

  @JsonProperty("data-source")
  private String dataSource;

  @JsonProperty("data-source-version")
  private String dataSourceVersion;

  @JsonProperty("constraint-type")
  private String constraintType;

  @JsonProperty("check-type")
  private String checkType;

  @JsonProperty("resource-version")
  private String resourceVersion;

  @JsonProperty("element-choice-sets")
  private List<ElementChoiceSet> elementChoiceSets;
}
