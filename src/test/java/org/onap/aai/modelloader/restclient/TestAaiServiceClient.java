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
package org.onap.aai.modelloader.restclient;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.write;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Local testing of the A&AI Service client.
 *
 */
public class TestAaiServiceClient {

    private Server server;
    private AaiRestClient aaiClient;

    @BeforeEach
    public void startJetty() throws Exception {
        server = new Server(0);
        server.setHandler(getMockHandler());
        server.start();

        Properties props = new Properties();
        props.put("ml.aai.KEYSTORE_PASSWORD", "2244");
        props.put("ml.aai.RESTCLIENT_CONNECT_TIMEOUT", "3000");
        props.put("ml.aai.RESTCLIENT_READ_TIMEOUT", "3000");
        ModelLoaderConfig config = new ModelLoaderConfig(props, ".");
        aaiClient = new AaiRestClient(config, new RestTemplate());
    }

    @AfterEach
    public void stopJetty() throws Exception {
        server.stop();
    }

    @Test
    public void testBuildAaiRestClient() {
        Properties props = new Properties();
        ModelLoaderConfig config = new ModelLoaderConfig(props, ".");
        new AaiRestClient(config, new RestTemplate());
        assertTrue(true);
    }

    @Test
    public void testOperations() {
        String url = server.getURI().toString();
        String transId = "";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        aaiClient.getResource(url, "", mediaType, String.class);
        aaiClient.deleteResource(url, "", transId);
        aaiClient.getAndDeleteResource(url, transId);
        aaiClient.postResource(url, "", transId, mediaType, String.class);
        aaiClient.putResource(url, "", transId, mediaType, String.class);
        assertTrue(true);
    }


    /**
     * Creates an {@link AbstractHandler handler} returning an arbitrary String as a response.
     * 
     * @return never <code>null</code>.
     */
    private Handler getMockHandler() {
        Handler handler = new AbstractHandler() {
            @Override
            public void handle(String target, Request request, HttpServletRequest servletRequest,
                    HttpServletResponse response) throws IOException, ServletException {
                response.setStatus(SC_OK);
                response.setContentType("text/json;charset=utf-8");
                write("", response.getOutputStream(), Charset.defaultCharset());
                request.setHandled(true);
            }
        };
        return handler;
    }
}

