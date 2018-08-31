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
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.cacher.injestion.parser.AAIResourcesUriTemplates;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.cacher.model.DBAction;
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

/**
 * AAI resource get all parser strategy
 */
@Component(value = "aai-resource-dmaap")
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AAIResourceDmaapParserStrategy implements PayloadParserStrategy {

    protected AAIResourcesUriTemplates aaiResourcesUriTemplates;

    private PayloadParserService payloadParserService;

    private DmaapAction actionType;

    @Autowired
    public AAIResourceDmaapParserStrategy(AAIResourcesUriTemplates aaiResourcesUriTemplates,
            PayloadParserService payloadParserService) {
        this.aaiResourcesUriTemplates = aaiResourcesUriTemplates;
        this.payloadParserService = payloadParserService;
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
        final List<CacheEntry> cacheEntries = new ArrayList<>();

        JsonObject header = jsonObject.getAsJsonObject("event-header");
        JsonObject entity = jsonObject.getAsJsonObject("entity");
        String topEntity = header.get("top-entity-type").getAsString();

        actionType = DmaapAction.valueOf(header.get("action").getAsString());
        boolean isTopLevel = topEntity.equals(header.get("entity-type").getAsString());
        String fullUri = getFullUri(header);

        CacheEntry cacheEntry = generateCacheEntry(entity, actionType, isTopLevel, fullUri);

        cacheEntries.add(cacheEntry);

        // determine relationships on the other end that need to be modified.
        MultiValueMap<String, AAIResourceDmaapParserStrategy.AAIRelatedToDetails> relationships;
        if (isTopLevel) {
            relationships = getFromRelationshipFullUriToRelationshipObj(entity, fullUri);
        } else {
            relationships = getFromRelationshipFullUriToRelationshipObj(entity, getBaseUri(fullUri));
        }
        if (jsonObject.has("existing-obj")) {
            adjustRelationshipsBasedOnExisting(jsonObject, fullUri, relationships);
        }

        JsonObject relatedToObj;
        for (Map.Entry<String, List<AAIResourceDmaapParserStrategy.AAIRelatedToDetails>> relationship : relationships
                .entrySet()) {
            for (AAIResourceDmaapParserStrategy.AAIRelatedToDetails aaiRelatedToDetails : relationship.getValue()) {
                relatedToObj = fullUriToRelationshipObj(relationship.getKey(), aaiRelatedToDetails.getLabel());
                cacheEntries.add(generateCacheEntry(relatedToObj, aaiRelatedToDetails.getActionType(), false,
                        aaiRelatedToDetails.getFullUri() + "/relationship-list/relationship/"
                                + aaiResourcesUriTemplates.encodeProp(relationship.getKey())));
            }
        }

        return cacheEntries;
    }

    private String getBaseUri(String fullUri) {
        String uri = getUri(fullUri);
        List<AAIUriSegment> uriSegmentList = getAaiUriSegments(uri);
        return uriSegmentList.get(0).getSegment();
    }

    protected String getFullUriPrefix(String fullUri) {
        return StringUtils.substring(fullUri, 0, StringUtils.ordinalIndexOf(fullUri, "/", 3));
    }

    protected CacheEntry generateCacheEntry(JsonObject entity, DmaapAction actionType, boolean isTopLevel,
            String fullUri) {
        String uri = getUri(fullUri);
        List<AAIUriSegment> uriSegmentList = getAaiUriSegments(uri);
        String id = uriSegmentList.get(0).getSegment();
        String collection = uriSegmentList.get(0).getSegmentSingular();
        JsonObject entityBody = getEntityBody(entity, uriSegmentList);
        JsonObject findQuery = getFindQuery(uriSegmentList);
        JsonObject nestedFindQuery = getNestedFindQuery(uriSegmentList);
        String nestedField = getNestedField(uriSegmentList);
        JsonObject nestedIdentifier = getNestedIdentifier(uriSegmentList);
        DBAction dbAction = getDBAction(actionType);

        return CacheEntry.CacheEntryBuilder.createCacheEntry().inCollection(collection).withDbAction(dbAction)
                .withId(id).isNested(!isTopLevel).withPayload(entityBody).withFindQuery(findQuery)
                .withNestedFind(nestedFindQuery).withNestedField(nestedField)
                .withNestedFieldIdentifierObj(nestedIdentifier).build();
    }

    protected DBAction getDBAction(DmaapAction actionType) {
        DBAction dbAction = DBAction.INSERT_REPLACE;
        switch (actionType) {
        case CREATE:
            dbAction = DBAction.INSERT_REPLACE;
            break;
        case DELETE:
            dbAction = DBAction.DELETE;
            break;
        case UPDATE:
            dbAction = DBAction.UPDATE;
            break;
        }
        return dbAction;
    }

    protected JsonObject getNestedIdentifier(List<AAIUriSegment> uriSegmentList) {
        final JsonObject nestedIdentifier = new JsonObject();
        if (uriSegmentList.size() > 1) {
            AAIUriSegment lastSegment = uriSegmentList.get(uriSegmentList.size() - 1);
            lastSegment.getSegmentKeyValues().forEach(nestedIdentifier::addProperty);
        }
        return nestedIdentifier;
    }

    protected String getNestedField(List<AAIUriSegment> uriSegmentList) {
        StringBuilder nestedField = new StringBuilder();

        if (uriSegmentList.size() > 1) {
            if (uriSegmentList.get(1).getSegmentPlural().isPresent()) {
                nestedField.append(uriSegmentList.get(1).getSegmentPlural().get()).append(".")
                        .append(uriSegmentList.get(1).getSegmentSingular());
            } else {
                nestedField.append(uriSegmentList.get(1).getSegmentSingular());
            }

            for (int i = 2; i < uriSegmentList.size(); i++) {
                if (uriSegmentList.get(i).getSegmentPlural().isPresent()) {
                    nestedField.append(".$.").append(uriSegmentList.get(i).getSegmentPlural().get()).append(".")
                            .append(uriSegmentList.get(i).getSegmentSingular());
                } else {
                    nestedField.append(".$.").append(uriSegmentList.get(i).getSegmentSingular());
                }
            }
        }
        return nestedField.toString();
    }

    protected JsonObject getNestedFindQuery(List<AAIUriSegment> uriSegmentList) {
        return getFindQuery(uriSegmentList, true);
    }

    protected JsonObject getFindQuery(List<AAIUriSegment> uriSegmentList) {
        return getFindQuery(uriSegmentList, false);
    }

    protected JsonObject getFindQuery(List<AAIUriSegment> uriSegmentList, boolean isNested) {
        final JsonObject findQuery = new JsonObject();
        if (uriSegmentList.isEmpty()) {
            return findQuery;
        }

        AAIUriSegment aaiUriSegment = uriSegmentList.get(0);
        findQuery.addProperty("_id", aaiUriSegment.getSegment());
        aaiUriSegment.getSegmentKeyValues().forEach(findQuery::addProperty);

        StringBuilder nestedField = new StringBuilder();
        int segmentToProcess = uriSegmentList.size();
        if (!isNested) {
            segmentToProcess--;
        }
        for (int i = 1; i < segmentToProcess; i++) {
            aaiUriSegment = uriSegmentList.get(i);
            if (nestedField.length() != 0) {
                nestedField.append(".");
            }
            if (aaiUriSegment.getSegmentPlural().isPresent()) {
                nestedField.append(aaiUriSegment.getSegmentPlural().get()).append(".");
            }
            nestedField.append(aaiUriSegment.getSegmentSingular());
            aaiUriSegment.getSegmentKeyValues()
                    .forEach((k, v) -> findQuery.addProperty(nestedField.toString() + "." + k, v));
        }
        return findQuery;
    }

    /**
     * strips away the parent wrapping from the dmaap events entity payload
     * 
     * @param entity
     * @param uriSegmentList
     * @return
     */
    protected JsonObject getEntityBody(JsonObject entity, List<AAIUriSegment> uriSegmentList) {

        if (uriSegmentList.size() == 1) {
            return entity;
        }

        JsonObject entityBody = entity.getAsJsonObject();

        // if processing relationship no need to look for nested obj, entity is the obj
        if (!"relationship".equals(uriSegmentList.get(uriSegmentList.size() - 1).getSegmentSingular())) {
            for (int i = 1; i < uriSegmentList.size(); i++) {
                if (uriSegmentList.get(i).getSegmentPlural().isPresent()) {
                    entityBody = entityBody.getAsJsonObject(uriSegmentList.get(i).getSegmentPlural().get())
                            .getAsJsonArray(uriSegmentList.get(i).getSegmentSingular()).get(0).getAsJsonObject();
                } else {
                    entityBody = entityBody.getAsJsonArray(uriSegmentList.get(i).getSegmentSingular()).get(0)
                            .getAsJsonObject();
                }

            }
        }

        return entityBody;

    }

    protected List<AAIUriSegment> getAaiUriSegments(String uri) {
        List<String> uriSegmentTemplates = aaiResourcesUriTemplates.uriToTemplates(uri);
        List<String> uriSegments = aaiResourcesUriTemplates.uriToSegments(uri);

        List<AAIUriSegment> uriSegmentList = new ArrayList<>(uriSegments.size());

        AAIUriSegment aus;
        for (int i = 0; i < uriSegments.size(); i++) {
            aus = new AAIUriSegment(uriSegments.get(i), uriSegmentTemplates.get(i));
            aus.setSegmentKeyValues(
                    aaiResourcesUriTemplates.getUriTemplateMappings(aus.getSegment(), aus.getSegmentTemplate()));
            uriSegmentList.add(aus);
        }
        return uriSegmentList;
    }

    /**
     * For update events with an existing obj available adjust the cache actions to
     * be taken on relationship objects.
     * 
     * @param jsonObject
     * @param fullUri
     * @param newObjectRelationships
     */
    private void adjustRelationshipsBasedOnExisting(JsonObject jsonObject, String fullUri,
            MultiValueMap<String, AAIResourceDmaapParserStrategy.AAIRelatedToDetails> newObjectRelationships) {
        JsonObject existingObj = jsonObject.getAsJsonObject("existing-obj");
        MultiValueMap<String, AAIResourceDmaapParserStrategy.AAIRelatedToDetails> oldRelationships = getFromRelationshipFullUriToRelationshipObj(
                existingObj, fullUri);
        oldRelationships.forEach((k, v) -> {
            if (newObjectRelationships.containsKey(k)) {
                v.forEach(oldA -> {
                    int found = -1;
                    for (int i = 0; i < newObjectRelationships.get(k).size(); i++) {
                        if (newObjectRelationships.get(k).get(i).getFullUri().equals(oldA.getFullUri())) {
                            found = i;
                            break;
                        }
                    }
                    if (found != -1) {
                        newObjectRelationships.get(k).remove(newObjectRelationships.get(k).get(found));
                    } else {
                        oldA.setActionType(DmaapAction.DELETE);
                        newObjectRelationships.get(k).add(oldA);
                    }
                });
            } else {
                v.forEach(aaiRelatedToDetails -> {
                    aaiRelatedToDetails.setActionType(DmaapAction.DELETE);
                    newObjectRelationships.add(k, aaiRelatedToDetails);
                });
            }
        });
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
        String uri = getUri(fullUri);
        List<AAIUriSegment> uriSegmentList = getAaiUriSegments(uri);

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

    /**
     *
     * @param entity
     * @param fullUri
     * @return
     */
    protected MultiValueMap<String, AAIResourceDmaapParserStrategy.AAIRelatedToDetails> getFromRelationshipFullUriToRelationshipObj(
            JsonObject entity, String fullUri) {
        final MultiValueMap<String, AAIResourceDmaapParserStrategy.AAIRelatedToDetails> relationshipMapping = new LinkedMultiValueMap<>();
        for (Map.Entry<String, JsonElement> e : entity.entrySet()) {
            if (e.getKey().equals("relationship-list") && e.getValue().isJsonObject()) {
                JsonArray relationships = e.getValue().getAsJsonObject().getAsJsonArray("relationship");
                for (JsonElement relationship : relationships) {
                    relationshipMapping.add(fullUri, new AAIResourceDmaapParserStrategy.AAIRelatedToDetails(
                            relationship.getAsJsonObject().get("related-link").getAsString(),
                            relationship.getAsJsonObject().get("relationship-label").getAsString(), actionType));
                }
            } else if (e.getValue().isJsonObject() && e.getValue().getAsJsonObject().entrySet().size() == 1) {
                Map.Entry<String, JsonElement> entry = e.getValue().getAsJsonObject().entrySet().iterator().next();
                if (entry.getValue().isJsonArray()) {
                    String type = entry.getKey();
                    JsonArray children = entry.getValue().getAsJsonArray();
                    for (JsonElement child : children) {
                        relationshipMapping.putAll(getFromRelationshipFullUriToRelationshipObj(child.getAsJsonObject(),
                                fullUri + aaiResourcesUriTemplates.getUri(type, child.getAsJsonObject())));
                    }
                }
            }
        }
        return relationshipMapping;
    }

    protected String getUri(String fullUri) {
        return fullUri.replaceAll("/aai/v\\d+", "");
    }

    protected String getFullUri(JsonObject header) {
        return header.get("entity-link").getAsString();
    }

    protected enum DmaapAction {
        DELETE, UPDATE, CREATE
    }

    class AAIUriSegment {

        private String segment;
        private String segmentTemplate;
        private Optional<String> segmentPlural = Optional.empty();
        private String segmentSingular;
        private Map<String, String> segmentKeyValues;

        AAIUriSegment(String segment, String template) {
            this.segment = segment;
            this.segmentTemplate = template;
            String[] segmentSplit = segment.split("/");
            String[] templateSplit = template.split("/");
            for (int i = 0; i < templateSplit.length; i++) {
                if (templateSplit[i].contains("{")) {
                    segmentSingular = segmentSplit[i - 1];
                    if (!"".equals(segmentSplit[i - 2])) {
                        segmentPlural = Optional.of(segmentSplit[i - 2]);
                    }
                    break;
                }
            }
        }

        String getSegment() {
            return segment;
        }

        String getSegmentTemplate() {
            return segmentTemplate;
        }

        Map<String, String> getSegmentKeyValues() {
            return segmentKeyValues;
        }

        void setSegmentKeyValues(Map<String, String> segmentKeyValues) {
            this.segmentKeyValues = segmentKeyValues;
        }

        Optional<String> getSegmentPlural() {
            return segmentPlural;
        }

        String getSegmentSingular() {
            return segmentSingular;
        }
    }

    class AAIRelatedToDetails {
        private String fullUri;
        private String label;
        private DmaapAction actionType;

        public AAIRelatedToDetails(String fullUri, String label, DmaapAction actionType) {
            this.fullUri = fullUri;
            this.label = label;
            this.actionType = actionType;
        }

        public String getFullUri() {
            return fullUri;
        }

        public String getLabel() {
            return label;
        }

        public DmaapAction getActionType() {
            return actionType;
        }

        public void setActionType(DmaapAction actionType) {
            this.actionType = actionType;
        }

        @Override
        public String toString() {
            return fullUri + " : " + label;
        }
    }
}
