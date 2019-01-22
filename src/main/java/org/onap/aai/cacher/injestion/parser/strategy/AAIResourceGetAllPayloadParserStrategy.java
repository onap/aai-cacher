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
import org.onap.aai.cacher.injestion.parser.strategy.aai.AAIResourcesUriTemplates;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.cacher.model.DBAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This parser strategy breaks down the all nested
 * aai objects to be stored separately
 */
@Component(value="aai-resource-get-all")
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AAIResourceGetAllPayloadParserStrategy implements PayloadParserStrategy {

	private static final String RELATIONSHIP = "relationship";

	protected AAIResourcesUriTemplates aaiResourcesUriTemplates;

	@Autowired
	public AAIResourceGetAllPayloadParserStrategy(AAIResourcesUriTemplates aaiResourcesUriTemplates) {
		this.aaiResourcesUriTemplates = aaiResourcesUriTemplates;
	}


	/**
	 * Parses aai resources specific payloads generating the details for its cache entries.
	 * @param originalKey cache key for this entry
	 * @param jsonObject object to be broken down via this strategy
	 * @return List of cache entries to be stored
	 */
	@Override
	public List<CacheEntry> process(String originalKey, JsonObject jsonObject) {
		return internalProcess(originalKey, jsonObject, "");
	}

	protected List<CacheEntry> internalProcess(String originalKey, JsonObject jsonObject, String baseUri) {
		final List<CacheEntry> cacheEntries = getCacheEntries(originalKey, baseUri, jsonObject);
		List<CacheEntry> nestedCacheEntries;
		Set<String> nestedProperties;

		boolean foundNested = true;
		int start = 0;
		while (foundNested) {
			nestedCacheEntries = new ArrayList<>();
			for (int i = start; i < cacheEntries.size(); i++) {
				nestedProperties = nestedPropsToBePopulated(cacheEntries.get(i).getPayload());
				if (!nestedProperties.isEmpty()) {
					nestedCacheEntries.addAll(processEntriesWithNested(originalKey, cacheEntries.get(i), nestedProperties));
				}
			}
			if (nestedCacheEntries.isEmpty()) {
				foundNested = false;
			} else {
				start = cacheEntries.size();
				cacheEntries.addAll(nestedCacheEntries);
				foundNested = true;
			}
		}

		cacheEntries.forEach(cacheEntry ->
				cacheEntry.getPayload().addProperty("_id", cacheEntry.getId()));
		return cacheEntries;
	}

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


	/**
	 * Replaces the nested object with the id to the object and generates a cache entry per object
	 * @param originalKey top node type
	 * @param cacheEntry with nested aai object
	 * @param nestedProperties the keys that contain nested properties
	 * @return List of CacheEntry for each nested json object
	 */
	private List<CacheEntry> processEntriesWithNested(String originalKey, CacheEntry cacheEntry, Set<String> nestedProperties) {
		final List<CacheEntry> cacheEntries = new ArrayList<>();

		for (String nestedProperty : nestedProperties) {
			List<CacheEntry> nestedCacheEntries = new ArrayList<>();
			if (cacheEntry.getPayload().get(nestedProperty).isJsonObject()) {
				String type = cacheEntry.getPayload().get(nestedProperty).getAsJsonObject().entrySet().iterator().next().getKey();
				nestedCacheEntries =
						getCacheEntries(originalKey, cacheEntry.getId(), cacheEntry.getPayload().get(nestedProperty).getAsJsonObject());
				JsonArray nestedElementsIds = new JsonArray();
				nestedCacheEntries.forEach(nestedCacheEntry -> nestedElementsIds.add(nestedCacheEntry.getId()));
				cacheEntry.getPayload().get(nestedProperty).getAsJsonObject().add(type, nestedElementsIds);
			} else if (cacheEntry.getPayload().get(nestedProperty).isJsonArray()) {
				nestedCacheEntries =
						getCacheEntries(originalKey, cacheEntry.getId(), nestedProperty, cacheEntry.getPayload().get(nestedProperty).getAsJsonArray());
				JsonArray nestedElementsIds = new JsonArray();
				nestedCacheEntries.forEach(nestedCacheEntry -> nestedElementsIds.add(nestedCacheEntry.getId()));
				cacheEntry.getPayload().add(nestedProperty, nestedElementsIds);
			}
			cacheEntries.addAll(nestedCacheEntries);
		}

		return cacheEntries;
	}


	/**
	 * Generates the CacheEntries for all of the nested objects in the JsonObject
	 * @param originalKey the aai-node-type
	 * @param baseUri the base "parent" url used to populate full uri
	 * @param jsonObject the object to be scanned
	 * @return List of CacheEntries for all of the nested objects in the payload
	 */
	protected List<CacheEntry> getCacheEntries(String originalKey, String baseUri, JsonObject jsonObject) {
		String type = jsonObject.entrySet().iterator().next().getKey();
		JsonArray ja = jsonObject.getAsJsonArray(type);
		return getCacheEntries(originalKey, baseUri, type, ja);
	}

	/**
	 * Generates the CacheEntries for all of the nested objects in the JsonArray
	 * @param originalKey the top aai-node-type
	 * @param baseUri the base "parent" url used to populate full uri
	 * @param ja the JsonArray to be scanned
	 * @return List of CacheEntries for all of the nested objects in the payload
	 */
	private List<CacheEntry> getCacheEntries(String originalKey, String baseUri, String type, JsonArray ja) {
		final List<CacheEntry> cacheEntries = new ArrayList<>(ja.size());
		final Set<String> uris = new HashSet<>(ja.size());
		for (JsonElement jsonElement : ja) {
			String uri;
			JsonObject jo = jsonElement.getAsJsonObject();
			if (RELATIONSHIP.equals(type)) {
				uri = jo.get("related-link").getAsString().replaceAll("/aai/v\\d+", "");
			} else {
				uri = baseUri + aaiResourcesUriTemplates.getUri(type, jo);
			}
			// checks for nested dupes and does not create a cache entry for them
			if (uris.contains(uri)) {
				continue;
			} else {
				uris.add(uri);
			}
			CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
					.withId(uri)
					.inCollection(originalKey)
					.withFindQuery(getFindQuery(uri))
					.withPayload(jo)
					.withDbAction(DBAction.INSERT_REPLACE)
					.build();
			cacheEntries.add(cacheEntry);
		}

		return cacheEntries;
	}

	/**
	 * Creates a find query payload
	 * @param uri used as the _id field for lookup
	 * @return the find query JsonObject
	 */
	private JsonObject getFindQuery(String uri) {
		JsonObject jo = new JsonObject();
		jo.addProperty("_id", uri);
		return jo;
	}

}
