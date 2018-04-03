/**
 * ============LICENSE_START==========================================
 * org.onap.aai
 * ===================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 */

package org.onap.aai.modelloader.entity.model;

/**
 * Defines methods for c
 */
public interface IModelId {

    /**
     * This method is responsible for using the values in the supplied Pair to set the id of the model.
     *
     * The definition of what the relationship will be is defined by the implementation.  Some model ids would have
     * single key/value pairs, others would have a composite key.
     *
     * Where the id of the model is a composite key multiple calls to this method will be required to successfully set
     * the relationship in order to meet the rules of {@link #defined}
     *
     * @param pair object representing a key and its value.
     */
    void setRelationship(Pair<String, String> pair);

    /**
     * This method indicates whether the id of the model has been defined according to the rules of the specific model
     * implemented.
     *
     * Usually defined means that all properties that make up the key have been set.
     *
     * @return boolean <code>true</code> if the id of the model has been defined otherwise <code>false</code>
     */
    boolean defined();
}
