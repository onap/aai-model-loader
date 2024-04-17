package org.onap.aai.modelloader.babel;

import java.util.List;

import org.onap.aai.babel.service.data.BabelArtifact;

import lombok.Value;

@Value
public class BabelArtifactResponse {
  List<BabelArtifact> babelArtifactList;
}
