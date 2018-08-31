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
import com.google.gson.JsonParser;
import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.service.helper.CacheHelperService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/cache/v1")
@Produces({ MediaType.APPLICATION_JSON })
public class CacheInteractionService {

    private final static EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(CacheKeyService.class);

    @Autowired
    protected CacheHelperService chs;

    /**
     * Delete the associated cache and handle responses
     * 
     * @param payload, requires the cache key to delete the cache
     */
    @DELETE
    @Path("/delete")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeDelete(String payload) {
        CacheKey ck = CacheKey.fromJson(new JsonParser().parse(payload).getAsJsonObject());
        Response resp = chs.deleteCache(null, ck.getCacheKey());
        chs.dropCollection(ck.getCacheKey());
        return resp;
    }

    /**
     * Sync the cache from the provided cache key and handle responses
     * 
     * @param payload, needs the cache key in the request payload to force sync the
     *        cache
     */
    @PUT
    @Path("/sync")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeSync(String payload) {
        CacheKey ck = CacheKey.fromJson(new JsonParser().parse(payload).getAsJsonObject());
        return chs.forceSync(ck);
    }

    /**
     * Execute to build and return the payload for the provided cache key
     * 
     * @param cacheKey, needs the cacheKey to know which response payload to build
     *        and return
     */
    @GET
    @Path("/get")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response executeGetSingle(@DefaultValue("-1") @QueryParam("cacheKey") String cacheKey) {
        CacheKey ck = new CacheKey(cacheKey);
        ck = chs.retrieveCacheKeyObject(ck);
        return chs.getData(ck);
    }
}