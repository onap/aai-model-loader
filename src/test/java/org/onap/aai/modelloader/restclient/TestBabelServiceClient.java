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
package org.onap.aai.modelloader.restclient;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import org.junit.Ignore;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.restclient.BabelServiceClient;

/**
 * Local testing of the Babel service
 *
 */
public class TestBabelServiceClient {

    // Load properties from src/test/resources
    protected static String CONFIG_FILE = "model-loader.properties";

    // This test requires a running Babel system. To test locally, annotate with org.junit.Test
    @Ignore
    public void testRestClient() throws Exception { // NOSONAR
        Properties configProperties = new Properties();
        try {
            configProperties.load(this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE));
        } catch (IOException e) {
            fail();
        }
        BabelServiceClient client = new BabelServiceClient(new ModelLoaderConfig(configProperties, "."));
        List<BabelArtifact> result =
                client.postArtifact(readBytesFromFile("compressedArtifacts/service-VscpaasTest-csar.csar"),
                        "service-Vscpass-Test", "1.0", "Test-Transaction-ID-BabelClient");

        assertThat(result.size(), is(equalTo(3)));
    }

    private byte[] readBytesFromFile(String resourceFile) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(resourceFile).toURI()));
    }
}
