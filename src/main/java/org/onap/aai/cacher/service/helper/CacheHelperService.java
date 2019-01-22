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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.collect.Lists;
import com.google.gson.*;
import com.mongodb.*;

import org.apache.commons.lang3.StringUtils;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.egestion.printer.PayloadPrinterService;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.util.AAIConstants;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CacheHelperService {

    private final static EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(CacheHelperService.class);
    private Gson gson = new GsonBuilder().create();

    @Autowired
    private MongoHelperSingleton mongoHelper;

    @Autowired
    private RestClientHelperService rchs;

    @Autowired
    private PayloadParserService payloadParserService;

    @Autowired
    private PayloadPrinterService payloadPrinterService;

    public void setMongoHelper(MongoHelperSingleton mongoHelper) {
        this.mongoHelper = mongoHelper;
    }

    public void setRchs(RestClientHelperService rchs) {
        this.rchs = rchs;
    }

    public void setPayloadParserService(PayloadParserService payloadParserService) {
        this.payloadParserService = payloadParserService;
    }

    public void setPayloadPrinterService(PayloadPrinterService payloadPrinterService) {
        this.payloadPrinterService = payloadPrinterService;
    }

    public CacheKey retrieveCacheKeyObject(CacheKey ck) {
        String ckString = retrieveCollectionString(ck, AAIConstants.COLLECTION_CACHEKEY);
        if (ckString.equals("")) {
            EELF_LOGGER.error("Could not retrieve cache key");
            return null;
        }
        JsonParser parser = new JsonParser();
        JsonObject ckJson = (JsonObject) parser.parse(ckString);
        return CacheKey.fromJson(ckJson);
    }

    public String retrieveCollectionString(CacheKey ck, String collectionName) {
        StringBuilder result = new StringBuilder("");
        try {
            DBCollection collection = mongoHelper.getDb().getCollection(collectionName);
            BasicDBObject whereQuery = new BasicDBObject();
            whereQuery.put("_id", ck.getCacheKey());
            DBCursor cursor = collection.find(whereQuery);
            while (cursor.hasNext()) {
                result.append(cursor.next());
            }
        } catch (Exception e) {
            e.printStackTrace();
            ErrorLogHelper.logException(new AAIException("AAI_4000", e));
        }
        return result.toString();
    }

    public boolean isCollectionPresent(String collectionName) {
        if (collectionName != null && !collectionName.isEmpty()) {
            try {
                DBCollection collection = mongoHelper.getDb().getCollection(collectionName);
                DBCursor cursor = collection.find();
                if (cursor.count() > 0) {
                    return true;
                }
            } catch (Exception e) {
                ErrorLogHelper.logException(new AAIException("AAI_4000", e));
            }
        }
        return false;
    }

    public String retrieveCollectionString(CacheKey ck) {
        JsonArray jsonArray = new JsonArray();
        try {
            DBCollection collection = mongoHelper.getDb().getCollection(ck.getCacheKey());
            DBCursor cursor = collection.find();
            if (cursor.count() > 0) {
                while (cursor.hasNext()) {
                    // remove "_id" property from cache response
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObj = (JsonObject) parser.parse(cursor.next().toString());
                    jsonArray.add(jsonObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ErrorLogHelper.logException(new AAIException("AAI_4000", e));
        }
        JsonObject jsonObject = payloadPrinterService.createJson(ck.getCacheKey(), jsonArray, ck.getParserStrategy());
        if (jsonObject != null) {
            return jsonObject.toString();
        }
        return "";
    }

    public boolean isKeyPresent(CacheKey ck, String collectionName) {
        return !retrieveCollectionString(ck, collectionName).equals("");
    }

    public boolean isCurrentlyRunning(CacheKey ck) {
        CacheKey ckPopulated = retrieveCacheKeyObject(ck);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ");
        Long syncStartTimeInMillis = -1L;
        Long syncLastEndInMillis = -1L;
        if (ckPopulated != null && !ckPopulated.getLastSyncStartTime().equals("-1")) {
            try {
                syncStartTimeInMillis = sdf.parse(ckPopulated.getLastSyncStartTime()).getTime();
            } catch (Exception e) {
                // TODO handle exceptions
            }
        }
        if (ckPopulated != null && !ckPopulated.getLastSyncEndTime().equals("-1")) {
            try {
                syncLastEndInMillis = sdf.parse(ckPopulated.getLastSyncEndTime()).getTime();
            } catch (Exception e) {
                // TODO handle exceptions
            }
        }
        return ckPopulated != null && syncLastEndInMillis < syncStartTimeInMillis;
    }

    public Response getData(CacheKey ck) {
        if (ck == null) {
            AAIException aaiException = new AAIException("AAI_3014", "Cache key provided does not exist");
            return buildExceptionResponse(aaiException);
        } else if (isCurrentlyRunning(ck)) {
            AAIException aaiException = new AAIException("AAI_4000", "Sync is currently running from another process.");
            return buildExceptionResponse(aaiException);
        } else if (isKeyPresent(ck, AAIConstants.COLLECTION_CACHEKEY)) {
            if (isCollectionPresent(ck.getCacheKey())) {
                return retrieveCollectionByKey(ck);
            } else {
                ResponseEntity resp = rchs.triggerRestCall(ck);
                if (!resp.getStatusCode().is2xxSuccessful()) {
                    // TODO log/return accordingly
                }
                Response response = populateCache(ck, (String) resp.getBody());
                if (response.getStatus() == 201) {
                    return retrieveCollectionByKey(ck);
                } else {
                    AAIException aaiException = new AAIException("AAI_5105");
                    return buildExceptionResponse(aaiException);
                }
            }
        } else {
            AAIException aaiException = new AAIException("AAI_3014", "Cache key provided does not exist");
            return buildExceptionResponse(aaiException);
        }
    }

    public Response forceSync(CacheKey ck) {
        if (isCurrentlyRunning(ck)) {
            AAIException aaiException = new AAIException("AAI_3000", "Sync is currently running from another process.");
            ArrayList<String> templateVars = new ArrayList();
            templateVars.add("/sync");
            templateVars.add(ck.getCacheKey());
            return buildExceptionResponse(aaiException, templateVars);
        } else if (isKeyPresent(ck, AAIConstants.COLLECTION_CACHEKEY)) {
            // populate cache and return status on sync
            ResponseEntity resp = rchs.triggerRestCall(ck);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                // TODO unsure if this is correct behavior
                return Response.noContent().build();
            }
            return populateCache(ck, (String) resp.getBody());
        } else {
            AAIException aaiException = new AAIException("AAI_3014", "Cache key provided does not exist");
            return buildExceptionResponse(aaiException);
        }
    }

    public Response retrieveCollectionByKey(CacheKey ck, String collection) {
        Status status = Status.OK;
        String result = "";
        try {
            result = this.retrieveCollectionString(ck, collection);

            if (result.equals("")) {
                EELF_LOGGER.error("Cannot not find the  cache key from mongodb " + ck.getCacheKey());
                AAIException aaiException = new AAIException("AAI_3001", "cacheKey request");
                ArrayList<String> templateVars = new ArrayList();
                templateVars.add("/get");
                templateVars.add(ck.getCacheKey());
                return buildExceptionResponse(aaiException, templateVars);
            }
            return this.buildResponse(status, result);
        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000", e);
            return buildExceptionResponse(aaiException);

        }
    }

    public Response retrieveCollectionByKey(CacheKey ck) {
        Status status = Status.OK;
        String result = "";
        try {
            result = this.retrieveCollectionString(ck);
            if (result.equals("")) {
                status = Status.NOT_FOUND;
                EELF_LOGGER.error("Cannot not found the  cache key from mongodb");
                AAIException aaiException = new AAIException("AAI_3001", "cacheKey get for" + ck.getCacheKey() );
                return buildExceptionResponse(aaiException);
            }
            return this.buildResponse(status, result);
        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000", e);
            return buildExceptionResponse(aaiException);

        }
    }

    public boolean addCacheKey(CacheKey ck) {
        return mongoHelper.addToMongo(AAIConstants.COLLECTION_CACHEKEY, ck.toDBObject());
    }

    public Response getAllKeys() {
        Status status = Status.OK;
        StringBuilder result = new StringBuilder();
        try {
            DBCollection collection = mongoHelper.getDb().getCollection(AAIConstants.COLLECTION_CACHEKEY);
            DBCursor cursor = collection.find();
            if (cursor.count() > 1) {
                result.append("[");
                while (cursor.hasNext()) {
                    result.append(cursor.next());
                    if (cursor.numSeen() != cursor.count()) {
                        result.append(",");
                    }
                }
                result.append("]");

            } else if (cursor.count() == 1) {
                while (cursor.hasNext()) {
                    result.append(cursor.next());
                }
            } else {
                AAIException aaiException = new AAIException("AAI_3001", "cacheKey request");
                ArrayList<String> templateVars = new ArrayList();
                templateVars.add("/get");
                templateVars.add("ALL");
                return buildExceptionResponse(aaiException, templateVars);
            }
            return buildResponse(status, result.toString());
        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000", e);
            return buildExceptionResponse(aaiException);
        }
    }

    public Response updateCacheKey(CacheKey ck) {
        DBCollection collection = mongoHelper.getDb().getCollection(AAIConstants.COLLECTION_CACHEKEY);
        Status status;

        BasicDBObject updateFields = new BasicDBObject();

        for (Field field : ck.getClass().getDeclaredFields()) {
            try {
                String name = field.getName();
                Object value = field.get(ck);
                if (!name.equals(AAIConstants.COLLECTION_CACHEKEY) && !value.equals("-1")) {
                    updateFields.append(name, value);
                }
            } catch (Exception e) {
                EELF_LOGGER.warn("Could not retrieve updatable field from the class", e);
            }
        }

        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);

        BasicDBObject searchQuery = new BasicDBObject("_id", ck.getCacheKey());
        try {
            WriteResult result = collection.update(searchQuery, setQuery);
            if (result.getN() > 0) {
                status = Status.OK;
            } else {
                AAIException aaiException = new AAIException("AAI_3001", "cacheKey request");
                ArrayList<String> templateVars = new ArrayList();
                templateVars.add("/update");
                templateVars.add(ck.getCacheKey());
                return buildExceptionResponse(aaiException, templateVars);
            }
            return buildResponse(status, "{}");
        } catch (MongoException ex) {
            AAIException aaiException = new AAIException("AAI_5105", ex);
            return buildExceptionResponse(aaiException);
        }
    }

    public boolean bulkAddCacheKeys(List<CacheKey> ckList) {
        try {
            List<BasicDBObject> documents = new ArrayList<BasicDBObject>();
            for (CacheKey ck : ckList) {
                documents.add(ck.toDBObject());
            }
            return mongoHelper.addToMongo(AAIConstants.COLLECTION_CACHEKEY, documents);
        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000", e);
            ErrorLogHelper.logException(aaiException);
            return false;
        }
    }

    public Response deleteCacheKeyAndAssociatedCache(String id) {
        String cacheDelete = deleteFromCollection(null, id);
        dropCollection(id);
        String cacheKeyDelete = deleteFromCollection(id, AAIConstants.COLLECTION_CACHEKEY);
        Status status;
        if (cacheKeyDelete.equals("DELETED") && (cacheDelete.equals("DELETED") || cacheDelete.equals("NOT_FOUND"))) {
            status = Status.NO_CONTENT;
            return buildResponse(status, "{}");
        } else if (cacheKeyDelete.equals("NOT_FOUND")) {
            AAIException aaiException = new AAIException("AAI_3001", "cacheKey request");
            ArrayList<String> templateVars = new ArrayList();
            templateVars.add("/delete");
            templateVars.add(id);
            return buildExceptionResponse(aaiException, templateVars);
        } else {
            AAIException aaiException = new AAIException("AAI_5105");
            return buildExceptionResponse(aaiException);
        }
    }

    public Response deleteCache(String id, String collection) {
        String cacheDelete = deleteFromCollection(id, collection);
        Status status;
        if (cacheDelete.equals("DELETED")) {
            status = Status.NO_CONTENT;
            return buildResponse(status, "{}");
        } else if (cacheDelete.equals("NOT_FOUND")) {
            AAIException aaiException = new AAIException("AAI_3001", "cacheKey request");
            ArrayList<String> templateVars = new ArrayList();
            templateVars.add("/delete");
            templateVars.add(id);
            return buildExceptionResponse(aaiException, templateVars);
        } else {
            AAIException aaiException = new AAIException("AAI_5105");
            return buildExceptionResponse(aaiException);
        }
    }

    public String deleteFromCollection(String id, String collection) {
        Map<String, String> whereClause = new HashMap<>();
        if (id != null) {
            whereClause.put("_id", id);
        }
        return mongoHelper.deleteFromMongo(collection, whereClause);
    }

    public void dropCollection(String collection) {
        mongoHelper.dropCollection(collection);
    }

    public Response populateCache(CacheKey ck, String responseBody) {
        // Check to see if the cache key object is fully populated or an empty
        // identifier object
        // if it's an empty identifier object pull the entire object
        if (ck.getBaseUrl().equals("-1")) {
            ck = retrieveCacheKeyObject(ck);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ");

        List<CacheEntry> cacheEntries = payloadParserService.doParse(ck.getCacheKey(), responseBody,
                ck.getParserStrategy());
        for (CacheEntry cacheEntry : cacheEntries) {
            boolean success = false;

            switch (cacheEntry.getDbAction()) {
            case DELETE:
                success = mongoHelper.delete(cacheEntry);
                break;
            case UPDATE:
                success = mongoHelper.insertReplace(cacheEntry);
                break;
            case INSERT_REPLACE:
                success = mongoHelper.insertReplace(cacheEntry);
                break;
            }

            if (!success) {
                ck.setLastSyncEndTime(formatter.format(System.currentTimeMillis()));
                updateCacheKey(ck);
                AAIException aaiException = new AAIException("AAI_4000", "Unable to populate the cache");
                return buildExceptionResponse(aaiException);
            }
        }
        ck.setLastSyncSuccessTime(formatter.format(System.currentTimeMillis()));
        ck.setLastSyncEndTime(formatter.format(System.currentTimeMillis()));
        updateCacheKey(ck);
        return buildResponse(Status.CREATED, null);
    }

    public Response buildResponse(Status status, String result) {
        
        if ( result == null ) {
            return Response.status(status).type(MediaType.APPLICATION_JSON).build();
        }
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(result).build();
    }

    public Response buildMissingFieldResponse(List<String> fields) {
        AAIException aaiException = new AAIException("AAI_6118");
        ArrayList<String> templateVars = new ArrayList<>();
        templateVars.add(StringUtils.join(fields, ", "));
        ErrorLogHelper.logException(aaiException);
        return Response.status(aaiException.getErrorObject().getHTTPResponseCode())
                .entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(
                        Lists.newArrayList(MediaType.APPLICATION_JSON_TYPE), aaiException, templateVars))
                .build();
    }

    public Response buildValidationResponse(List<String> issues) {
        AAIException aaiException = new AAIException("AAI_3014");
        ArrayList<String> templateVars = new ArrayList<>();
        
        if (templateVars.isEmpty()) {
            templateVars.add(issues.toString());
        }
        ErrorLogHelper.logException(aaiException);
        return Response.status(aaiException.getErrorObject().getHTTPResponseCode())
                .entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(
                        Lists.newArrayList(MediaType.APPLICATION_JSON_TYPE), aaiException, templateVars))
                .build();
    }    

    public Response buildExceptionResponse(AAIException aaiException, ArrayList templateVars) {
        ErrorLogHelper.logException(aaiException);
        return Response.status(aaiException.getErrorObject().getHTTPResponseCode())
                .entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(
                        Lists.newArrayList(MediaType.APPLICATION_JSON_TYPE), aaiException, templateVars))
                .build();
    }
    
    public Response buildExceptionResponse(AAIException aaiException) {
        ErrorLogHelper.logException(aaiException);
        return Response.status(aaiException.getErrorObject().getHTTPResponseCode())
                .entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(
                        Lists.newArrayList(MediaType.APPLICATION_JSON_TYPE), aaiException, new ArrayList()))
                .build();
    }

    public List<CacheKey> getScheduledCaches() {
        List<CacheKey> cks = new ArrayList<>();
        EELF_LOGGER.info("Retrieving scheduled cache keys");
        DBCollection collection = mongoHelper.getDb().getCollection(AAIConstants.COLLECTION_CACHEKEY);
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("timingIndicator", "scheduled");
        DBCursor cursor = collection.find(whereQuery);
        while (cursor.hasNext()) {
            JsonObject ckJson = (JsonObject) new JsonParser().parse((cursor.next().toString()));
            CacheKey ck = CacheKey.fromJson(ckJson);
            cks.add(ck);
        }
        return cks;
    }

    public void checkAndInitTasks() {
        List<CacheKey> ckList = this.getScheduledCaches();
        int numOfThread = 10;
        ExecutorService taskExecutor = Executors.newFixedThreadPool(numOfThread);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (CacheKey ck : ckList) {

                boolean shouldTrigger = isShouldTrigger(ck);

                if (shouldTrigger) {
                    Callable<Void> task = new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            long startTimeV = System.currentTimeMillis();
                            ResponseEntity respEntity = rchs.triggerRestCall(ck);
                            if (respEntity.getStatusCode().is2xxSuccessful()) {
                                populateCache(ck, (String) respEntity.getBody());
                                long endTimeV = System.currentTimeMillis();
                                EELF_LOGGER.info("Elapsed time in seconds: " + (endTimeV - startTimeV) / 1000);
                            } else {
                                // TODO: cache update failed
                            }
                            return null;
                        }
                    };
                    if (task != null) {
                        tasks.add(task);
                    }
                }
            }
            if (!tasks.isEmpty()) {
                taskExecutor.invokeAll(tasks);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // TODO throw exception
        } finally {
            taskExecutor.shutdown();
        }
    }

    protected boolean isShouldTrigger(CacheKey ck) {

        // convert minutes to milliseconds for the interval
        int interval = Integer.parseInt(ck.getSyncInterval()) * 60000;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ");
        long syncStartTimeInMillis = Integer.MAX_VALUE;
        long syncLastEndInMillis = Integer.MIN_VALUE;

        if ("-1".equals(ck.getLastSyncStartTime())) {
            return true;
        } else {
            try {
                syncStartTimeInMillis = sdf.parse(ck.getLastSyncStartTime()).getTime();
            } catch (Exception e) {
                e.printStackTrace();
                // TODO handle exceptions
            }
        }

        if (!"-1".equals(ck.getLastSyncEndTime())) {
            try {
                syncLastEndInMillis = sdf.parse(ck.getLastSyncEndTime()).getTime();
            } catch (Exception e) {
                e.printStackTrace();
                // TODO handle exceptions
            }
        }

        return ((System.currentTimeMillis() - syncStartTimeInMillis) > interval)
                && (syncStartTimeInMillis < syncLastEndInMillis);
    }
}
