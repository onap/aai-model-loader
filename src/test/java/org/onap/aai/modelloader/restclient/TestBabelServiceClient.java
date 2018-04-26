/**
 * ﻿============LICENSE_START=======================================================
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.babel.service.data.BabelArtifact;
import org.onap.aai.modelloader.config.ModelLoaderConfig;
import org.onap.aai.modelloader.restclient.BabelServiceClient.BabelServiceException;

/**
 * Local testing of the Babel service
 *
 */
public class TestBabelServiceClient {

    private Server server;
    private String responseBody;

    {
        List<BabelArtifact> response = new ArrayList<>();
        response.add(new BabelArtifact("", null, ""));
        response.add(new BabelArtifact("", null, ""));
        response.add(new BabelArtifact("", null, ""));
        responseBody = new Gson().toJson(response);
    }

    @Before
    public void startJetty() throws Exception {
        server = new Server(8080);
        server.setHandler(getMockHandler());
        server.start();
    }

    @After
    public void stopJetty() throws Exception {
        server.stop();
    }

    @Test
    public void testRestClient() throws UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException,
            KeyStoreException, CertificateException, IOException, BabelServiceException, URISyntaxException {
        Properties configProperties = new Properties();
        configProperties.put("ml.babel.KEYSTORE_PASSWORD", "OBF:1vn21ugu1saj1v9i1v941sar1ugw1vo0");
        configProperties.put("ml.babel.BASE_URL", "http://localhost:8080/");
        configProperties.put("ml.babel.GENERATE_ARTIFACTS_URL", "generate");
        BabelServiceClient client =
                new BabelServiceClientFactory().create(new ModelLoaderConfig(configProperties, "."));
        List<BabelArtifact> result =
                client.postArtifact(readBytesFromFile("compressedArtifacts/service-VscpaasTest-csar.csar"),
                        "service-Vscpass-Test", "1.0", "Test-Transaction-ID-BabelClient");
        assertThat(result.size(), is(equalTo(3)));
    }

    private byte[] readBytesFromFile(String resourceFile) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(resourceFile).toURI()));
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
                response.setContentType("text/xml;charset=utf-8");
                write(responseBody, response.getOutputStream());
                request.setHandled(true);
            }
        };
        return handler;
    }
}

