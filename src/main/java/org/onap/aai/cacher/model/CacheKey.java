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
        CacheKey cacheKey = gson.fromJson(payload.toString(), CacheKey.class);

        if (cacheKey.cacheKey == null) {
            cacheKey.cacheKey = DEFAULT_VALUE;
            if (payload.has("_id")) {
                cacheKey.cacheKey = payload.get("_id").getAsString();
            }
        }
        if (cacheKey.baseUrl == null) {
            cacheKey.baseUrl = DEFAULT_VALUE;
        }
        if (cacheKey.module == null) {
            cacheKey.module = DEFAULT_VALUE;
        }
        if (cacheKey.URI == null) {
            cacheKey.URI = DEFAULT_VALUE;
        }
        if (cacheKey.syncInterval == null) {
            cacheKey.syncInterval = DEFAULT_VALUE;
        }
        if (cacheKey.lastSyncStartTime == null) {
            cacheKey.lastSyncStartTime = DEFAULT_VALUE;
        }
        if (cacheKey.lastSyncSuccessTime == null) {
            cacheKey.lastSyncSuccessTime = DEFAULT_VALUE;
        }
        if (cacheKey.lastSyncEndTime == null) {
            cacheKey.lastSyncEndTime = DEFAULT_VALUE;
        }
        if (cacheKey.httpBody == null) {
            cacheKey.httpBody = DEFAULT_VALUE;
        }
        if (cacheKey.parserStrategy == null) {
            cacheKey.parserStrategy = DEFAULT_VALUE;
        }
        if (cacheKey.timingIndicator == null) {
            cacheKey.timingIndicator = DEFAULT_VALUE;
        }
        if (cacheKey.httpMethod == null) {
            cacheKey.httpMethod = DEFAULT_VALUE;
        }
        return cacheKey;
    }

    public static CacheKey fromJson(JsonObject payload) {
        CacheKey cacheKey = createCacheKeyDefault(payload);
        if (DEFAULT_VALUE.equals(cacheKey.parserStrategy)) {
            cacheKey.parserStrategy = "none";
        }
        if (DEFAULT_VALUE.equals(cacheKey.timingIndicator)) {
            cacheKey.timingIndicator = "firstHit";
        } else if (cacheKey.getTimingIndicator().equals("scheduled") && DEFAULT_VALUE.equals(cacheKey.syncInterval)) {
            cacheKey.syncInterval = "1440";
        }
        if (DEFAULT_VALUE.equals(cacheKey.httpMethod)) {
            cacheKey.httpMethod = "GET";
        }
        return cacheKey;
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
        StringBuilder sb = new StringBuilder();

        sb.append("cacheKey: " + cacheKey + "\n");
        sb.append("Base URL: " + baseUrl + "\n");
        sb.append("Module: " + module + "\n");
        sb.append("URI: " + URI + "\n");
        sb.append("timingIndicator: " + timingIndicator + "\n");
        sb.append("syncInterval: " + syncInterval + "\n");
        sb.append("lastSyncStartTime: " + lastSyncStartTime + "\n");
        sb.append("lastSyncSuccessTime: " + lastSyncSuccessTime + "\n");
        sb.append("lastSyncEndTime: " + lastSyncEndTime + "\n");
        sb.append("httpMethod: " + httpMethod + "\n");
        sb.append("httpBody: " + httpBody + "\n");
        sb.append("parserStrategy: " + parserStrategy + "\n");

        return sb.toString();
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