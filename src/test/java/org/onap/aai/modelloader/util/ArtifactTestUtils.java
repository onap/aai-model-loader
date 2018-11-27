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
package org.onap.aai.modelloader.util;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.onap.aai.babel.service.data.BabelArtifact;

/**
 * This class provides some utilities to assist with running tests.
 */
public class ArtifactTestUtils {
    public static BabelArtifact loadModelArtifact(String resource) throws IOException {
        return new BabelArtifact("ModelArtifact", BabelArtifact.ArtifactType.MODEL,
                ArtifactTestUtils.loadResourceAsString(resource));
    }

    /**
     * Finds the resource with the given name and returns it as an array of bytes.
     *
     * @param resourceName
     *        the /-separated path to the resource
     * @return the requested resource contents as a byte array
     * @throws IOException
     *         if the resource could not be found (using current privileges)
     */
    public byte[] loadResource(String resourceName) throws IOException {
        URL resource = getResource(resourceName);
        if (resource != null) {
            return IOUtils.toByteArray(resource);
        } else {
            throw new IOException("Cannot locate resource: " + resourceName);
        }
    }

    public static String loadResourceAsString(String resourceName) throws IOException {
        return IOUtils.toString(getResource(resourceName));
    }

    private static URL getResource(String resourceName) {
        return ArtifactTestUtils.class.getClassLoader().getResource(resourceName);
    }

}
