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
package org.onap.aai.cacher.dmaap.consumer;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoCommandException;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserType;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AAIDmaapEventProcessor implements DmaapProcessor {

    private static EELFLogger LOGGER = EELFManager.getInstance().getLogger(AAIEventConsumer.class);

    private final JsonParser parser = new JsonParser();

    private JSONObject event;
    private JSONObject eventHeader;
    private JSONObject eventBody;

    private MongoHelperSingleton mongoHelper;
    private PayloadParserService payloadParserService;

    @Autowired
    public AAIDmaapEventProcessor(MongoHelperSingleton mongoHelper, PayloadParserService payloadParserService) {
        this.mongoHelper = mongoHelper;
        this.payloadParserService = payloadParserService;
    }

    public AAIDmaapEventProcessor() {
    }

    /**
     * 
     * @param eventMessage
     * @return
     */
    public void process(String eventMessage) throws Exception {
        this.event = null;
        this.eventHeader = null;
        this.eventBody = null;

        try {
            LOGGER.debug("Processing event: " + eventMessage);
            this.event = new JSONObject(eventMessage);
        } catch (JSONException je) {
            LOGGER.error("ERROR: Event is not valid JSON [" + eventMessage + "].");
            ErrorLogHelper.logException(new AAIException("AAI_4000", je));
            throw je;
        }

        try {
            LOGGER.debug("Validating event header.");
            this.validateEventHeader(this.event);
        } catch (JSONException je) {
            LOGGER.error("ERROR: Event header is not valid [" + eventMessage + "].");
            ErrorLogHelper.logException(new AAIException("AAI_4000", je));
            throw je;
        }

        try {
            LOGGER.debug("Processing entity.");
            eventBody = this.event.getJSONObject("entity");
        } catch (JSONException je) {
            LOGGER.error("ERROR: Event body is not valid JSON [" + eventMessage + "].");
            ErrorLogHelper.logException(new AAIException("AAI_4000", je));
            throw je;
        }

        List<CacheEntry> dmaapCacheEntries = payloadParserService.doParse("aai-dmaap",
                parser.parse(eventMessage).getAsJsonObject(), PayloadParserType.AAI_RESOURCE_DMAAP);

        // Get existing object if is update
        Optional<Document> existingObj = Optional.empty();
        if (this.eventHeader != null && "UPDATE".equals(eventHeader.getString("action"))) {
            existingObj = mongoHelper.getObject(dmaapCacheEntries.get(0));
        }

        // Add existing object to payload to be parsed by AAI_RESOURCE_DMAAP parser
        if (existingObj.isPresent()) {
            JsonObject eventMessageObj = parser.parse(eventMessage).getAsJsonObject();
            eventMessageObj.add("existing-obj", parser.parse(existingObj.get().toJson()).getAsJsonObject());
            eventMessage = eventMessageObj.toString();
            dmaapCacheEntries = payloadParserService.doParse("aai-dmaap", parser.parse(eventMessage).getAsJsonObject(),
                    PayloadParserType.AAI_RESOURCE_DMAAP);
        }

        for (CacheEntry cacheEntry : dmaapCacheEntries) {
            try {
                switch (cacheEntry.getDbAction()) {
                case DELETE:
                    mongoHelper.delete(cacheEntry);
                    break;
                case UPDATE:
                    mongoHelper.insertReplace(cacheEntry);
                    break;
                case INSERT_REPLACE:
                    mongoHelper.insertReplace(cacheEntry);
                    break;
                }
            } catch (MongoCommandException exception) {
                LOGGER.warn("Attempted to update nested that does not exist in cache.", exception);
            }
        }

        LOGGER.debug("Event processed successfully.");

    }

    /**
     * Validates that the event header has the id and source name for processing.
     * (needed for status response msg)
     * 
     * @param event
     * @throws JSONException
     */
    private void validateEventHeader(JSONObject event) throws JSONException {
        eventHeader = event.getJSONObject("event-header");
        if (this.eventHeader.getString("id") == null || this.eventHeader.getString("id").isEmpty()) {
            throw new JSONException("Event header id missing.");
        } else if (this.eventHeader.getString("source-name") == null
                || this.eventHeader.getString("source-name").isEmpty()) {
            throw new JSONException("Event header source-name missing.");
        }
    }

    /**
     * 
     * @return
     */
    public JSONObject getEventHeader() {
        return eventHeader;
    }

    /**
     * 
     * @return
     */
    public JSONObject getEventBody() {
        return eventBody;
    }

}
