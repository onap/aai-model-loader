/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.aai.modelloader.entity.model;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.onap.aai.modelloader.service.ModelLoaderMsgs;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class ModelParserFactory {
  private static Logger logger = LoggerFactory.getInstance().getLogger(ModelParserFactory.class.getName());
  
  private static String MODEL_ELEMENT = "model";
  private static String NAMED_QUERY_ELEMENT = "named-query";
  
	public static IModelParser createModelParser(byte[] artifactPayload, String artifactName) {
	  Document doc = null;

	  try {
	    String payload = new String(artifactPayload);
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder;
	    builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(payload));
	    doc = builder.parse(is);
	  } catch (Exception e) {
	    logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName);
	    return null;
	  }

    if (doc.getDocumentElement().getNodeName().equalsIgnoreCase(NAMED_QUERY_ELEMENT)) {
      return new NamedQueryArtifactParser();
    }
    
    if (!doc.getDocumentElement().getNodeName().equalsIgnoreCase(MODEL_ELEMENT)) {
      logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName 
          + ": Invalid root element: " + doc.getDocumentElement().getNodeName());
      return null;
    }
    
    Element e = doc.getDocumentElement();
    String ns = e.getAttribute("xmlns");
    String[] parts = ns.split("/");
    
    if (parts.length < 1) {
      logger.error(ModelLoaderMsgs.ARTIFACT_PARSE_ERROR, "Unable to parse artifact " + artifactName 
          + ": Could not parse namespace version");
      return null;
    }
    
    String modelNamespaceVersion = parts[parts.length-1].trim().replace("v", "");
    int version = Integer.parseInt(modelNamespaceVersion);
    
    if (version > 8) {
      return new ModelArtifactParser();
    }
    
    return new ModelV8ArtifactParser(); 
	}
}
