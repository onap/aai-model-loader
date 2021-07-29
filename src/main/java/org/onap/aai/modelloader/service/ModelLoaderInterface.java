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

import java.io.IOException;
import javax.ws.rs.core.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

public interface ModelLoaderInterface {

    @GetMapping(value = "/loadModel/{modelid}", produces = "application/json")
    @ResponseBody
    Response loadModel(String modelid);

    @PutMapping(value = "/saveModel/{modelid}/{modelname}", produces = "application/json")
    @ResponseBody
    Response saveModel(String modelid, String modelname);

    @PostMapping(value = "/ingestModel/{modelName}/{modelVersion}", produces = "application/json")
    @ResponseBody
    Response ingestModel(String modelid, String modelVersion, String payload) throws IOException;
}
