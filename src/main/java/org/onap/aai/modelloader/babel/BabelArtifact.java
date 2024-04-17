package org.onap.aai.modelloader.babel;

import org.onap.aai.modelloader.entity.ArtifactType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BabelArtifact {
  String name;
  ArtifactType type;
  String payload;
}
