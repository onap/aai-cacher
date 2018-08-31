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
package org.onap.aai.cacher.service.helper;

import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.util.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

@Component
public class RestClientHelperService {

    @Autowired
    CacheHelperService chs;

    private RestClient restClient = getRestClient();

    public RestClient getRestClient() {
        try {
            return new RestClient();
        } catch (Exception e) {
            // TODO handle exceptions
            return null;
        }
    }

    /**
     * Given a cacheKey trigger its corresponding rest call
     *
     * @param ck
     * @return ResponseEntity to process
     */
    public ResponseEntity triggerRestCall(CacheKey ck) {
        // populated cacheKey with mongo variables
        CacheKey ckPopulated = chs.retrieveCacheKeyObject(ck);
        ResponseEntity resp = null;
        // Check to see if the cache key object is fully populated or an empty
        // identifier object
        // if it's an empty identifier object pull the entire object
        if ("-1".equals(ck.getBaseUrl())) {
            ck = chs.retrieveCacheKeyObject(ck);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ");
        String dateFormatted = formatter.format(System.currentTimeMillis());
        ck.setLastSyncStartTime(dateFormatted);
        chs.updateCacheKey(ck);
        try {
            resp = this.restClient.get(ckPopulated.getBaseUrl(), ckPopulated.getModule(), ckPopulated.getURI(),
                    "AAI-CACHER");
        } catch (Exception e) {
            // TODO log an exception
        }
        return resp;
    }
}
