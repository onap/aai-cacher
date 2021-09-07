/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
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
package org.onap.aai.cacher.util;

import java.nio.charset.StandardCharsets;
import org.apache.commons.net.util.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.util.AAIConfig;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Collections;

public class RestClient {

    private HttpClient clientSingleton = null;

    public RestClient() {
        this.clientSingleton = initClient();
    }

    public HttpClient getRestClient() {
        return clientSingleton;
    }

    /**
     * @return initialized rest client
     *
     */
    private HttpClient initClient() {
        HttpClient rc;
        try {
            String truststorePath = AAIConstants.AAI_HOME_ETC_AUTH
                    + AAIConfig.get(AAIConstants.AAI_TRUSTSTORE_FILENAME);
            String truststorePassword = AAIConfig.get(AAIConstants.AAI_TRUSTSTORE_PASSWD);
            String keystorePath = AAIConstants.AAI_HOME_ETC_AUTH + AAIConfig.get(AAIConstants.AAI_KEYSTORE_FILENAME);
            String keystorePassword = AAIConfig.get(AAIConstants.AAI_KEYSTORE_PASSWD);
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadKeyMaterial(loadPfx(keystorePath, keystorePassword.toCharArray()),
                            keystorePassword.toCharArray())
                    .loadTrustMaterial(ResourceUtils.getFile(truststorePath), truststorePassword.toCharArray())
                    .build();

            rc = HttpClients.custom().setSSLContext(sslContext).setSSLHostnameVerifier((s, sslSession) -> true).build();
        } catch (Exception e) {
            // TODO Handle exceptions/logging
            rc = null;
        }
        return rc;
    }
    
    private String getAuth(String baseUrl ) {
        int startIndex = baseUrl.indexOf("://") + "://".length();
        int ampersandIndex = baseUrl.indexOf('@');
        if ( ampersandIndex >= 0 ) {
            return baseUrl.substring(startIndex, ampersandIndex);
        }
        return null;
    }

    public ResponseEntity get(String baseUrl, String module, String restUri, String sourceName) throws Exception {
        ResponseEntity responseEntity = null;
        try {

            RestTemplate restTemplate = restTemplate(new RestTemplateBuilder());
            String endpoint;
            if (!module.equals("-1")) {
                endpoint = module + restUri;
            } else {
                endpoint = restUri;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-FromAppId", "AAI-CACHER");
            headers.add("X-TransactionId", "JUNIT");
            String auth = getAuth(baseUrl);
            if ( auth != null ) {
                byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
                headers.add("Authorization", "Basic " + new String(encodedAuth));
            }
            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
            responseEntity = restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, httpEntity, String.class);
        } catch (Exception e) {
            ErrorLogHelper.logException(new AAIException("AAI_4000", e));
            // TODO handle exceptions
        }
        return responseEntity;
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) throws Exception {
        RestTemplate restTemplate = builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(
                clientSingleton))
                .build();

        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                return clientHttpResponse.getStatusCode() != HttpStatus.OK;
            }

            @Override
            public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
                // TODO handle the error
            }
        });

        return restTemplate;
    }

    private KeyStore loadPfx(String file, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        File key = ResourceUtils.getFile(file);
        try (InputStream in = new FileInputStream(key)) {
            keyStore.load(in, password);
        }
        return keyStore;
    }
}
