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
package org.onap.aai.cacher.service.rest.util;

import com.google.gson.JsonObject;
import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.service.helper.CacheHelperService;
import org.onap.aai.cacher.util.AAIConstants;

import java.util.ArrayList;
import java.util.List;

public class CacheKeyRequestValidation {
    private CacheKeyRequestValidationType type;

    public CacheKeyRequestValidation(CacheKeyRequestValidationType type) {
        this.type = type;
    }
    
    public List<String> checkMissingRequiredFields(JsonObject input, CacheHelperService chs) {
        ArrayList<String> results = new ArrayList<>();
        if (input == null) {
            return results;
        }
        CacheKey cacheKey = CacheKey.fromJson(input);
        String baseUrl = cacheKey.getBaseUrl();
        String uri = cacheKey.getURI();
        if ( CacheKey.DEFAULT_VALUE.equals(baseUrl)) {
            results.add("baseUrl");
        }
        if ( CacheKey.DEFAULT_VALUE.equals(uri)) {
            results.add("URI");
        }
        return results;
    }

    public List<String> validateCacheKeyRequest(JsonObject input, CacheHelperService chs) {
        ArrayList<String> results = new ArrayList<>();
        if (input == null) {
            results.add("Unsupported CacheKey request format, empty payload.");
            return results;
        }
        CacheKey cacheKey = CacheKey.fromJson(input);
        if ("-1".equals(cacheKey.getCacheKey())) {
            results.add("Unsupported CacheKey request format, unspecified cacheKey.");
            return results;
        }
        if (type.equals(CacheKeyRequestValidationType.DELETE)) {
            return results;
        }

        boolean keyExists = chs.isKeyPresent(cacheKey, AAIConstants.COLLECTION_CACHEKEY);
        if (type.equals(CacheKeyRequestValidationType.ADD)) {
            if (keyExists) {
                results.add("Invalid request to add cacheKey " + cacheKey.getCacheKey() + ", cacheKey exists.");
            }
        } else if (type.equals(CacheKeyRequestValidationType.UPDATE) && !keyExists) {
            results.add(
                    "Invalid request to update cacheKey " + cacheKey.getCacheKey() + ", cacheKey does not exist.");
        }
        return results;
    }

}