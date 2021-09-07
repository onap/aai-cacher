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
package org.onap.aai.cacher.service.tasks;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.onap.aai.cacher.common.CacheKeyConfig;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.dmaap.consumer.AAIDmaapEventProcessor;
import org.onap.aai.cacher.dmaap.consumer.AAIEventConsumer;
import org.onap.aai.cacher.dmaap.consumer.DmaapConsumerSingleton;
import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.service.helper.CacheHelperService;
import org.onap.aai.cacher.service.helper.RestClientHelperService;
import org.onap.aai.cacher.util.AAIConstants;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.util.AAIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class ScheduledTasks {
    private static final EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(ScheduledTasks.class);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private int checkInterval = -1;
    private boolean cacheLoaded = false;

    @Autowired
    AAIDmaapEventProcessor aaiDmaapEventProcessor;

    @Autowired
    CacheHelperService chs;

    @Autowired
    RestClientHelperService rchs;

    /**
     * Starts the aaiDmaapEventConsumer task bean init. Will restart 1 min after the
     * previous one ended.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 0)
    public void dmaapAAIDmaapEventProcessor() {
        try {
            dmaapAAIEventProcessorTask();
        } catch (Exception e) {
            EELF_LOGGER.error("ERROR: Exception in scheduled task  [" + e.getMessage() + "].");
            ErrorLogHelper.logException(new AAIException(MongoHelperSingleton.AAI_4000_LBL, e));
        }
    }

    public void dmaapAAIEventProcessorTask() throws Exception {
        String methodName = "dmaapAAIEventProcessorTask()";

        EELF_LOGGER.info("Started fixed rate job dmaapAAIEventProcessor @ " + dateFormat.format(new Date()));
        EELF_LOGGER.debug("started scheduled task for " + methodName + " checkInterval " + checkInterval);
        try {
            int delayCheck = Integer.parseInt(AAIConfig.get("aai.cacher.dmaap.consumer.delayCheck", "0"));
            if (checkInterval > 0 && checkInterval++ < delayCheck) {
                return;
            }
            checkInterval = 1;
            if (AAIConfig.get("aai.cacher.dmaap.consumer.enableEventProcessing").equals("true")) {
                EELF_LOGGER.info("aai.cacher.dmaap.consumer.enableEventProcessing set to true, starting AAIEventConsumer.");
                AAIEventConsumer aec = new AAIEventConsumer("aaiDmaaPEventConsumer.properties", false);
                aec.startProcessing(aaiDmaapEventProcessor);
            } else {
                EELF_LOGGER.info(
                        "aai.cacher.dmaap.consumer.enableEventProcessing set to false, not starting AAIDmaapEventConsumer.");
            }
            // initialize the cache
            if (!cacheLoaded) {
                EELF_LOGGER.info("initializing: Start loading cache @ " + dateFormat.format(new Date()));
                init();
                cacheLoaded = true;
            }

        } catch (Exception e) {
            ErrorLogHelper.logException(new AAIException(MongoHelperSingleton.AAI_4000_LBL, e));
        }
        EELF_LOGGER.info("Completed fixed rate job dmaapAAIEventProcessor @ " + dateFormat.format(new Date()));
    }

    public void init() throws IOException {

        Path path = Paths.get(AAIConstants.INITIAL_CACHEKEY_CONFIG_FILENAME);
        String cacheKeyConfigJson = new String(Files.readAllBytes(path));
        CacheKeyConfig cacheKeyConfig = new CacheKeyConfig(cacheKeyConfigJson);
        List<CacheKey> cacheKeys = cacheKeyConfig.populateCacheKeyList();
        chs.bulkAddCacheKeys(cacheKeys);

        for (CacheKey cacheKey : cacheKeys) {
            if ("onInit".equalsIgnoreCase(cacheKey.getTimingIndicator())) {
                try {
                    EELF_LOGGER.info("initializing: cacheKey " + cacheKey.getCacheKey() + " loading");
                    ResponseEntity respEntity = rchs.triggerRestCall(cacheKey);
                    if (respEntity.getStatusCode().is2xxSuccessful()) {
                        Response resp = chs.populateCache(cacheKey, (String) respEntity.getBody());
                        if (resp != null) {
                            if (resp.getStatus() == Response.Status.CREATED.getStatusCode()) {
                                EELF_LOGGER.info("initializing: cacheKey " + cacheKey.getCacheKey() + " loaded");
                            } else {
                                EELF_LOGGER.error("unexpected 2xx response status for cacheKey " + cacheKey.getCacheKey()
                                        + " " + resp.getStatusInfo());
                            }
                        }
                    } else {
                        EELF_LOGGER.error("unexpected response status for cacheKey " + cacheKey.getCacheKey() + " "
                                + respEntity.getStatusCode());
                    }
                } catch (Exception e) {
                    EELF_LOGGER.error("exception caught for cacheKey " + cacheKey.getCacheKey());
                    ErrorLogHelper.logException(new AAIException(MongoHelperSingleton.AAI_4000_LBL, e));
                }
            }
        }
        EELF_LOGGER.info("initializing: cache completed @ " + dateFormat.format(new Date()));
        DmaapConsumerSingleton.getInstance().setProcessEvents(true);
    }
}
