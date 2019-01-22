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
package org.onap.aai.cacher.injestion.parser.strategy.aai.dmaap;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.onap.aai.cacher.injestion.parser.strategy.AAIResourceGetAllPayloadParserStrategy;
import org.onap.aai.cacher.injestion.parser.strategy.aai.AAIResourcesUriTemplates;
import org.onap.aai.cacher.injestion.parser.strategy.aai.AAIUriSegment;
import org.onap.aai.cacher.model.CacheEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AAI resource get all parser strategy
 */
@Component(value = "aai-resource-dmaap")
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AAIResourceDmaapParserStrategy extends AAIResourceGetAllPayloadParserStrategy {
    
    private final static EELFLogger LOGGER = EELFManager.getInstance().getLogger(AAIResourceDmaapParserStrategy.class);

    @Autowired
    public AAIResourceDmaapParserStrategy(AAIResourcesUriTemplates aaiResourcesUriTemplates) {
        super(aaiResourcesUriTemplates);
    }

    /**
     * Parses aai resources specific payloads generating the details for caching.
     *
     * @param originalKey
     * @param jsonObject
     * @return
     */
    @Override
    public List<CacheEntry> process(String originalKey, JsonObject jsonObject) {
        final List<CacheEntry> cacheEntries;

        JsonObject header = jsonObject.getAsJsonObject("event-header");
        String topEntityType = header.get("top-entity-type").getAsString();
        String fullUri = header.get("entity-link").getAsString();
        if ( fullUri.endsWith("/")) {
            fullUri = fullUri.substring(0, fullUri.length() - 1);
        }
        String entityUri = aaiResourcesUriTemplates.getAAIUriFromEntityUri(fullUri);
        String fullUriPrefix = aaiResourcesUriTemplates.getAAIUriFromEntityUriPrefix(fullUri);

        DmaapAction actionType = DmaapAction.valueOf(header.get("action").getAsString());

        List<AAIUriSegment> uriSegments = aaiResourcesUriTemplates.getAaiUriSegments(entityUri);

        // get base uri so if is top lvl use "" else go 1 back from current node type. to reuse exist functions.
        String baseUri = getBaseUri(uriSegments);

        // get wrapped wrap obj it looks like a get-all result with 1
        // will wrap address lists even though they do not normally have a wrapper
        JsonObject wrappedEntityObject = getWrappedEntityObject(jsonObject.getAsJsonObject("entity"), uriSegments);

        // get cache entries
        cacheEntries = internalProcess(topEntityType, wrappedEntityObject, baseUri);

        // modify action to map to dmaap event type
        cacheEntries.forEach(cacheEntry -> cacheEntry.setDbAction(actionType.getDbAction()));

        // cache entries for relationships
        MultiValueMap<String, AAIRelatedToDetails> cacheEntriesRelationships =
                getFromRelationshipFullUriToRelationshipObj(cacheEntries, fullUriPrefix, actionType);

        if (jsonObject.has("existing-obj")) {
            MultiValueMap<String, AAIRelatedToDetails> existingCacheEntriesRelationships =
                    getFromRelationshipFullUriToRelationshipObj(
                            internalProcess(topEntityType, jsonObject.getAsJsonObject("existing-obj"), baseUri),
                            fullUriPrefix,
                            actionType
                    );
            adjustRelationshipsBasedOnExisting(existingCacheEntriesRelationships, cacheEntriesRelationships);

        }

        cacheEntries.addAll(getRelationshipCacheEntries(cacheEntriesRelationships));

        getParentUpdateCacheEntryIfNeeded(topEntityType, entityUri, actionType, uriSegments).ifPresent(cacheEntries::add);

        return cacheEntries;
    }

    private Optional<CacheEntry> getParentUpdateCacheEntryIfNeeded(String topEntityType, String entityUri, DmaapAction actionType, List<AAIUriSegment> uriSegments) {

        if (uriSegments.size() <= 1) {
            return Optional.empty();
        }

        switch (actionType) {
            case DELETE:
                return Optional.of(getParentUpdateCacheEntry(topEntityType, entityUri, actionType, uriSegments));
            case CREATE:
                return Optional.of(getParentUpdateCacheEntry(topEntityType, entityUri, DmaapAction.UPDATE, uriSegments));
            default:
                return Optional.empty();
        }
    }

    private CacheEntry getParentUpdateCacheEntry(String topEntityType, String entityUri, DmaapAction actionType, List<AAIUriSegment> uriSegments) {
        String parentUri = String.join(
                "",
                uriSegments.stream().limit(uriSegments.size()-1).map(AAIUriSegment::getSegment).collect(Collectors.toList()));
        JsonObject findQuery = new JsonObject();
        findQuery.addProperty("_id", parentUri);
        JsonObject nestedFindQuery = new JsonObject();
        nestedFindQuery.addProperty("_id", parentUri);
        StringBuilder nestedField = new StringBuilder();
        uriSegments.get(uriSegments.size()-1).getSegmentPlural().ifPresent(plural -> nestedField.append(plural).append("."));
        nestedField.append(uriSegments.get(uriSegments.size()-1).getSegmentSingular());
        JsonObject nestedIdentifier = new JsonObject();
        JsonArray ja = new JsonArray();
        ja.add(entityUri);
        nestedIdentifier.add("$in", ja);
        return CacheEntry.CacheEntryBuilder.createCacheEntry()
                .inCollection(topEntityType)
                .withDbAction(actionType.getDbAction())
                .withId(parentUri)
                .isNested(true)
                .isNestedPayloadString(true)
                .withNestedString(entityUri)
                .withFindQuery(findQuery)
                .withNestedFind(nestedFindQuery)
                .withNestedField(nestedField.toString())
                .withNestedFieldIdentifierObj(nestedIdentifier)
                .build();
    }

    private List<CacheEntry> getRelationshipCacheEntries(MultiValueMap<String, AAIRelatedToDetails> cacheEntriesRelationships) {
        final List<CacheEntry> cacheEntries = new ArrayList<>();
        JsonObject relatedToObj;
        for (Map.Entry<String, List<AAIRelatedToDetails>> relationship : cacheEntriesRelationships.entrySet()) {
            for (AAIRelatedToDetails aaiRelatedToDetails : relationship.getValue()) {
                relatedToObj = fullUriToRelationshipObj(relationship.getKey(), aaiRelatedToDetails.getLabel());
                cacheEntries.add(generateRelationshipCacheEntry(relatedToObj, aaiRelatedToDetails.getActionType(),
                        aaiRelatedToDetails.getFullUri()));
            }
        }
        return cacheEntries;
    }

    private CacheEntry generateRelationshipCacheEntry(JsonObject entity, DmaapAction actionType, String fullUri) {

        String uri = aaiResourcesUriTemplates.getAAIUriFromEntityUri(fullUri);
        List<AAIUriSegment> uriSegmentList = aaiResourcesUriTemplates.getAaiUriSegments(uri);
        String collection = uriSegmentList.get(0).getSegmentSingular();
        JsonObject findQuery = new JsonObject();
        findQuery.addProperty("_id", uri);
        JsonObject nestedFindQuery = new JsonObject();
        nestedFindQuery.addProperty("_id", uri);
        nestedFindQuery.addProperty("relationship-list.relationship.related-link", entity.get("related-link").getAsString());
        String nestedField = "relationship-list.relationship";
        JsonObject nestedIdentifier = new JsonObject();
        nestedIdentifier.addProperty("related-link", entity.get("related-link").getAsString());

        return CacheEntry.CacheEntryBuilder.createCacheEntry().inCollection(collection).withDbAction(actionType.getDbAction())
                .withId(uri).isNested(true).withPayload(entity).withFindQuery(findQuery)
                .withNestedFind(nestedFindQuery).withNestedField(nestedField)
                .withNestedFieldIdentifierObj(nestedIdentifier).build();
    }

    private void adjustRelationshipsBasedOnExisting(MultiValueMap<String, AAIRelatedToDetails> existingCacheEntriesRelationships,
                                                    MultiValueMap<String, AAIRelatedToDetails> cacheEntriesRelationships) {
        existingCacheEntriesRelationships.forEach((k, v) -> {
            if (cacheEntriesRelationships.containsKey(k)) {
                v.forEach(oldA -> {
                    int found = -1;
                    for (int i = 0; i < cacheEntriesRelationships.get(k).size(); i++) {
                        if (cacheEntriesRelationships.get(k).get(i).getFullUri().equals(oldA.getFullUri())) {
                            found = i;
                            break;
                        }
                    }
                    if (found != -1) {
                        cacheEntriesRelationships.get(k).remove(cacheEntriesRelationships.get(k).get(found));
                    } else {
                        oldA.setActionType(DmaapAction.DELETE);
                        cacheEntriesRelationships.get(k).add(oldA);
                    }
                });
            } else {
                v.forEach(aaiRelatedToDetails -> {
                    aaiRelatedToDetails.setActionType(DmaapAction.DELETE);
                    cacheEntriesRelationships.add(k, aaiRelatedToDetails);
                });
            }
        });
    }

    private MultiValueMap<String, AAIRelatedToDetails> getFromRelationshipFullUriToRelationshipObj(
            List<CacheEntry> cacheEntries,
            String fullUriPrefix,
            DmaapAction actionType) {
        final MultiValueMap<String, AAIRelatedToDetails> relationshipMapping = new LinkedMultiValueMap<>();
        for (CacheEntry cacheEntry : cacheEntries) {
            for (Map.Entry<String, JsonElement> e : cacheEntry.getPayload().entrySet()) {
                if (e.getKey().equals("relationship-list") && e.getValue().isJsonObject()) {
                    JsonArray relationships = e.getValue().getAsJsonObject().getAsJsonArray("relationship");
                    for (JsonElement relationship : relationships) {
                        relationshipMapping.add(fullUriPrefix + cacheEntry.getId(), new AAIRelatedToDetails(
                                relationship.getAsJsonObject().get("related-link").getAsString(),
                                relationship.getAsJsonObject().get("relationship-label").getAsString(), actionType));
                    }
                }
            }
        }
        return relationshipMapping;
    }

    /**
     * Given fullUri uri generate an aai relationship obj
     *
     * @param fullUri
     * @return
     */
    protected JsonObject fullUriToRelationshipObj(String fullUri, String label) {
        final JsonObject relObj = new JsonObject();
        final JsonArray relData = new JsonArray();
        String uri = aaiResourcesUriTemplates.getAAIUriFromEntityUri(fullUri);
        List<AAIUriSegment> uriSegmentList = aaiResourcesUriTemplates.getAaiUriSegments(uri);

        relObj.addProperty("related-to", uriSegmentList.get(uriSegmentList.size() - 1).getSegmentSingular());
        if (label != null) {
            relObj.addProperty("relationship-label", label);
        }
        relObj.addProperty("related-link", fullUri);

        for (AAIUriSegment aaiUriSegment : uriSegmentList) {
            aaiUriSegment.getSegmentKeyValues().forEach((k, v) -> {
                JsonObject relDataEntry;
                relDataEntry = new JsonObject();
                relDataEntry.addProperty("relationship-key", aaiUriSegment.getSegmentSingular() + "." + k);
                relDataEntry.addProperty("relationship-value", v);
                relData.add(relDataEntry);
            });
        }
        relObj.add("relationship-data", relData);

        return relObj;
    }

    private JsonObject getWrappedEntityObject(JsonObject dmaapEntity, List<AAIUriSegment> uriSegments) {
        JsonObject objectWrapper = new JsonObject();
        JsonArray arrayWrapper = new JsonArray();
        String arrayKey = uriSegments.get(0).getSegmentSingular();
        JsonObject entityBody = dmaapEntity.getAsJsonObject();

        if (uriSegments.size() > 1) {
            Optional<String> segmentPlural;
            String segmentSingular;
            String jsonStr;
            JsonObject jsonObj;
            JsonArray jsonArray;
            JsonElement jsonElement;

            for (int i = 1; i < uriSegments.size(); i++) {

                if (uriSegments.get(i).getSegmentPlural().isPresent()) {
                    segmentPlural = uriSegments.get(i).getSegmentPlural();
                    segmentSingular = uriSegments.get(i).getSegmentSingular();
                    if ( segmentSingular.equals("cvlan-tag")) {
                            // map to what is in the entity
                        segmentSingular = "cvlan-tag-entry";
                    }
                    jsonObj = entityBody.getAsJsonObject(uriSegments.get(i).getSegmentPlural().get());
                    jsonArray = jsonObj.getAsJsonArray(segmentSingular);
                    if ( jsonArray == null ) {
                        LOGGER.error("failed in getWrappedEntityObject " + segmentSingular + " not found in " + jsonObj );
                        // exception expected for missing template
                    }
                    entityBody = jsonArray.get(0).getAsJsonObject();
                    arrayKey = uriSegments.get(i).getSegmentSingular();
                } else {
                    entityBody = entityBody.getAsJsonArray(uriSegments.get(i).getSegmentSingular()).get(0).getAsJsonObject();
                    arrayKey = uriSegments.get(i).getSegmentSingular();
                }

            }
        }
        arrayWrapper.add(entityBody);
        objectWrapper.add(arrayKey, arrayWrapper);
        return objectWrapper;
    }

    private String getBaseUri(List<AAIUriSegment> uriSegments) {
        if (uriSegments.isEmpty() || uriSegments.size() == 1) {
            return "";
        }
        return String.join("", uriSegments.subList(0, uriSegments.size()-1).stream().map(AAIUriSegment::getSegment).collect(Collectors.toList()));
    }



}