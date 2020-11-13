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

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;

public class CrudOperationsTest {

	public static final String DB_NAME = "testDb";
	private static MongoDatabase mongoDatabase;

	@BeforeClass
	public static void setup() {
		MongoServer mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress serverAddress = mongoServer.bind();

		MongoClient client = new MongoClient(new ServerAddress(serverAddress));
		mongoDatabase = client.getDatabase(DB_NAME);
	}

	@Test
	public void insertOrUpdateObjInCollection() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery1 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery1, newObj, new UpdateOptions().upsert(true));

		Document findQuery2 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\"}");
		newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\",\"hostname\":\"testPserver_2\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery2, newObj, new UpdateOptions().upsert(true));

		newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\",\"hostname\":\"testPserver_2\",\"equip-type\":\"NEW\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery2, newObj, new UpdateOptions().upsert(true));

		FindIterable<Document> docs = collection.find();
		int counter = 0;
		for (Document doc : docs) {
			counter++;
		}
		assertEquals("collection contains 2 document", 2, counter);

		Document doc = collection.find(findQuery1).first();
		assertEquals("Found testPserver_1", "testPserver_1", doc.getString("hostname"));

		doc = collection.find(findQuery2).first();
		assertEquals("Found testPserver_2", "testPserver_2", doc.getString("hostname"));
		assertEquals("testPserver_2 has NEW as equip-type", "NEW", doc.getString("equip-type"));
	}

	@Test
	public void removeObjectFromCollection() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery1 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery1, newObj, new UpdateOptions().upsert(true));

		Document findQuery2 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\"}");
		newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\",\"hostname\":\"testPserver_2\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery2, newObj, new UpdateOptions().upsert(true));

		assertEquals("PRE DELETE: collection contains 2 documents", 2, collection.count());

		collection.deleteOne(findQuery1);

		assertEquals("POST DELETE: collection contains 1 documents", 1, collection.count());


	}

	@Test
	public void findOneFromCollection() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery1 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery1, newObj, new UpdateOptions().upsert(true));

		Document findQuery2 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\"}");
		newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\",\"hostname\":\"testPserver_2\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery2, newObj, new UpdateOptions().upsert(true));

		Document findQuery = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_2\"}");

		assertEquals("collection contains 1 document with id /cloud-infrastructure/pservers/pserver/testPserver_2",
				1L, collection.count(findQuery));
	}

	@Test
	public void findFromCollectionWithNested() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery1 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery1, newObj, new UpdateOptions().upsert(true));

		Document findQuery = Document.parse(
				"{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.interface-name\":\"interface-1\"}"
		);

		assertEquals("collection contains 1 document with id /cloud-infrastructure/pservers/pserver/testPserver_2",
				1L, collection.count(findQuery));


	}

	@Test
	public void findFromCollectionWithNestedFail() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery1 = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery1, newObj, new UpdateOptions().upsert(true));

		Document findQuery = Document.parse(
				"{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
						"\"p-interfaces.p-interface.interface-name\":\"interface-NONE\"}"
		);

		assertEquals("collection contains 1 document with id /cloud-infrastructure/pservers/pserver/testPserver_2",
				0L, collection.count(findQuery));

	}


	@Test
	public void addToNestedListWithNonExistingList() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\"}");
		collection.replaceOne(findQuery, newObj, new UpdateOptions().upsert(true));

		//Check for existing obj
		String field = "p-interfaces.p-interface";
		String obj = "{\"interface-name\":\"interface-NEW\"}";
		String nestedFindString = "{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.interface-name\":\"interface-NEW\"}";

		Document nestedFind = Document.parse(nestedFindString);

		assertEquals("PRE UPDATE: collection contains 0 document with id and nested field",
				Long.valueOf(0L),
				Long.valueOf(collection.count(nestedFind)));

		Document doc = new Document();
		doc.put(field, Document.parse(obj));
		Document push = new Document();
		push.put("$push", doc);

		System.out.println(collection.find(findQuery).first().toJson());

		collection.findOneAndUpdate(findQuery, push, new FindOneAndUpdateOptions().upsert(true));

		System.out.println(collection.find(findQuery).first().toJson());

		assertEquals("POST UPDATE: collection contains 1 document with id and nested field",
				Long.valueOf(1L),
				Long.valueOf(collection.count(nestedFind)));

	}

	@Test
	public void addToNestedExistingList() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery, newObj, new UpdateOptions().upsert(true));

		//Check for existing obj
		String field = "p-interfaces.p-interface";
		String obj = "{\"interface-name\":\"interface-NEW\"}";
		String nestedFindString = "{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.interface-name\":\"interface-NEW\"}";

		Document nestedFind = Document.parse(nestedFindString);

		assertEquals("PRE UPDATE: collection contains 0 document with id and nested field",
				Long.valueOf(0L),
				Long.valueOf(collection.count(nestedFind)));

		Document doc = new Document();
		doc.put(field, Document.parse(obj));
		Document push = new Document();
		push.put("$push", doc);

		System.out.println(collection.find(findQuery).first().toJson());

		collection.findOneAndUpdate(findQuery, push, new FindOneAndUpdateOptions().upsert(true));

		System.out.println(collection.find(findQuery).first().toJson());

		assertEquals("POST UPDATE: collection contains 1 document with id and nested field",
				Long.valueOf(1L),
				Long.valueOf(collection.count(nestedFind)));

	}

	@Test
	public void addToNestedWhereNestedObjExists() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document findQuery = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"}");
		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\",\"equip-type\":\"JUNIPER XXXX\",\"p-interfaces\":{\"p-interface\":[{\"interface-name\":\"interface-1\"}]}}");
		collection.replaceOne(findQuery, newObj, new UpdateOptions().upsert(true));

		//Check for existing obj
		String field = "p-interfaces.p-interface";
		String obj = "{\"interface-name\":\"interface-NEW\",\"new-field\":\"NEW\"}";
		String nestedFindString = "{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.interface-name\":\"interface-1\"}";
		String nestedFindUsingNewFieldString = "{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.new-field\":\"NEW\"}";
		String nestedFieldPull = "{\"p-interfaces.p-interface\":{\"interface-name\":\"interface-1\"}}";

		Document nestedFind = Document.parse(nestedFindString);

		assertEquals("PRE UPDATE: collection contains 1 document with id and nested field",
				Long.valueOf(1L),
				Long.valueOf(collection.count(nestedFind)));
		assertEquals("PRE UPDATE: collection contains 0 document with id and new nested field",
				Long.valueOf(0L),
				Long.valueOf(collection.count(Document.parse(nestedFindUsingNewFieldString))));

		//REMOVE existing
		Document pull = new Document();
		pull.put("$pull", Document.parse(nestedFieldPull));

		collection.findOneAndUpdate(nestedFind, pull);

		assertEquals("PRE UPDATE POST DELETE: collection no longer the nested obj that will be inserted",
				Long.valueOf(0L),
				Long.valueOf(collection.count(nestedFind)));


		Document doc = new Document();
		doc.put(field, Document.parse(obj));
		Document push = new Document();
		push.put("$push", doc);

		System.out.println(collection.find(findQuery).first().toJson());

		collection.findOneAndUpdate(findQuery, push, new FindOneAndUpdateOptions().upsert(true));

		System.out.println(collection.find(findQuery).first().toJson());

		assertEquals("POST UPDATE: collection contains 1 document with id and nested field",
				Long.valueOf(1L),
				Long.valueOf(collection.count(Document.parse(nestedFindUsingNewFieldString))));

	}

	@Test
	public void addToNestedWhereNestedObjExistsTwoLevels() {
		String collectionName = new Object() {}
				.getClass()
				.getEnclosingMethod()
				.getName();

		mongoDatabase.createCollection(collectionName);

		MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);

		Document newObj = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\",\"hostname\":\"testPserver_1\"," +
				"\"p-interfaces\":{\"p-interface\":[" +
				"{\"interface-name\":\"interface-1\",\"l-interfaces\":{\"l-interface\":[{\"interface-name\":\"l-interface-1\"}]}}," +
				"{\"interface-name\":\"interface-2\"}" +
				"]}}");

		Document findQuery = Document.parse("{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.interface-name\":\"interface-1\"}");

		collection.replaceOne(findQuery, newObj, new UpdateOptions().upsert(true));

		//Check for existing obj
		String field = "p-interfaces.p-interface.$.l-interfaces.l-interface";
		String obj = "{\"interface-name\":\"l-interface-1\",\"new-field\":\"NEW\"}";

		String nestedFindString = "{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.interface-name\":\"interface-1\"," +
				"\"p-interfaces.p-interface.l-interfaces.l-interface.interface-name\":\"l-interface-1\"}";

		String nestedFindUsingNewFieldString = "{\"_id\":\"/cloud-infrastructure/pservers/pserver/testPserver_1\"," +
				"\"p-interfaces.p-interface.interface-name\":\"interface-1\"" +
				"\"p-interfaces.p-interface.l-interfaces.l-interface.new-field\":\"NEW\"}";

		String nestedFieldPull = "{\"p-interfaces.p-interface.$.l-interfaces.l-interface\":{\"interface-name\":\"l-interface-1\"}}";

		Document nestedFind = Document.parse(nestedFindString);

		assertEquals("PRE UPDATE: collection contains 1 document with id and nested field",
				Long.valueOf(1L),
				Long.valueOf(collection.count(nestedFind)));
		assertEquals("PRE UPDATE: collection contains 0 document with id and new nested field",
				Long.valueOf(0L),
				Long.valueOf(collection.count(Document.parse(nestedFindUsingNewFieldString))));

		//REMOVE existing
		Document pull = new Document();
		pull.put("$pull", Document.parse(nestedFieldPull));

		collection.findOneAndUpdate(nestedFind, pull);

		assertEquals("PRE UPDATE POST DELETE: collection no longer the nested obj that will be inserted",
				Long.valueOf(0L),
				Long.valueOf(collection.count(nestedFind)));


		Document doc = new Document();
		doc.put(field, Document.parse(obj));
		Document push = new Document();
		push.put("$push", doc);

		System.out.println(collection.find(findQuery).first().toJson());

		collection.findOneAndUpdate(findQuery, push, new FindOneAndUpdateOptions().upsert(true));

		System.out.println(collection.find(findQuery).first().toJson());

		assertEquals("POST UPDATE: collection contains 1 document with id and nested field",
				Long.valueOf(1L),
				Long.valueOf(collection.count(Document.parse(nestedFindUsingNewFieldString))));

	}

}