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
package org.onap.aai.cacher.common;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.DBCollectionUpdateOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates and returns a mongo instance
 */

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MongoHelperSingleton {

    private final static EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(MongoHelperSingleton.class);

    private DB db;

    private MongoDatabase mongoDatabase;

    @Autowired
    public MongoHelperSingleton(DB db, MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
        this.db = db;
    }

    public DB getDb() {
        return db;
    }

    public void createCollection(String name) {
        try {
            db.getCollection(name);
            EELF_LOGGER.info("Collection " + name + " created successfully");
        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000");
            ErrorLogHelper.logException(aaiException);
        }
    }

    public boolean addToMongo(String collectionName, Object document) {
        try {
            DBCollection collection = db.getCollection(collectionName);
            WriteResult result;
            if (document instanceof List) {
                result = collection.insert((List<BasicDBObject>) document);
                return result.wasAcknowledged();
            } else if (document instanceof BasicDBObject) {
                result = collection.insert((BasicDBObject) document);
                return result.wasAcknowledged();
            } else {
                EELF_LOGGER.error("The cachekey object to add was of unknown type");
                return false;
            }
        } catch (MongoException ex) {
            AAIException aaiException = new AAIException("AAI_5105");
            ErrorLogHelper.logException(aaiException);
            return false;
        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000");
            ErrorLogHelper.logException(aaiException);
            return false;
        }
    }

    public boolean updateInMongo(String collectionName, BasicDBObject searchQuery, Object document,
            DBCollectionUpdateOptions updateOptions) {
        try {
            DBCollection collection = db.getCollection(collectionName);
            WriteResult result;
            result = collection.update(searchQuery, (BasicDBObject) document, updateOptions);
            return result.wasAcknowledged();
        } catch (MongoException ex) {
            AAIException aaiException = new AAIException("AAI_5105");
            ErrorLogHelper.logException(aaiException);
            return false;
        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000");
            ErrorLogHelper.logException(aaiException);
            return false;
        }
    }

    public String deleteFromMongo(String collectionName, Map<String, String> whereClause) {
        DBCollection collection = db.getCollection(collectionName);
        DBObject searchQuery = new BasicDBObject();
        for (Map.Entry<String, String> entry : whereClause.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            searchQuery.put(key, value);
        }
        try {
            WriteResult result = collection.remove(searchQuery);
            if (result.getN() > 0) {
                return "DELETED";
            } else {
                return "NOT_FOUND";
            }
        } catch (MongoException ex) {
            AAIException aaiException = new AAIException("AAI_5105");
            ErrorLogHelper.logException(aaiException);
            return "EXCEPTION_THROWN";
        }
    }

    public void dropCollection(String collectionName) {
        db.getCollection(collectionName).drop();
    }

    public Response buildResponse(Status status, String result) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(result).build();
    }

    public Response buildExceptionResponse(AAIException aaiException) {
        ErrorLogHelper.logException(aaiException);
        return Response.status(aaiException.getErrorObject().getHTTPResponseCode())
                .entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(
                        Lists.newArrayList(MediaType.APPLICATION_JSON_TYPE), aaiException, new ArrayList<>()))
                .build();
    }

    public boolean insertReplace(CacheEntry cacheEntry) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(cacheEntry.getCollection());

        Document findQuery = Document.parse(cacheEntry.getFindQuery().toString());
        Document payload = Document.parse(cacheEntry.getPayload().toString());

        if (!cacheEntry.isNested()) {
            UpdateResult updateResult = collection.replaceOne(findQuery, payload, new UpdateOptions().upsert(true));

            return updateResult.wasAcknowledged();
        } else {
            CacheEntry localCacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry().deepCopy(cacheEntry).build();
            Document nestedFind = Document.parse(localCacheEntry.getNestedFind().toString());

            // if exists remove
            if (collection.count(nestedFind) > 0L) {
                mongoPull(localCacheEntry, collection, nestedFind);
            }

            ArrayList<Document> filters = this.getFiltersAndUpdateNestedField(localCacheEntry);
            Document doc = new Document();
            doc.put(localCacheEntry.getNestedField(), payload);
            Document push = new Document();
            push.put("$push", doc);

            collection.findOneAndUpdate(findQuery, push,
                    new FindOneAndUpdateOptions().arrayFilters(filters).upsert(true));

            return collection.count(nestedFind) > 0L;
        }
    }

    public boolean delete(CacheEntry cacheEntry) {

        MongoCollection<Document> collection = mongoDatabase.getCollection(cacheEntry.getCollection());

        Document findQuery = Document.parse(cacheEntry.getFindQuery().toString());

        if (!cacheEntry.isNested()) {
            if (collection.count(findQuery) == 0L) {
                return true;
            }
            DeleteResult deleteResult = collection.deleteOne(findQuery);
            return deleteResult.wasAcknowledged();

        } else {
            Document nestedFind = Document.parse(cacheEntry.getNestedFind().toString());
            if (collection.count(nestedFind) == 0L) {
                return true;
            }

            mongoPull(cacheEntry, collection, nestedFind);
            return collection.count(nestedFind) == 0L;

        }
    }

    public Optional<Document> getObject(CacheEntry cacheEntry) {
        MongoCollection<Document> collection = mongoDatabase.getCollection(cacheEntry.getCollection());
        Document resultingObject;

        if (!cacheEntry.isNested()) {
            resultingObject = collection.find(Document.parse(cacheEntry.getFindQuery().toString())).first();
        } else {
            List<Document> aggregate = getAggregateFromFind(cacheEntry.getNestedFind());
            AggregateIterable<Document> aggregateResult = collection.aggregate(aggregate);
            resultingObject = aggregateResult.first();
            if (resultingObject != null) {
                resultingObject = (Document) (resultingObject.get("output"));
            }
        }

        return Optional.ofNullable(resultingObject);
    }

    private List<Document> getAggregateFromFind(JsonObject nestedFind) {
        Document nestedFindDocument = Document.parse(nestedFind.toString());
        List<Document> aggregate = new ArrayList<>();
        List<String> keys = new ArrayList<>(nestedFindDocument.keySet());
        for (int i = 0; i < keys.size(); i++) {
            if (!keys.get(i).contains(".")) {
                aggregate.add(new Document("$match", new Document(keys.get(i), nestedFindDocument.get(keys.get(i)))));
            } else {
                aggregate.add(new Document("$unwind", "$" + keys.get(i).substring(0, keys.get(i).lastIndexOf('.'))));
                aggregate.add(new Document("$match", new Document(keys.get(i), nestedFindDocument.get(keys.get(i)))));
            }
            if (i == keys.size() - 1) {
                aggregate.add(new Document("$project", new Document("_id", 0).append("output",
                        "$" + keys.get(i).substring(0, keys.get(i).lastIndexOf('.')))));
            }
        }
        return aggregate;
    }

    protected void mongoPull(CacheEntry cacheEntry, MongoCollection<Document> collection, Document nestedFind) {
        CacheEntry localCacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry().deepCopy(cacheEntry).build();
        ArrayList<Document> filters = this.getFiltersAndUpdateNestedField(localCacheEntry);

        Document pullObj = new Document();
        pullObj.put(localCacheEntry.getNestedField(),
                Document.parse(localCacheEntry.getNestedFieldIdentifierObj().toString()));
        Document pull = new Document();
        pull.put("$pull", pullObj);
        collection.findOneAndUpdate(nestedFind, pull, new FindOneAndUpdateOptions().arrayFilters(filters).upsert(true));
        // TODO remove wrapping if there are no entries in array.

    }

    private ArrayList<Document> getFiltersAndUpdateNestedField(CacheEntry cacheEntry) {

        if (StringUtils.countMatches(cacheEntry.getNestedField(), ".$.") < 2) {
            return new ArrayList<>();
        }

        ArrayList<Document> filters = new ArrayList<>();
        List<String> keys = cacheEntry.getNestedFind().entrySet().stream().map(Map.Entry::getKey)
                .filter(s -> !s.equals("_id")).sorted((s, t1) -> {
                    if (StringUtils.countMatches(s, ".") > StringUtils.countMatches(t1, ".")) {
                        return 1;
                    }
                    return s.compareTo(t1);
                }).collect(Collectors.toList());
        String filterKey;
        String filterValue;
        String key;
        char filterIndex = 'a';
        StringBuilder newNestedField = new StringBuilder();
        List<String> fieldSplit = Arrays.asList(cacheEntry.getNestedField().split("\\.\\$"));
        for (int i = 0; i < fieldSplit.size(); i++) {
            final String subSplit = StringUtils.join(fieldSplit.subList(0, i + 1), "");
            key = keys.stream().filter(s -> s.startsWith(subSplit)).findFirst().get();
            filterIndex += i;
            filterKey = filterIndex + "." + key.substring(key.lastIndexOf(".") + 1);
            filterValue = cacheEntry.getNestedFind().get(key).getAsString();
            newNestedField.append(fieldSplit.get(i));
            if (i + 1 != fieldSplit.size()) {
                newNestedField.append(".$[").append(filterIndex).append("]");
                filters.add(new Document().append(filterKey, filterValue));
            }

        }
        cacheEntry.setNestedField(newNestedField.toString());

        return filters;
    }

}
