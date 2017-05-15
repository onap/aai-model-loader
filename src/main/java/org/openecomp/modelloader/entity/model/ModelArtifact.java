/**
 * ============LICENSE_START=======================================================
 * Model Loader
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP and OpenECOMP are trademarks
 * and service marks of AT&T Intellectual Property.
 */
package org.openecomp.modelloader.entity.model;

import org.openecomp.modelloader.entity.Artifact;
import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.Set;

public class ModelArtifact extends Artifact {

	String modelVerId;
	String modelInvariantId;
	String nameVersionId;
	String modelVerModelVersionId;
	String modelModelInvariantId;
	String modelNamespace;
	String modelNamespaceVersion;
	Set<String> referencedModelIds = new HashSet<String>(); 
	Node modelVer;
	boolean isV9Artifact = true;
	
	public boolean isV9Artifact() {
		return isV9Artifact;
	}
	
	public void setV9Artifact(boolean isV9Artifact) {
		this.isV9Artifact = isV9Artifact;
	}

	public String getModelVerModelVersionId() {
		return modelVerModelVersionId;
	}
	
	public void setModelVerModelVersionId(String modelVerModelVersionId) {
		this.modelVerModelVersionId = modelVerModelVersionId;
	}
	
	public String getModelModelInvariantId() {
		return modelModelInvariantId;
	}
	
	public void setModelModelInvariantId(String modelModelInvariantId) {
		this.modelModelInvariantId = modelModelInvariantId;
	}
	
	public String getNameVersionId() {
		return nameVersionId;
	}

	public void setNameVersionId(String nameVersionId) {
		this.nameVersionId = nameVersionId;
	}
	
	public String getModelNamespace() {
		return modelNamespace;
	}
	
	public void setModelNamespace(String modelNamespace) {
		this.modelNamespace = modelNamespace;
		
		// Get the version from the namespace (in format 'http://org.openecomp.aai.inventory/v9')
		String[] parts = modelNamespace.split("/");
		modelNamespaceVersion = parts[parts.length-1].trim();
	}
	
	public String getModelNamespaceVersion() {
	  return modelNamespaceVersion;
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
		sb.append("ModelInvariantId=" + modelInvariantId + "(" + getType().toString() + ") ==> ");
		for (String dep : referencedModelIds) {
			sb.append(dep + "  ");
		}

		return sb.toString();
	}

	public String getModelVerId() {
		return modelVerId;
	}
	
	public void setModelVerId(String modelVerId) {
		this.modelVerId = modelVerId;
	}
	
	public String getModelInvariantId() {
		return modelInvariantId;
	}
	
	public void setModelInvariantId(String modelInvariantId) {
		this.modelInvariantId = modelInvariantId;
	}
	
	public Node getModelVer() {
		return modelVer;
	}
	
	public void setModelVer(Node modelVer) {
		this.modelVer = modelVer;
	}
	
	public String getModelModelVerCombinedKey() {
	  if ( (getModelInvariantId() == null) && (getModelVerId() == null) ) {
	    return getNameVersionId();
	  }
		return getModelInvariantId() + "|" + getModelVerId();
	}
}
