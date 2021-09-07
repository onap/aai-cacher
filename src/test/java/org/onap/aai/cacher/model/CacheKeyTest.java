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
package org.onap.aai.cacher.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CacheKeyTest {

    public String inputGETOnInit = "{" +
            "'cacheKey': 'cloud-region'," +
            "'baseUrl': 'http://localhost:8447'," +
            "'module': '/aai/v14/'," +
            "'URI': 'cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3'," +
            "'timingIndicator': 'onInit'," +
            "'httpMethod': 'GET'}";

    public String inputGETFirstHitDefault = "{" +
            "'cacheKey': 'cloud-region'," +
            "'baseUrl': 'http://localhost:8447'," +
            "'module': '/aai/v14/'," +
            "'URI': 'cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3'}";

    public String inputGETScheduledDefault = "{" +
            "'cacheKey': 'cloud-region'," +
            "'baseUrl': 'http://localhost:8447'," +
            "'module': '/aai/v14/'," +
            "'timingIndicator': 'scheduled'," +
            "'URI': 'cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3'}";

    public String inputGETScheduledWithSyncInterval = "{" +
            "'cacheKey': 'cloud-region'," +
            "'baseUrl': 'http://localhost:8447'," +
            "'module': '/aai/v14/'," +
            "'syncInterval': '2'," +
            "'timingIndicator': 'scheduled'," +
            "'URI': 'cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3'}";

    public String inputGETScheduledWithSyncIntervalWithId = "{" +
            "'_id': 'cloud-region'," +
            "'baseUrl': 'http://localhost:8447'," +
            "'module': '/aai/v14/'," +
            "'syncInterval': '2'," +
            "'timingIndicator': 'scheduled'," +
            "'URI': 'cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3'}";


    @Test
    public void testCacheKeyObjectGETOnInit() {
        JsonObject ckJson = (JsonObject) JsonParser.parseString(inputGETOnInit);
        CacheKey ck = CacheKey.fromJson(ckJson);
        assertEquals("cacheKey was incorrect", "cloud-region", ck.getCacheKey());
        assertEquals("baseUrl was incorrect", "http://localhost:8447", ck.getBaseUrl());
        assertEquals("Module was incorrect", "/aai/v14/", ck.getModule());
        assertEquals("URI was incorrect", "cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3", ck.getURI());
        assertEquals("timingIndicator was incorrect", "onInit", ck.getTimingIndicator());
        assertEquals("Http Method was incorrect", "GET", ck.getHttpMethod());
        assertEquals("Http Body was incorrect", "-1", ck.getHttpBody());
        assertEquals("Sync Interval was incorrect", "-1", ck.getSyncInterval());
        assertEquals("Last Sync Start Time was incorrect", "-1", ck.getLastSyncStartTime());
        assertEquals("Last Sync Success Time was incorrect", "-1", ck.getLastSyncSuccessTime());
    }

    @Test
    public void testCacheKeyObjectGETFirstHitDefault() {
        JsonObject ckJson = (JsonObject) JsonParser.parseString(inputGETFirstHitDefault);
        CacheKey ck = CacheKey.fromJson(ckJson);
        assertEquals("cacheKey was incorrect", "cloud-region", ck.getCacheKey());
        assertEquals("baseUrl was incorrect", "http://localhost:8447", ck.getBaseUrl());
        assertEquals("Module was incorrect", "/aai/v14/", ck.getModule());
        assertEquals("URI was incorrect", "cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3", ck.getURI());
        assertEquals("timingIndicator was incorrect", "firstHit", ck.getTimingIndicator());
        assertEquals("Http Method was incorrect", "GET", ck.getHttpMethod());
        assertEquals("Http Body was incorrect", "-1", ck.getHttpBody());
        assertEquals("Sync Interval was incorrect", "-1", ck.getSyncInterval());
        assertEquals("Last Sync Start Time was incorrect", "-1", ck.getLastSyncStartTime());
        assertEquals("Last Sync Success Time was incorrect", "-1", ck.getLastSyncSuccessTime());
    }

    @Test
    public void testCacheKeyObjectDefaults() {
        JsonObject ckJson = (JsonObject) JsonParser.parseString("{}");
        CacheKey ck = CacheKey.fromJson(ckJson);
        assertEquals("cacheKey was incorrect", "-1", ck.getCacheKey());
        assertEquals("baseUrl was incorrect", "-1", ck.getBaseUrl());
        assertEquals("Module was incorrect", "-1", ck.getModule());
        assertEquals("URI was incorrect", "-1", ck.getURI());
        assertEquals("timingIndicator was incorrect", "firstHit", ck.getTimingIndicator());
        assertEquals("Http Method was incorrect", "GET", ck.getHttpMethod());
        assertEquals("Http Body was incorrect", "-1", ck.getHttpBody());
        assertEquals("Sync Interval was incorrect", "-1", ck.getSyncInterval());
        assertEquals("Last Sync Start Time was incorrect", "-1", ck.getLastSyncStartTime());
        assertEquals("Last Sync Success Time was incorrect", "-1", ck.getLastSyncSuccessTime());
    }

    @Test
    public void testCacheKeyObjectGETScheduled() {
        JsonObject ckJson = (JsonObject) JsonParser.parseString(inputGETScheduledDefault);
        CacheKey ck = CacheKey.fromJson(ckJson);
        assertEquals("cacheKey was incorrect", "cloud-region", ck.getCacheKey());
        assertEquals("baseUrl was incorrect", "http://localhost:8447", ck.getBaseUrl());
        assertEquals("Module was incorrect", "/aai/v14/", ck.getModule());
        assertEquals("URI was incorrect", "cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3", ck.getURI());
        assertEquals("timingIndicator was incorrect", "scheduled", ck.getTimingIndicator());
        assertEquals("Http Method was incorrect", "GET", ck.getHttpMethod());
        assertEquals("Http Body was incorrect", "-1", ck.getHttpBody());
        assertEquals("Sync Interval was incorrect", "1440", ck.getSyncInterval());
        assertEquals("Last Sync Start Time was incorrect", "-1", ck.getLastSyncStartTime());
        assertEquals("Last Sync Success Time was incorrect", "-1", ck.getLastSyncSuccessTime());
    }

    @Test
    public void testCacheKeyObjectGETScheduledWithSyncInterval() {
        JsonObject ckJson = (JsonObject) JsonParser.parseString(inputGETScheduledWithSyncInterval);
        CacheKey ck = CacheKey.fromJson(ckJson);
        assertEquals("cacheKey was incorrect", "cloud-region", ck.getCacheKey());
        assertEquals("baseUrl was incorrect", "http://localhost:8447", ck.getBaseUrl());
        assertEquals("Module was incorrect", "/aai/v14/", ck.getModule());
        assertEquals("URI was incorrect", "cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3", ck.getURI());
        assertEquals("timingIndicator was incorrect", "scheduled", ck.getTimingIndicator());
        assertEquals("Http Method was incorrect", "GET", ck.getHttpMethod());
        assertEquals("Http Body was incorrect", "-1", ck.getHttpBody());
        assertEquals("Sync Interval was incorrect", "2", ck.getSyncInterval());
        assertEquals("Last Sync Start Time was incorrect", "-1", ck.getLastSyncStartTime());
        assertEquals("Last Sync Success Time was incorrect", "-1", ck.getLastSyncSuccessTime());
    }

    @Test
    public void testCacheKeyObjectGETScheduledWithSyncIntervalWithId() {
        JsonObject ckJson = (JsonObject) JsonParser.parseString(inputGETScheduledWithSyncIntervalWithId);
        CacheKey ck = CacheKey.fromJson(ckJson);
        assertEquals("cacheKey was incorrect", "cloud-region", ck.getCacheKey());
        assertEquals("baseUrl was incorrect", "http://localhost:8447", ck.getBaseUrl());
        assertEquals("Module was incorrect", "/aai/v14/", ck.getModule());
        assertEquals("URI was incorrect", "cloud-infrastructure/cloud-regions?depth=0&resultIndex=1&resultSize=3", ck.getURI());
        assertEquals("timingIndicator was incorrect", "scheduled", ck.getTimingIndicator());
        assertEquals("Http Method was incorrect", "GET", ck.getHttpMethod());
        assertEquals("Http Body was incorrect", "-1", ck.getHttpBody());
        assertEquals("Sync Interval was incorrect", "2", ck.getSyncInterval());
        assertEquals("Last Sync Start Time was incorrect", "-1", ck.getLastSyncStartTime());
        assertEquals("Last Sync Success Time was incorrect", "-1", ck.getLastSyncSuccessTime());
    }
}
