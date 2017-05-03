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
package org.openecomp.modelloader.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

public class JsonXmlConverter {

  /**
   * Determines whether or not the supplied text string represents a valid
   * JSON structure or not.
   * 
   * @param text - The text to be evaluated.
   * 
   * @return - true if the string represents a valid JSON object,
   *           false, otherwise.
   */
  public static boolean isValidJson(String text) {
    try {
      new JSONObject(text);
    } catch (JSONException ex) {
      try {
        new JSONArray(text);
      } catch (JSONException ex1) {
        return false;
      }
    }

    return true;
  }

  /**
   * Takes a text string representing a valid JSON structure and converts it to
   * an equivalent XML string.
   * 
   * @param jsonText - The JSON string to convert to XML.
   * 
   * @return - An XML string representation of the supplied JSON string.
   */
  public static String convertJsonToXml(String jsonText) {
    JSONObject jsonObj = new JSONObject(jsonText);
    String xmlText = XML.toString(jsonObj);
    return xmlText;
  }

  /**
   * Takes a text string representing a valid XML structure and converts it to
   * an equivalent JSON string.
   * 
   * @param xmlText - The XML string to convert to JSON.
   * 
   * @return - A JSON string representation of the supplied XML string.
   */
  public static String convertXmlToJson(String xmlText) {
    JSONObject jsonObj = XML.toJSONObject(xmlText);
    return jsonObj.toString();
  }
}
