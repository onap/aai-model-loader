/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.modelloader.restclient;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class BabelServiceClientExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String expectedMessage = "This is a test error message";
        BabelServiceClientException exception = new BabelServiceClientException(expectedMessage);
        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage(), "The exception message should match.");
    }

    @Test
    public void testConstructorWithException() {
        Exception underlyingException = new Exception("Underlying exception");
        BabelServiceClientException exception = new BabelServiceClientException(underlyingException);

        // Assert
        assertNotNull(exception);
        assertEquals(underlyingException, exception.getCause(), "The cause should match the passed exception.");
    }
}
