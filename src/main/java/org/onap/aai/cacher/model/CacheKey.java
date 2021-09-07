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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;

public class CacheKey {

    public static final String DEFAULT_VALUE = "-1";

    public String cacheKey = DEFAULT_VALUE;
    public String baseUrl = DEFAULT_VALUE;
    public String module = DEFAULT_VALUE;
    public String URI = DEFAULT_VALUE;
    public String timingIndicator = DEFAULT_VALUE;

    public String syncInterval = DEFAULT_VALUE;
    public String lastSyncStartTime = DEFAULT_VALUE;
    public String lastSyncSuccessTime = DEFAULT_VALUE;
    public String lastSyncEndTime = DEFAULT_VALUE;
    public String httpBody = DEFAULT_VALUE;
    public String httpMethod = DEFAULT_VALUE;

    public String parserStrategy = "none";

    public CacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public static CacheKey createCacheKeyDefault(JsonObject payload) {
        Gson gson = new Gson();
        CacheKey newCacheKey = gson.fromJson(payload.toString(), CacheKey.class);

        if (newCacheKey.cacheKey == null) {
            newCacheKey.cacheKey = DEFAULT_VALUE;
            if (payload.has("_id")) {
                newCacheKey.cacheKey = payload.get("_id").getAsString();
            }
        }
        if (newCacheKey.baseUrl == null) {
            newCacheKey.baseUrl = DEFAULT_VALUE;
        }
        if (newCacheKey.module == null) {
            newCacheKey.module = DEFAULT_VALUE;
        }
        if (newCacheKey.URI == null) {
            newCacheKey.URI = DEFAULT_VALUE;
        }
        if (newCacheKey.syncInterval == null) {
            newCacheKey.syncInterval = DEFAULT_VALUE;
        }
        if (newCacheKey.lastSyncStartTime == null) {
            newCacheKey.lastSyncStartTime = DEFAULT_VALUE;
        }
        if (newCacheKey.lastSyncSuccessTime == null) {
            newCacheKey.lastSyncSuccessTime = DEFAULT_VALUE;
        }
        if (newCacheKey.lastSyncEndTime == null) {
            newCacheKey.lastSyncEndTime = DEFAULT_VALUE;
        }
        if (newCacheKey.httpBody == null) {
            newCacheKey.httpBody = DEFAULT_VALUE;
        }
        if (newCacheKey.parserStrategy == null) {
            newCacheKey.parserStrategy = DEFAULT_VALUE;
        }
        if (newCacheKey.timingIndicator == null) {
            newCacheKey.timingIndicator = DEFAULT_VALUE;
        }
        if (newCacheKey.httpMethod == null) {
            newCacheKey.httpMethod = DEFAULT_VALUE;
        }
        return newCacheKey;
    }

    public static CacheKey fromJson(JsonObject payload) {
        CacheKey cacheKeyDefault = createCacheKeyDefault(payload);
        if (DEFAULT_VALUE.equals(cacheKeyDefault.parserStrategy)) {
            cacheKeyDefault.parserStrategy = "none";
        }
        if (DEFAULT_VALUE.equals(cacheKeyDefault.timingIndicator)) {
            cacheKeyDefault.timingIndicator = "firstHit";
        } else if (cacheKeyDefault.getTimingIndicator().equals("scheduled") && DEFAULT_VALUE.equals(cacheKeyDefault.syncInterval)) {
            cacheKeyDefault.syncInterval = "1440";
        }
        if (DEFAULT_VALUE.equals(cacheKeyDefault.httpMethod)) {
            cacheKeyDefault.httpMethod = "GET";
        }
        return cacheKeyDefault;
    }

    public BasicDBObject toDBObject() {
        BasicDBObject document = new BasicDBObject();
        document.put("_id", this.cacheKey);
        document.put("baseUrl", this.baseUrl);
        document.put("module", this.module);
        document.put("URI", this.URI);
        document.put("timingIndicator", this.timingIndicator);
        document.put("syncInterval", this.syncInterval);
        document.put("lastSyncStartTime", this.lastSyncStartTime);
        document.put("lastSyncSuccessTime", this.lastSyncSuccessTime);
        document.put("lastSyncEndTime", this.lastSyncEndTime);
        document.put("httpBody", this.httpBody);
        document.put("httpMethod", this.httpMethod);
        document.put("parserStrategy", parserStrategy);
        return document;
    }

    @Override
    public String toString() {

        String sb = "cacheKey: " + cacheKey + "\n" +
            "Base URL: " + baseUrl + "\n" +
            "Module: " + module + "\n" +
            "URI: " + URI + "\n" +
            "timingIndicator: " + timingIndicator + "\n" +
            "syncInterval: " + syncInterval + "\n" +
            "lastSyncStartTime: " + lastSyncStartTime + "\n" +
            "lastSyncSuccessTime: " + lastSyncSuccessTime + "\n" +
            "lastSyncEndTime: " + lastSyncEndTime + "\n" +
            "httpMethod: " + httpMethod + "\n" +
            "httpBody: " + httpBody + "\n" +
            "parserStrategy: " + parserStrategy + "\n";

        return sb;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModule() {
        return module;
    }

    public String getURI() {
        return URI;
    }

    public String getTimingIndicator() {
        return timingIndicator;
    }

    public String getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(String syncInterval) {
        this.syncInterval = syncInterval;
    }

    public String getLastSyncStartTime() {
        return lastSyncStartTime;
    }

    public void setLastSyncStartTime(String ls) {
        this.lastSyncStartTime = ls;
    }

    public String getLastSyncSuccessTime() {
        return lastSyncSuccessTime;
    }

    public void setLastSyncSuccessTime(String ls) {
        this.lastSyncSuccessTime = ls;
    }

    public String getLastSyncEndTime() {
        return lastSyncEndTime;
    }

    public void setLastSyncEndTime(String le) {
        this.lastSyncEndTime = le;
    }

    public String getHttpBody() {
        return httpBody;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getParserStrategy() {
        return parserStrategy;
    }

    public void setParserStrategy(String parserStrategy) {
        this.parserStrategy = parserStrategy;
    }

}