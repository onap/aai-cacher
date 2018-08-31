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
package org.onap.aai.cacher.dmaap.consumer;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.att.nsa.mr.client.MRConsumer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.Properties;

/**
 * Encapsulates the MRConsumer and invokes it only if the environment is dev,
 * testINT, testEXT If its one of those environments, uses the RestClient
 */
public class RestDmaapClientConsumer implements ClientConsumer {

    private MRConsumer aaiDmaapEventConsumer;

    private static final Base64.Encoder base64Encoder = Base64.getEncoder();

    private HttpHeaders httpHeaders;
    private RestTemplate restTemplate;

    private String env;
    // private String url;
    private UriComponentsBuilder builder;

    private Properties consumerProperties;

    private static EELFLogger LOGGER = EELFManager.getInstance().getLogger(RestDmaapClientConsumer.class);

    public RestDmaapClientConsumer(MRConsumer consumer, Properties aaiDmaapEventConsumerProperties) {

        env = System.getProperty("lrmRO");

        this.aaiDmaapEventConsumer = consumer;
        this.consumerProperties = aaiDmaapEventConsumerProperties;

    }

    /**
     * Checks if the environment is null or if the environment starts with dev,
     * testEXT, or testINT and then if that is the case, it makes a request to the
     * url to subscribe to that topic to retrieve all messages there If it is not
     * one of those environments, then it will call the default fetch of messages
     * from the MR Consumer
     *
     * @return a list of messages from the topic
     * @throws Exception
     */
    @Override
    public Iterable<String> process() throws Exception {

        Iterable<String> messages = aaiDmaapEventConsumer.fetch();

        LOGGER.debug("Finished the consumption of messages from dmaap");
        return messages;
    }
}
