/*-
 * ============LICENSE_START=======================================================
 * MODEL LOADER SERVICE
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.modelloader.entity.model;

import org.openecomp.modelloader.entity.Artifact;

import java.util.HashSet;
import java.util.Set;

public class ModelArtifact extends Artifact {

  String nameVersionId;
  Set<String> referencedModelIds = new HashSet<String>();

  public String getNameVersionId() {
    return nameVersionId;
  }

  public void setNameVersionId(String nameVersionId) {
    this.nameVersionId = nameVersionId;
  }

  public Set<String> getDependentModelIds() {
    return referencedModelIds;
  }

  public void addDependentModelId(String dependentModelId) {
    this.referencedModelIds.add(dependentModelId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("NameVersId=" + nameVersionId + "(" + getType().toString() + ") ==> ");
    for (String dep : referencedModelIds) {
      sb.append(dep + "  ");
    }

    return sb.toString();
  }

}
