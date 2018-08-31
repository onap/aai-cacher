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
package org.onap.aai.cacher.egestion.printer.strategy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component(value = "aai-resource-get-all-printer")
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AAIResourceGetAllPayloadPrinterStrategy implements PayloadPrinterStrategy {

    /**
     * Create a jsonObject from the jsonArray for a specific collection
     * 
     * @param collectionName
     * @param jsonArray
     * @return
     */
    @Override
    public JsonObject createJson(String collectionName, JsonArray jsonArray) {
        if (jsonArray != null && jsonArray.size() > 0) {
            JsonObject jsonObj = new JsonObject();
            jsonObj.add(collectionName, jsonArray);
            return jsonObj;
        } else {
            return null;
        }
    }
}
