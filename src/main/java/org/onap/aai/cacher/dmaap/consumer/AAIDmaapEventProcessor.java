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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mongodb.MongoCommandException;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.egestion.printer.PayloadPrinterService;
import org.onap.aai.cacher.egestion.printer.strategy.PayloadPrinterType;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserType;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AAIDmaapEventProcessor implements DmaapProcessor {

    private static EELFLogger LOGGER = EELFManager.getInstance().getLogger(AAIEventConsumer.class);

    private final JsonParser parser = new JsonParser();

    private MongoHelperSingleton mongoHelper;
    private PayloadParserService payloadParserService;
    private PayloadPrinterService payloadPrinterService;

    @Autowired
    public AAIDmaapEventProcessor(MongoHelperSingleton mongoHelper, PayloadParserService payloadParserService, PayloadPrinterService payloadPrinterService) {
        this.mongoHelper = mongoHelper;
        this.payloadParserService = payloadParserService;
        this.payloadPrinterService = payloadPrinterService;
    }

    public AAIDmaapEventProcessor(){}

    /**
     * 
     * @param eventMessage
     * @return
     */
    public void process(String eventMessage) throws Exception {
        JsonObject event;
        JsonObject eventHeader;

        try {
            LOGGER.debug("Processing event: " + eventMessage);
            event = parser.parse(eventMessage).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException je) {
            LOGGER.error("ERROR: Event is not valid JSON [" + eventMessage + "].");
            ErrorLogHelper.logException(new AAIException("AAI_4000", je));
            throw je;
        }

        try {
            LOGGER.debug("Validating event header.");
            eventHeader = this.getEventHeader(event);
        } catch (JsonSyntaxException | IllegalStateException je) {
            LOGGER.error("ERROR: Event header is not valid [" + eventMessage + "].");
            ErrorLogHelper.logException(new AAIException("AAI_4000", je));
            throw je;
        }

        try {
            LOGGER.debug("Processing entity.");
            validateEventBody(event);
        } catch (JsonSyntaxException | IllegalStateException je) {
            LOGGER.error("ERROR: Event body is not valid JSON [" + eventMessage + "].");
            ErrorLogHelper.logException(new AAIException("AAI_4000", je));
            throw je;
        }

        JsonObject dmaapJson = parser.parse(eventMessage).getAsJsonObject();

        // Get existing object if is update
        if (eventHeader != null && "UPDATE".equals(eventHeader.get("action").getAsString())) {
            String uri = eventHeader.get("entity-link").getAsString()
                    .replaceAll("/aai/v\\d+", "");
            String type = eventHeader.getAsJsonObject().get("entity-type").getAsString();

            List<JsonObject> found = mongoHelper.findAllWithIdsStartingWith(type, uri);
            if (!found.isEmpty()) {
                JsonArray ja = new JsonArray();
                found.forEach(ja::add);
                JsonObject existing = payloadPrinterService.createJson(type, ja, PayloadPrinterType.AAI_RESOURCE_GET_ALL_PRINTER);
                dmaapJson.add("existing-obj", existing);
            }
        }

        List<CacheEntry> dmaapCacheEntries = payloadParserService.doParse("aai-dmaap",
                dmaapJson, PayloadParserType.AAI_RESOURCE_DMAAP);

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

    private void validateEventBody(JsonObject event) {
        if (!event.has("entity")) {
            throw new JsonSyntaxException("Event header missing.");
        }

        event.get("entity").getAsJsonObject();
    }

    /**
     * Validates that the event header has the id and source name for processing.
     * (needed for status response msg)
     * 
     * @param event
     * @throws JsonSyntaxException
     */
    private JsonObject getEventHeader(JsonObject event) throws JsonSyntaxException {

        if (!event.has("event-header")) {
            throw new JsonSyntaxException("Event header missing.");
        }
        JsonObject eventHeader = event.get("event-header").getAsJsonObject();
        if (eventHeader.get("id") == null || eventHeader.get("id").getAsString().isEmpty()) {
            throw new JsonSyntaxException("Event header id missing.");
        } else if (eventHeader.get("source-name") == null || eventHeader.get("source-name").getAsString().isEmpty()) {
            throw new JsonSyntaxException("Event header source-name missing.");
        }
        return eventHeader;

    }

}
