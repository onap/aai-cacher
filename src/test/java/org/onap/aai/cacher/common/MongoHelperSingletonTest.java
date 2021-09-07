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

import com.google.gson.JsonParser;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.cacher.model.DBAction;
import org.skyscreamer.jsonassert.JSONAssert;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MongoHelperSingletonTest {

	private static final String DB_NAME = "testDb";
	private static MongoDatabase mongoDatabase;
	private static DB db;
	private MongoHelperSingleton mongoHelperSingleton;


	@BeforeClass
	public static void setup() {
		MongoServer mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress serverAddress = mongoServer.bind();

		MongoClient client = new MongoClient(new ServerAddress(serverAddress));
		mongoDatabase = client.getDatabase(DB_NAME);
		db = client.getDB(DB_NAME);
	}

	@Before
	public void init() {
		mongoHelperSingleton = new MongoHelperSingleton(db, mongoDatabase);
	}

	@After
	public void cleanup() {
		final List<String> collectionNames = new ArrayList<>();
		mongoDatabase.listCollections().iterator().forEachRemaining(document -> collectionNames.add(document.getString("name")));
		collectionNames.stream().forEach(collectionName -> mongoDatabase.getCollection(collectionName).drop());
	}

	private MongoCollection<Document> setupCollection(String collectionName) {

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery = Document.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'}");
		Document obj = Document.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1','hostname':'testPserver_1'," +
				"'p-interfaces':{'p-interface':[" +
				"{'interface-name':'interface-1','l-interfaces':{'l-interface':[{'interface-name':'l-interface-1','test':'test'}]}}," +
				"{'interface-name':'interface-2'}" +
				"]}}");
		collection.replaceOne(findQuery, obj, new UpdateOptions().upsert(true));


		findQuery = Document.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'}");
		obj = Document.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2','hostname':'testPserver_2'," +
				"'p-interfaces':{'p-interface':[" +
				"{'interface-name':'interface-1','l-interfaces':{'l-interface':[{'interface-name':'l-interface-1'}]}}," +
				"{'interface-name':'interface-2'}" +
				"]}}");
		collection.replaceOne(findQuery, obj, new UpdateOptions().upsert(true));

		findQuery = Document.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_99'}");
		obj = Document.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_99','hostname':'testPserver_99'," +
				"'p-interfaces':{'p-interface':[" +
				"{'interface-name':'interface-1','l-interfaces':{'l-interface':[{'interface-name':'l-interface-1','l3-interface-ipv4-address-list':[{'l3-interface-ipv4-address':'address'}]}]}}," +
				"{'interface-name':'interface-2'}" +
				"]}}");
		collection.replaceOne(findQuery, obj, new UpdateOptions().upsert(true));

		assertEquals("Pre " + collectionName + " test: collection contains 3 documents", 3L, collection.count());

		return collection;
	}

	@Test
	public void getNestedObjectAddressList() throws JSONException {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();
		setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_99'," +
				"'hostname':'testPserver_99'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.l3-interface-ipv4-address-list.l3-interface-ipv4-address':'address'}";
		CacheEntry ce = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.isNested(true)
				.withNestedFind(JsonParser.parseString(nestedFindString).getAsJsonObject()).build();

		Optional<Document> nested = mongoHelperSingleton.getObject(ce);

		assertTrue(nested.isPresent());

		JSONAssert.assertEquals(
				new JSONObject("{'l3-interface-ipv4-address':'address'}"),
				new JSONObject(nested.get().toJson()),
				true);


	}

	@Test
	public void getNestedObject() throws JSONException {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();
		setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_99'," +
				"'hostname':'testPserver_99'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-1'}";
		CacheEntry ce = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.isNested(true)
				.withNestedFind(JsonParser.parseString(nestedFindString).getAsJsonObject()).build();

		Optional<Document> nested = mongoHelperSingleton.getObject(ce);

		assertTrue(nested.isPresent());
		JSONAssert.assertEquals(
				new JSONObject("{'interface-name':'l-interface-1','l3-interface-ipv4-address-list':[{'l3-interface-ipv4-address':'address'}]}"),
				new JSONObject(nested.get().toJson()),
				true);
	}

		@Test
	public void insertNewTop() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.INSERT_REPLACE)
				.isNested(false)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_3")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_3'}").getAsJsonObject())
				.withPayload(JsonParser.parseString("{'hostname':'testPserver_1'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.insertReplace(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 4 document", 4L, collection.count());

	}

	@Test
	public void insertNewNested() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
				"'p-interfaces.p-interface.interface-name':'interface-NEW'}";
		Document nestedFind = Document.parse(nestedFindString);

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.INSERT_REPLACE)
				.isNested(true)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_1")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'}").getAsJsonObject())
				.withPayload(JsonParser.parseString("{'interface-name':'interface-NEW'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface")
				.withNestedFind(JsonParser.parseString(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(JsonParser.parseString("{'interface-name':'interface-NEW'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.insertReplace(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 1 document with new nested", 1L, collection.count(nestedFind));

	}

	@Test
	public void insertNewTwoNested() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-NEW'}";
		Document nestedFind = Document.parse(nestedFindString);

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.INSERT_REPLACE)
				.isNested(true)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_1")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
						"'p-interfaces.p-interface.interface-name':'interface-1'}").getAsJsonObject())
				.withPayload(JsonParser.parseString("{'interface-name':'l-interface-NEW','new-field':'NEW'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface.$.l-interfaces.l-interface")
				.withNestedFind(JsonParser.parseString(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(JsonParser.parseString("{'interface-name':'l-interface-NEW'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.insertReplace(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 1 document with new nested", 1L, collection.count(nestedFind));

	}


	@Test
	public void insertExstingNested() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'}";
		Document nestedFind = Document.parse(nestedFindString);

		String newNestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
				"'p-interfaces.p-interface.new-field':'NEW'}";
		Document newNestedFind = Document.parse(newNestedFindString);

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.INSERT_REPLACE)
				.isNested(true)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_1")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'}").getAsJsonObject())
				.withPayload(JsonParser.parseString("{'interface-name':'interface-1','new-field':'NEW'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface")
				.withNestedFind(JsonParser.parseString(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(JsonParser.parseString("{'interface-name':'interface-1'}").getAsJsonObject())
				.build();

		assertEquals("Pre " + collectionName + " test: collection contains 1 document with new nested", 1L, collection.count(nestedFind));
		assertTrue(mongoHelperSingleton.insertReplace(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 1 document with new nested", 1L, collection.count(nestedFind));
		assertEquals("Post " + collectionName + " test: collection contains 1 document with new nested property", 1L, collection.count(newNestedFind));

	}

	@Test
	public void replaceTop() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.INSERT_REPLACE)
				.isNested(false)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_2")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'}").getAsJsonObject())
				.withPayload(JsonParser.parseString("{'hostname':'testPserver_2','new-property':'NEW'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.insertReplace(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 3 document", 3L, collection.count());
		assertEquals("Post " + collectionName + " test: collection contains 1 document with new property",
				1L,
				collection.count(Document.parse("{'new-property':'NEW'}")));


	}

	@Test
	public void deleteTopLevel() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.DELETE)
				.isNested(false)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_2")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.delete(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 2 document", 2L, collection.count());

	}

	@Test
	public void deleteNestedOneLevel() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'}";
		Document nestedFind = Document.parse(nestedFindString);

		assertEquals("Pre " + collectionName + " test: collection contains 1 document with filter",
				1L,
				collection.count(nestedFind));

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.DELETE)
				.isNested(true)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_2")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface")
				.withNestedFind(JsonParser.parseString(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(JsonParser.parseString("{'interface-name':'interface-1'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.delete(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 0 document",
				0L,
				collection.count(nestedFind));

	}

	@Test
	public void deleteNestedTwoLevel() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-1'}";
		Document nestedFind = Document.parse(nestedFindString);

		assertEquals("Pre " + collectionName + " test: collection contains 1 document with filter",
				1L,
				collection.count(nestedFind));

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.DELETE)
				.isNested(true)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_2")
				.withFindQuery(JsonParser.parseString("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'," +
						"'p-interfaces.p-interface.interface-name':'interface-1'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface.$.l-interfaces.l-interface")
				.withNestedFind(JsonParser.parseString(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(JsonParser.parseString("{'interface-name':'l-interface-1'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.delete(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 0 document",
				0L,
				collection.count(nestedFind));

	}

	//TODO delete non existent top


}