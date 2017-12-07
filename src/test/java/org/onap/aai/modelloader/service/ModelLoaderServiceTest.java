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
package org.onap.aai.modelloader.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ModelLoaderServiceTest {

    ModelLoaderService service;
    @Before
    public void init() throws IOException, NoSuchFieldException, IllegalAccessException {
        System.setProperty("AJSC_HOME", new File(".").getCanonicalPath().replace('\\', '/'));
        setFinalStatic(System.getProperty("AJSC_HOME")+"/src/test/resources/model-loader.properties");
        service = new ModelLoaderService();
    }

    @Test
    public void testLoadModel(){
        Response response = service.loadModel("model-1");
        Assert.assertNotNull(response);
    }

    @Test
    public void testSaveModel(){
        Response response = service.saveModel("model-1", "name-1");
        Assert.assertNotNull(response);
    }

    @Test
    public void testIngestModel() throws IOException {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Response response = service.ingestModel("model-id-1", req, "payload");
        Assert.assertNotNull(response);
    }

    static void setFinalStatic(String fieldValue) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field configField = ModelLoaderService.class.getDeclaredField("CONFIG_FILE");
        configField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField( "modifiers" );
        modifiersField.setAccessible( true );
        modifiersField.setInt( configField, configField.getModifiers() & ~Modifier.FINAL );

        configField.set(null, fieldValue);

        Field authField = ModelLoaderService.class.getDeclaredField("CONFIG_AUTH_LOCATION");
        authField.setAccessible(true);

        Field modifiersField1 = Field.class.getDeclaredField( "modifiers" );
        modifiersField1.setAccessible( true );
        modifiersField1.setInt( authField, authField.getModifiers() & ~Modifier.FINAL );

        authField.set(null, System.getProperty("AJSC_HOME"));
    }
}
