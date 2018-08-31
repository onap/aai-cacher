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
package org.onap.aai.cacher.injestion.parser.strategy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.onap.aai.cacher.injestion.parser.AAIResourcesUriTemplates;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.cacher.model.DBAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AAI resource get all parser strategy
 */
@Component(value = "aai-resource-get-all")
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AAIResourceGetAllPayloadParserStrategy implements PayloadParserStrategy {

    AAIResourcesUriTemplates aaiResourcesUriTemplates;

    @Autowired
    public AAIResourceGetAllPayloadParserStrategy(AAIResourcesUriTemplates aaiResourcesUriTemplates) {
        this.aaiResourcesUriTemplates = aaiResourcesUriTemplates;
    }

    /**
     * Parses aai resources specific payloads generating the details for .
     * 
     * @param originalKey
     * @param jsonObject
     * @return
     */
    @Override
    public List<CacheEntry> process(String originalKey, JsonObject jsonObject) {
        final List<CacheEntry> cacheEntries = new ArrayList<>();

        String type = jsonObject.entrySet().iterator().next().getKey();

        JsonArray ja = jsonObject.getAsJsonArray(type);
        CacheEntry cacheEntry;
        String uri;
        JsonObject jo;
        for (JsonElement jsonElement : ja) {
            jo = jsonElement.getAsJsonObject();
            uri = aaiResourcesUriTemplates.getUri(type, jo);
            jsonObject.addProperty("_id", uri);
            cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry().withId(uri).inCollection(originalKey)
                    .withFindQuery(getFindQuery(uri)).withPayload(jo).withDbAction(DBAction.INSERT_REPLACE).build();
            cacheEntries.add(cacheEntry);
        }

        return cacheEntries;
    }

    protected JsonObject getFindQuery(String uri) {
        JsonObject jo = new JsonObject();
        jo.addProperty("_id", uri);
        return jo;
    }

}
