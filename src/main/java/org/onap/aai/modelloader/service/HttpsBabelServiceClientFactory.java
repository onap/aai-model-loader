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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.restclient.BabelServiceClient;
import org.onap.aai.modelloader.restclient.BabelServiceClientException;
import org.onap.aai.modelloader.restclient.HttpsBabelServiceClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class HttpsBabelServiceClientFactory implements BabelServiceClientFactory {

    /*
     * (non-Javadoc)
     * 
     * @see org.onap.aai.modelloader.service.BabelServiceClientFactory#create(org.onap.aai.modelloader.config.
     * ModelLoaderConfig)
     */
    @Override
    public BabelServiceClient create(ModelLoaderConfig config) throws BabelServiceClientException {
        try {
            return new HttpsBabelServiceClient(config);
        } catch (UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException
                | CertificateException | IOException ex) {
            throw new BabelServiceClientException(ex);
        }
    }

}
