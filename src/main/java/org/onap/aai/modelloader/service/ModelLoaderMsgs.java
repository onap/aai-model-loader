/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 European Software Marketing Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.modelloader.service;

import com.att.eelf.i18n.EELFResourceManager;
import org.onap.aai.cl.eelf.LogMessageEnum;

public enum ModelLoaderMsgs implements LogMessageEnum {

    /**
     * Arguments: None.
     */
    LOADING_CONFIGURATION,

    /**
     * Arguments: None.
     */
    STOPPING_CLIENT,

    /**
     * Arguments: {0} = message.
     */
    INITIALIZING,

    /**
     * Arguments: {0} = reason.
     */
    ASDC_CONNECTION_ERROR,

    /**
     * Arguments: {0} = message.
     */
    DISTRIBUTION_EVENT,

    /**
     * Arguments: {0} = error message.
     */
    DISTRIBUTION_EVENT_ERROR,

    /**
     * Arguments: {0} = request type. {1} = endpoint. {2} = result code.
     */
    AAI_REST_REQUEST_SUCCESS,

    /**
     * Arguments: {0} = request type. {1} = endpoint. {2} = result code. {3} = result. message
     */
    AAI_REST_REQUEST_UNSUCCESSFUL,

    /**
     * Arguments: {0} = request type. {1} = endpoint. {2} = error message.
     */
    AAI_REST_REQUEST_ERROR,

    /**
     * Arguments: {0} = request type. {1} = endpoint. {2} = error message.
     */
    BABEL_REST_REQUEST_ERROR,

    /**
     * Arguments: {0} = info request payload.
     **/
    AAI_REST_REQUEST_PAYLOAD,

    /**
     * Arguments: {0} = artifact name
     */
    ARTIFACT_PARSE_ERROR,

    /**
     * Arguments: {0} = info request for metrics.
     **/
    BABEL_REST_REQUEST,

    /**
     * Arguments: {0} = info request details.
     **/
    BABEL_REST_REQUEST_PAYLOAD,

    /**
     * Arguments: {0} = info Babel response payload.
     **/
    BABEL_RESPONSE_PAYLOAD,

    /**
     * Arguments: {0} = artifact name. {1} = payload.
     */
    DOWNLOAD_COMPLETE,

    /**
     * Arguments: {0} = event. {1} = artifact name. {2} = result.
     */
    EVENT_PUBLISHED,

    /**
     * Arguments: {0} = artifact name. {1} = artifact type.
     */
    UNSUPPORTED_ARTIFACT_TYPE,

    /**
     * Arguments: {0} = artifact name.
     */
    DUPLICATE_VNFC_DATA_ERROR;

    /**
     * Load message bundle (ModelLoaderMsgs.properties file)
     */
    static {
        EELFResourceManager.loadMessageBundle("org/onap/aai/modelloader/service/ModelLoaderMsgs");
    }

}
