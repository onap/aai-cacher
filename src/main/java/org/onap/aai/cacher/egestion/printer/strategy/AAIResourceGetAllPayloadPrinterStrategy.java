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
import org.onap.aai.cacher.injestion.parser.strategy.aai.AAIResourcesUriTemplates;
import org.onap.aai.cacher.injestion.parser.strategy.aai.AAIUriSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This strategy re constructs the aai objects as the parser for this strategy
 * would have taken all nested objects and stored them separately
 */
@Component(value = "aai-resource-get-all-printer")
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AAIResourceGetAllPayloadPrinterStrategy implements PayloadPrinterStrategy {

    private static final String RELATIONSHIP = "relationship";

    private AAIResourcesUriTemplates aaiResourcesUriTemplates;

    @Autowired
    public AAIResourceGetAllPayloadPrinterStrategy(AAIResourcesUriTemplates aaiResourcesUriTemplates) {
        this.aaiResourcesUriTemplates = aaiResourcesUriTemplates;
    }


    /**
     * Create a jsonObject from the jsonArray for a specific collection
     *
     * @param collectionName name of the collection the json array is from.
     * @param jsonArray json objects from the collection to be aggregated
     * @return the cache payload reconstructed
     */
    @Override
    public JsonObject createJson(String collectionName, JsonArray jsonArray) {

        List<JsonObject> topLevels = new ArrayList<>();
        Map<String, JsonObject> nested = new HashMap<>();

        if (jsonArray == null || jsonArray.size() == 0) {
            return null;
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            final JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            String id = jsonObject.get("_id").getAsString();
            List<AAIUriSegment> segments = aaiResourcesUriTemplates.getAaiUriSegments(id);
            if (segments.size() == 1) {
                topLevels.add(jsonObject);
            } else {
                nested.put(id, jsonObject);
            }
        }

        jsonArray = new JsonArray();
        topLevels.forEach(top -> populateNested(top, nested));
        topLevels.stream().filter(jsonObject -> jsonObject.size() > 0).forEach(jsonArray::add);
        JsonObject jsonObj = new JsonObject();
        jsonObj.add(collectionName, jsonArray);
        return jsonObj;

    }

    /**
     * For the json object collect and populate all of the nested objects
     * @param jo JsonObject to be populated
     */
    private void populateNested(JsonObject jo, Map<String, JsonObject> nested) {
        List<String> uris = new ArrayList<>();
        MultiValueMap<Integer, String> lvlToUri = new LinkedMultiValueMap<>();
        Map<String, JsonObject> uriToJson = new HashMap<>();
        int lvl = 0;
        String uri = jo.getAsJsonPrimitive("_id").getAsString();
        lvlToUri.add(lvl, uri);
        uriToJson.put(uri, jo);
        uris.add(uri);

        boolean foundNested = true;
        int start = 0;
        while (foundNested) {
            List<JsonObject> nestedObjs = new ArrayList<>();
            for (int i = start; i < uris.size(); i++) {
                Set<String> nestedProps = nestedPropsToBePopulated(uriToJson.get(uris.get(i)));
                nestedProps = cleanEmptyProps(uriToJson.get(uris.get(i)), nestedProps, nested);
                nestedObjs.addAll(getNestedObjs(uriToJson.get(uris.get(i)), nestedProps, nested));
            }
            if (nestedObjs.isEmpty()) {
                foundNested = false;
            } else {
                lvl++;
                start = uris.size();
                for (JsonObject nestedObj : nestedObjs) {
                    String u = nestedObj.get("_id").getAsString();
                    uris.add(u);
                    uriToJson.put(u, nestedObj);
                    lvlToUri.add(lvl, u);
                }
            }
        }

        uriToJson.values().forEach(jsonObject -> jsonObject.remove("_id"));

        // skips the last lvl as it will not have any nested
        for (int i = lvl-1; i >= 0; i--) {
           lvlToUri.get(i).forEach(u -> {
               Set<String> nestedProps = nestedPropsToBePopulated(uriToJson.get(u));
               nestedProps.forEach(prop -> {
                   if (uriToJson.get(u).get(prop).isJsonArray()) {
                       JsonArray objs = new JsonArray();
                       uriToJson.get(u).get(prop).getAsJsonArray().forEach(jsonElement -> {
                           if (jsonElement != null
                                   && jsonElement.isJsonPrimitive()
                                   && jsonElement.getAsJsonPrimitive().isString()
                                   && uriToJson.containsKey(jsonElement.getAsString())) {
                               objs.add(uriToJson.get(jsonElement.getAsString()));
                               uriToJson.remove(jsonElement.getAsString());
                           }
                       });
                       uriToJson.get(u).add(prop, objs);
                   } else if (uriToJson.get(u).get(prop).isJsonObject()) {
                       JsonArray objs = new JsonArray();
                       JsonObject jsonObject = uriToJson.get(u).get(prop).getAsJsonObject();
                       String key = jsonObject.entrySet().iterator().next().getKey();
                       jsonObject.get(key).getAsJsonArray().forEach(jsonElement -> {
                           if (jsonElement != null
                                   && jsonElement.isJsonPrimitive()
                                   && jsonElement.getAsJsonPrimitive().isString()
                                   && uriToJson.containsKey(jsonElement.getAsString())) {
                               objs.add(uriToJson.get(jsonElement.getAsString()));
                               uriToJson.remove(jsonElement.getAsString());
                           }
                       });
                       uriToJson.get(u).get(prop).getAsJsonObject().add(key, objs);
                   }
               });
           });
        }
        //return uriToJson.get(uri);
    }

    private Set<String> cleanEmptyProps(JsonObject jsonObject, Set<String> nestedProps, Map<String, JsonObject> nested) {
        Set<String> updatedNested = new HashSet<>(nestedProps.size());
        for (String nestedProp : nestedProps) {
            if (jsonObject.get(nestedProp).isJsonObject()) {
                String key = jsonObject.get(nestedProp).getAsJsonObject().entrySet().iterator().next().getKey();
                if (jsonObject.get(nestedProp).getAsJsonObject().get(key).getAsJsonArray().size() == 0) {
                    jsonObject.remove(nestedProp);
                } else {
                    updatedNested.add(nestedProp);
                }
            } else if (jsonObject.get(nestedProp).isJsonArray()) {
                if (jsonObject.get(nestedProp).getAsJsonArray().size() == 0) {
                    jsonObject.remove(nestedProp);
                } else {
                    updatedNested.add(nestedProp);
                }
            }
        }

        return updatedNested;
    }

    /**
     * Collects the nested json objects from storage
     * @param jsonObject the object with nested to be collected
     * @param nestedProps the properties with nested objs to be collected
     * @return Collected json object
     */
    private List<JsonObject> getNestedObjs(JsonObject jsonObject, Set<String> nestedProps, Map<String, JsonObject> nested) {
        final List<JsonObject> objs = new ArrayList<>();

        for (String nestedProp : nestedProps) {
            List<String> uris = new ArrayList<>();
            if (jsonObject.get(nestedProp).isJsonObject()) {
                String key = jsonObject.get(nestedProp).getAsJsonObject().entrySet().iterator().next().getKey();
                jsonObject.get(nestedProp).getAsJsonObject().get(key).getAsJsonArray()
                        .iterator().forEachRemaining(jsonElement -> uris.add(jsonElement.getAsString()));
            } else if (jsonObject.get(nestedProp).isJsonArray()) {
                jsonObject.get(nestedProp).getAsJsonArray()
                        .iterator().forEachRemaining(jsonElement -> uris.add(jsonElement.getAsString()));
            }

            uris.stream().filter(nested::containsKey).map(nested::get).forEach(objs::add);
        }
        return objs;
    }

    /**
     * For the given JsonObject determine which properties have nested to be populated
     * @param jsonObject to be scanned
     * @return Set of property keys that have nested to be populated
     */
    private Set<String> nestedPropsToBePopulated(JsonObject jsonObject) {
        Set<String> props = jsonObject.entrySet()
                .stream()
                .filter(nestedEntry -> nestedEntry.getValue().isJsonObject() &&
                        aaiResourcesUriTemplates.hasType(nestedEntry.getValue().getAsJsonObject().entrySet().iterator().next().getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        props.addAll(jsonObject.entrySet()
                .stream()
                .filter(nestedEntry -> nestedEntry.getValue().isJsonArray() &&
                        aaiResourcesUriTemplates.hasType(nestedEntry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet()));
        props.remove("relationship-list");
        return props;
    }
}
