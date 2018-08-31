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
package org.onap.aai.cacher.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.onap.aai.cacher.model.CacheKey;

import java.util.ArrayList;
import java.util.List;

public class CacheKeyConfig {
    private JsonArray cachekeys = null;

    private static final String CACHEKEYS = "cachekeys";

    public CacheKeyConfig(String json) {
        init(json);
    }

    private void init(String json) {
        JsonParser parser = new JsonParser();
        JsonObject queriesObject = parser.parse(json).getAsJsonObject();
        if (queriesObject.has(CACHEKEYS)) {
            cachekeys = queriesObject.getAsJsonArray(CACHEKEYS);
        }
    }

    public List<CacheKey> populateCacheKeyList() {
        List<CacheKey> ckList = new ArrayList<>();
        for (JsonElement cacheKeyElement : cachekeys) {
            if (cacheKeyElement.isJsonObject()) {
                JsonObject cacheJsonObj = cacheKeyElement.getAsJsonObject();
                if (cacheJsonObj != null) {
                    CacheKey cacheKey = CacheKey.fromJson(cacheJsonObj);
                    ckList.add(cacheKey);
                }
            }
        }
        return ckList;
    }

}
