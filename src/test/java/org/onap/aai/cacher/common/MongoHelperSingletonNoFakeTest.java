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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.runtime.Network;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;
import org.onap.aai.cacher.dmaap.consumer.AAIDmaapEventProcessorScenariosTest;
import org.onap.aai.cacher.model.CacheEntry;
import org.onap.aai.cacher.model.DBAction;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class MongoHelperSingletonNoFakeTest {

	private static final String DB_NAME = AAIDmaapEventProcessorScenariosTest.class.getSimpleName();
	private static MongoDatabase mongoDatabase;
	private static DB db;
	private static MongodProcess mongod;
	private static MongoClient mongoC;

	private MongoHelperSingleton mongoHelperSingleton;
	private JsonParser parser = new JsonParser();


	@BeforeClass
	public static void setup() throws IOException, InterruptedException {

		String bindIp = "localhost";
		int port = 27017;
		startEmbedded(port);

		mongoC = new MongoClient(bindIp, port);
		mongoDatabase = mongoC.getDatabase(DB_NAME);
		db = mongoC.getDB(DB_NAME);

	}

	protected static void startEmbedded(int port) throws IOException {
		Logger logger = LoggerFactory.getLogger("mongo");

		MongodConfig mongoConfigConfig = MongodConfig.builder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(port, Network.localhostIsIPv6()))
				.cmdOptions(MongoCmdOptions.builder().enableTextSearch(true).useNoPrealloc(false).build())
				.isConfigServer(false)
				.build();

		ProcessOutput processOutput = new ProcessOutput(Processors.logTo(logger, Slf4jLevel.WARN), Processors.logTo(logger,
				Slf4jLevel.WARN), Processors.logTo(logger, Slf4jLevel.WARN));

		MongodExecutable mongodExecutable = MongodStarter
				.getInstance(Defaults.runtimeConfigFor(Command.MongoD)
						.processOutput(processOutput)
						.build())
				.prepare(mongoConfigConfig);

		mongod = mongodExecutable.start();
	}

	@AfterClass
	public static void tearDown() {
		if (mongod != null && mongod.isProcessRunning()) {
			mongod.stop();
		}
	}

	@Before
	public void init() {
		mongoHelperSingleton = new MongoHelperSingleton(db, mongoDatabase);
	}

	@After
	public void cleanup() {
		final List<String> collectionNames = new ArrayList<>();
		mongoDatabase.listCollections().iterator().forEachRemaining(document -> collectionNames.add(document.getString("name")));
		collectionNames.forEach(collectionName -> mongoDatabase.getCollection(collectionName).drop());
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

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_99'," +
				"'hostname':'testPserver_99'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.l3-interface-ipv4-address-list.l3-interface-ipv4-address':'address'}";
		CacheEntry ce = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.isNested(true)
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject()).build();

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

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_99'," +
				"'hostname':'testPserver_99'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-1'}";
		CacheEntry ce = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.isNested(true)
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject()).build();

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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_3'}").getAsJsonObject())
				.withPayload(parser.parse("{'hostname':'testPserver_1'}").getAsJsonObject())
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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'}").getAsJsonObject())
				.withPayload(parser.parse("{'interface-name':'interface-NEW'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface")
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(parser.parse("{'interface-name':'interface-NEW'}").getAsJsonObject())
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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
						"'p-interfaces.p-interface.interface-name':'interface-1'}").getAsJsonObject())
				.withPayload(parser.parse("{'interface-name':'l-interface-NEW','new-field':'NEW'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface.$.l-interfaces.l-interface")
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(parser.parse("{'interface-name':'l-interface-NEW'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.insertReplace(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 1 document with new nested", 1L, collection.count(nestedFind));
		
	}

	@Test
	public void insertNewThreeNested() throws Exception {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		MongoCollection<Document> collection = setupCollection(collectionName);

		String nestedFindString = "{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.vlans.vlan.vlan-interface':'vlan-NEW'}";
		Document nestedFind = Document.parse(nestedFindString);

		CacheEntry cacheEntry = CacheEntry.CacheEntryBuilder.createCacheEntry()
				.inCollection(collectionName)
				.withDbAction(DBAction.INSERT_REPLACE)
				.isNested(true)
				.withId("/cloud-infrastructure/pservers/pserver/testPserver_1")
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'," +
						"'p-interfaces.p-interface.interface-name':'interface-1'," +
						"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'l-interface-1'}").getAsJsonObject())
				.withPayload(parser.parse("{'vlan-interface':'vlan-NEW','new-field':'NEW4'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface.$.l-interfaces.l-interface.$.vlans.vlan")
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(parser.parse("{'vlan-interface':'vlan-NEW'}").getAsJsonObject())
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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_1'}").getAsJsonObject())
				.withPayload(parser.parse("{'interface-name':'interface-1','new-field':'NEW'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface")
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(parser.parse("{'interface-name':'interface-1'}").getAsJsonObject())
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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'}").getAsJsonObject())
				.withPayload(parser.parse("{'hostname':'testPserver_2','new-property':'NEW'}").getAsJsonObject())
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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'}").getAsJsonObject())
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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface")
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(parser.parse("{'interface-name':'interface-1'}").getAsJsonObject())
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
				.withFindQuery(parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/testPserver_2'," +
						"'p-interfaces.p-interface.interface-name':'interface-1'}").getAsJsonObject())
				.withNestedField("p-interfaces.p-interface.$.l-interfaces.l-interface")
				.withNestedFind(parser.parse(nestedFindString).getAsJsonObject())
				.withNestedFieldIdentifierObj(parser.parse("{'interface-name':'l-interface-1'}").getAsJsonObject())
				.build();

		assertTrue(mongoHelperSingleton.delete(cacheEntry));
		assertEquals("Post " + collectionName + " test: collection contains 0 document",
				0L,
				collection.count(nestedFind));

		
	}

	//TODO delete non existent top


}