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

import com.google.gson.JsonObject;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.cacher.model.DBAction;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Default parser strategy that dont not manipulate the payload.
 */
@Component(value = "none")
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class NonePayloadParserStrategy implements PayloadParserStrategy {

    @Override
    public List<CacheEntry> process(String originalKey, JsonObject jsonObject) {
        JsonObject find = new JsonObject();
        jsonObject.addProperty("_id", originalKey);
        return Collections.singletonList(
                CacheEntry.CacheEntryBuilder.createCacheEntry().withId(originalKey).inCollection(originalKey)
                        .withFindQuery(find).withPayload(jsonObject).withDbAction(DBAction.INSERT_REPLACE).build());
    }

}
