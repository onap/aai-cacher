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
package org.onap.aai.cacher.model;

import com.google.gson.JsonObject;

/**
 * Captures the details of a cache entry to be inserted onto the database
 */
public class CacheEntry {

    protected DBAction dbAction;

    protected String id;
    protected String collection;
    protected JsonObject payload;
    protected JsonObject findQuery;

    protected boolean isNested = false;
    protected boolean isNestedPayloadString = false;
    protected  String nestedString;
    protected String nestedField;
    protected JsonObject nestedFind;
    protected JsonObject nestedFieldIdentifierObj;

    private CacheEntry() {
    }

    public DBAction getDbAction() {
        return dbAction;
    }

    public void setDbAction(DBAction dbAction) {
        this.dbAction = dbAction;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }

    public JsonObject getFindQuery() {
        return findQuery;
    }

    public void setFindQuery(JsonObject findQuery) {
        this.findQuery = findQuery;
    }

    public boolean isNested() {
        return isNested;
    }

    public void setNested(boolean nested) {
        isNested = nested;
    }

    public boolean isNestedPayloadString() {
        return isNestedPayloadString;
    }

    public void setNestedPayloadString(boolean nestedPayloadString) {
        isNestedPayloadString = nestedPayloadString;
    }

    public String getNestedString() {
        return nestedString;
    }

    public void setNestedString(String nestedString) {
        this.nestedString = nestedString;
    }

    public String getNestedField() {
        return nestedField;
    }

    public void setNestedField(String nestedField) {
        this.nestedField = nestedField;
    }

    public JsonObject getNestedFind() {
        return nestedFind;
    }

    public void setNestedFind(JsonObject nestedFind) {
        this.nestedFind = nestedFind;
    }

    public JsonObject getNestedFieldIdentifierObj() {
        return nestedFieldIdentifierObj;
    }

    public void setNestedFieldIdentifierObj(JsonObject nestedFieldIdentifierObj) {
        this.nestedFieldIdentifierObj = nestedFieldIdentifierObj;
    }

    public static final class CacheEntryBuilder {
        private DBAction dbAction;
        private String id;
        private String collection;
        private JsonObject payload;
        private JsonObject findQuery;
        private boolean isNested;
        private String nestedField;
        private JsonObject nestedFind;
        private JsonObject nestedFieldIdentifierObj;
        private boolean isNestedPayloadString = false;
        private  String nestedString;

        private CacheEntryBuilder() {
        }

        public static CacheEntryBuilder createCacheEntry() {
            return new CacheEntryBuilder();
        }

        public CacheEntryBuilder deepCopy(CacheEntry cacheEntry) {
            dbAction = cacheEntry.getDbAction();
            id = cacheEntry.getId();
            collection = cacheEntry.getCollection();
            payload = cacheEntry.getPayload();
            findQuery = cacheEntry.getFindQuery();
            isNested = cacheEntry.isNested();
            nestedField = cacheEntry.getNestedField();
            nestedFind = cacheEntry.getNestedFind();
            nestedFieldIdentifierObj = cacheEntry.getNestedFieldIdentifierObj();
            nestedString  = cacheEntry.getNestedString();
            isNestedPayloadString = cacheEntry.isNestedPayloadString();

            return this;
        }

        public CacheEntryBuilder withDbAction(DBAction dbAction) {
            this.dbAction = dbAction;
            return this;
        }

        public CacheEntryBuilder withId(String id) {
            this.id = id;
            return this;
        }

        public CacheEntryBuilder inCollection(String collection) {
            this.collection = collection;
            return this;
        }

        public CacheEntryBuilder withPayload(JsonObject payload) {
            this.payload = payload;
            return this;
        }

        public CacheEntryBuilder withFindQuery(JsonObject findQuery) {
            this.findQuery = findQuery;
            return this;
        }

        public CacheEntryBuilder isNested(boolean isNested) {
            this.isNested = isNested;
            return this;
        }

        public CacheEntryBuilder isNestedPayloadString(boolean isNestedPayloadString) {
            this.isNestedPayloadString = isNestedPayloadString;
            return this;
        }

        public CacheEntryBuilder withNestedString(String nestedString) {
            this.nestedString = nestedString;
            return this;
        }

        public CacheEntryBuilder withNestedField(String nestedField) {
            this.nestedField = nestedField;
            return this;
        }

        public CacheEntryBuilder withNestedFind(JsonObject nestedFind) {
            this.nestedFind = nestedFind;
            return this;
        }

        public CacheEntryBuilder withNestedFieldIdentifierObj(JsonObject nestedFieldIdentifierObj) {
            this.nestedFieldIdentifierObj = nestedFieldIdentifierObj;
            return this;
        }

        public CacheEntry build() {
            CacheEntry cacheEntry = new CacheEntry();
            cacheEntry.setDbAction(dbAction);
            cacheEntry.setId(id);
            cacheEntry.setCollection(collection);
            cacheEntry.setPayload(payload);
            cacheEntry.setFindQuery(findQuery);
            cacheEntry.setNestedField(nestedField);
            cacheEntry.setNestedFind(nestedFind);
            cacheEntry.setNestedFieldIdentifierObj(nestedFieldIdentifierObj);
            cacheEntry.setNested(this.isNested);
            cacheEntry.setNestedPayloadString(this.isNestedPayloadString);
            cacheEntry.setNestedString(this.nestedString);

            return cacheEntry;
        }
    }
}
