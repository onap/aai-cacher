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
package org.onap.aai.cacher.service.rest;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.service.helper.CacheHelperService;
import org.onap.aai.cacher.service.rest.util.CacheKeyRequestValidation;
import org.onap.aai.cacher.service.rest.util.CacheKeyRequestValidationType;
import org.onap.aai.cacher.util.AAIConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

@Path("/cacheKey/v1")
@Produces({ MediaType.APPLICATION_JSON })
public class CacheKeyService {
    private final static EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(CacheKeyService.class);

    @Autowired
    protected CacheHelperService chs;

    /**
     * Store the cache key to Mongodb
     *
     * @param payload, the json payload needed to populate the cacheKey object and
     *        add to the database
     */
    @PUT
    @Path("/add")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeAdd(String payload) {
        EELF_LOGGER.info("Got the request to add cache key to mongodb");
        CacheKeyRequestValidation ckrv = new CacheKeyRequestValidation(CacheKeyRequestValidationType.ADD);
        JsonObject input = convertStringToJSON(payload);
        List<String> issues = ckrv.validateCacheKeyRequest(input, chs);
        Response response;
        if (!issues.isEmpty()) {
            response = chs.buildValidationResponse(issues);
        } else {
            CacheKey ck = CacheKey.fromJson(convertStringToJSON(payload));
            boolean addSuccessful = chs.addCacheKey(ck);
            // Since we are adding an onInit, we need to populate its cache
            if (ck.getTimingIndicator().equals("onInit")) {
                chs.forceSync(ck);
            }
            Status status;
            if (addSuccessful) {
                status = Status.CREATED;
            } else {
                EELF_LOGGER.error("Adding of cache key was not successfull");
                status = Status.INTERNAL_SERVER_ERROR;
            }
            response = Response.status(status).type(MediaType.APPLICATION_JSON).build();
        }
        return response;
    }

    /**
     * Update the stored cache key and handle responses
     *
     * @param payload, the json payload needed to populate the cacheKey object and
     *        update the entry in the database
     */
    @PUT
    @Path("/update")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeUpdate(String payload) {
        EELF_LOGGER.info("Got the request to update cache key in mongodb");
        CacheKeyRequestValidation ckrv = new CacheKeyRequestValidation(CacheKeyRequestValidationType.UPDATE);
        JsonObject input = convertStringToJSON(payload);
        List<String> issues = ckrv.validateCacheKeyRequest(input, chs);
        Response response;
        if (!issues.isEmpty()) {
            response = chs.buildValidationResponse(issues);
        } else {
            CacheKey ck = CacheKey.createCacheKeyDefault(input);
            response = chs.updateCacheKey(ck);
        }
        return response;
    }

    /**
     * Delete the cache key and associated cache and handle responses
     *
     * @param payload, the json payload needed to delete the cacheKey entry in the
     *        database
     */
    @DELETE
    @Path("/delete")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeDelete(String payload) {
        EELF_LOGGER.info("Got the request to delete cache key from mongodb");
        CacheKeyRequestValidation ckrv = new CacheKeyRequestValidation(CacheKeyRequestValidationType.DELETE);
        JsonObject input = convertStringToJSON(payload);
        List<String> issues = ckrv.validateCacheKeyRequest(input, chs);
        Response response;
        if (!issues.isEmpty()) {
            response = chs.buildValidationResponse(issues);
        } else {
            CacheKey ck = new CacheKey(getValueFromPayload(input, "cacheKey"));
            response = chs.deleteCacheKeyAndAssociatedCache(ck.getCacheKey());
        }
        return response;
    }

    /**
     * Get the cache key information given a provided cache key, or if no key is
     * provided return all keys
     *
     * @param cacheKey, the string id to match against _id in mongodb as the unique
     *        cache key
     */
    @GET
    @Path("/get")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeGet(@DefaultValue("-1") @QueryParam("cacheKey") String cacheKey) {
        /*
         * Method to get a single cache key entry
         */
        EELF_LOGGER.info("Got the request to get cache key from mongodb");
        CacheKey ck;
        Response response;
        if (cacheKey.equals("-1")) {
            response = chs.getAllKeys();
        } else {
            ck = new CacheKey(cacheKey);
            response = chs.retrieveCollectionByKey(ck, AAIConstants.COLLECTION_CACHEKEY);
        }
        return response;
    }

    /**
     * This method accepts a string converts it into a JsonObject
     *
     * @param payload, the string payload to convert to a JsonObject
     */
    public JsonObject convertStringToJSON(String payload) {
        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(payload);
    }

    /**
     * This method accepts a string payload and extracts the cacheKey
     *
     * @param payload, the string payload to convert to a JsonObject
     */
    public String getValueFromPayload(String payload, String key) {
        JsonObject payloadJSON = convertStringToJSON(payload);
        if (payloadJSON.has(key)) {
            return ((payloadJSON.get(key)).toString()).replaceAll("\"", "");
        } else {
            EELF_LOGGER.error("Could not extract cachekey from the payload");
            return null;
        }
    }

    /**
     * This method accepts a JsonObject input and extracts the cacheKey
     *
     * @param input, the string payload to convert to a JsonObject
     */
    public String getValueFromPayload(JsonObject input, String key) {
        if (input.has(key)) {
            return ((input.get(key)).toString()).replaceAll("\"", "");
        } else {
            EELF_LOGGER.error("Could not extract cachekey from the payload");
            return null;
        }
    }

}