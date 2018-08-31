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
package org.onap.aai.cacher.injestion.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserStrategy;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserType;
import org.onap.aai.cacher.model.CacheEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to be use to interact with parsers.
 */
@Service
public class PayloadParserService {

    private PayloadParserFactory payloadParserFactory;

    @Autowired
    public PayloadParserService(PayloadParserFactory payloadParserFactory) {
        this.payloadParserFactory = payloadParserFactory;
    }

    public List<CacheEntry> doParse(String cacheKey, JsonObject jo, PayloadParserType payloadParserType) {

        PayloadParserStrategy parser = payloadParserFactory.getPayloadParserStrategy(payloadParserType);
        return parser.process(cacheKey, jo);
    }

    public List<CacheEntry> doParse(String cacheKey, JsonObject jo, String payloadParserTypeStr) {
        return doParse(cacheKey, jo, PayloadParserType.fromString(payloadParserTypeStr));
    }

    public List<CacheEntry> doParse(String cacheKey, JsonObject jo) {
        return this.doParse(cacheKey, jo, PayloadParserType.NONE);
    }

    public List<CacheEntry> doParse(String cacheKey, String jo, PayloadParserType payloadParserType) {
        JsonParser jsonParser = new JsonParser();
        return doParse(cacheKey, jsonParser.parse(jo).getAsJsonObject(), payloadParserType);
    }

    public List<CacheEntry> doParse(String cacheKey, String jo, String payloadParserTypeStr) {
        return doParse(cacheKey, jo, PayloadParserType.fromString(payloadParserTypeStr));
    }

    public List<CacheEntry> doParse(String cacheKey, String jo) {
        return this.doParse(cacheKey, jo, PayloadParserType.NONE);
    }
}