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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
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
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.onap.aai.cacher.egestion.printer.PayloadPrinterService;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserType;
import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.service.helper.CacheHelperService;
import org.onap.aai.cacher.util.AAIConstants;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class LimitTest {

	private static final String DB_NAME = LimitTest.class.getSimpleName();
	private static MongoDatabase mongoDatabase;
	private static DB db;
	private static MongodProcess mongod;
	private static MongoClient mongoC;

	@Autowired private PayloadParserService payloadParserService;
	@Autowired private PayloadPrinterService payloadPrinterService;
	@Autowired private MongoHelperSingleton mongoHelperSingleton;
	private CacheHelperService cacheHelperService;
	private JsonParser parser = new JsonParser();


	@Configuration
	@ComponentScan({"org.onap.aai.cacher.egestion","org.onap.aai.cacher.injestion"})
	public static class SpringConfig {
		@Bean
		public MongoHelperSingleton getMongoHelperSingleton() {
			return new MongoHelperSingleton(db, mongoDatabase);
		}
	}

	@Rule
	public TestName name = new TestName();

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

		IMongodConfig mongoConfigConfig = new MongodConfigBuilder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(port, Network.localhostIsIPv6()))
				.cmdOptions(new MongoCmdOptionsBuilder().enableTextSearch(true).useNoPrealloc(false).build())
				.configServer(false)
				.build();

		ProcessOutput processOutput = new ProcessOutput(Processors.logTo(logger, Slf4jLevel.WARN), Processors.logTo(logger,
				Slf4jLevel.WARN), Processors.logTo(logger, Slf4jLevel.WARN));

		MongodExecutable mongodExecutable = MongodStarter
				.getInstance((new RuntimeConfigBuilder())
						.defaults(Command.MongoD)
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
		cacheHelperService = new CacheHelperService();
		cacheHelperService.setMongoHelper(mongoHelperSingleton);
		cacheHelperService.setPayloadParserService(payloadParserService);
		cacheHelperService.setPayloadPrinterService(payloadPrinterService);

	}

	@After
	public void cleanup() {
		final List<String> collectionNames = new ArrayList<>();
		mongoDatabase.listCollections().iterator().forEachRemaining(document -> collectionNames.add(document.getString("name")));
		collectionNames.forEach(collectionName -> mongoDatabase.getCollection(collectionName).drop());
	}

	@Test
	public void testBsonLimitWhereCacheIsArrayOfSmallObjs() throws JSONException {

		mongoDatabase.createCollection(AAIConstants.COLLECTION_CACHEKEY);
		String genericVnfCacheKey = "{" +
				"'cacheKey': 'generic-vnf'," +
				"'baseUrl': 'https://localhost:8443'," +
				"'module': '/aai/v14/'," +
				"'URI': 'network/generic-vnf?depth=0'" +
				"}";
		JsonObject ckJson = (JsonObject) parser.parse(genericVnfCacheKey);
		CacheKey ck = CacheKey.fromJson(ckJson);
		ck.setParserStrategy(PayloadParserType.AAI_RESOURCE_GET_ALL.getValue());
		cacheHelperService.addCacheKey(ck);
		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("\n\nCache key after insert");
		System.out.println(ck.toString());

		String collectionName = name.getMethodName();
		mongoDatabase.createCollection(collectionName);

		JsonObject genericVnfsObj = new JsonObject();
		JsonArray genericVnfsArray = new JsonArray();
		String genericVnfTemplate = "{" +
					"'vnf-id':'%s'," +
					"'vnf-name':'vn2f0-SDN'," +
					"'vnf-type':'test-gvnf-type'," +
					"'service-id':'a92a77d5a0-123e-4'," +
					"'orchestration-status':'active'," +
					"'in-maint':true," +
					"'is-closed-loop-disabled':false," +
					"'resource-version':'1506978203538'" +
				"}";

		String vnfId;
		JsonObject genericVnf;
		for (int i = 0; i < 70000; i++) {
			vnfId = UUID.randomUUID().toString();
			genericVnf = parser.parse(String.format(genericVnfTemplate, vnfId)).getAsJsonObject();
			genericVnfsArray.add(genericVnf);
		}

		genericVnfsObj.add("generic-vnf", genericVnfsArray);

		cacheHelperService.populateCache(ck, genericVnfsObj.toString());

		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("Updated cache key");
		System.out.println(ck.toString());

		Response response = cacheHelperService.getData(ck);
		assertEquals("Get is Successful", 200, response.getStatus());
		JSONAssert.assertEquals(genericVnfsObj.toString(), response.getEntity().toString(), false);

	}

	@Test
	public void testBsonLimitWhereCacheContainsOneLargeObj() throws JSONException {

		mongoDatabase.createCollection(AAIConstants.COLLECTION_CACHEKEY);
		String genericVnfCacheKey = "{" +
				"'cacheKey': 'generic-vnf'," +
				"'baseUrl': 'https://localhost:8443'," +
				"'module': '/aai/v14/'," +
				"'URI': 'network/generic-vnf?depth=0'" +
				"}";
		JsonObject ckJson = (JsonObject) parser.parse(genericVnfCacheKey);
		CacheKey ck = CacheKey.fromJson(ckJson);
		ck.setParserStrategy(PayloadParserType.AAI_RESOURCE_GET_ALL.getValue());
		cacheHelperService.addCacheKey(ck);
		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("\n\nCache key after insert");
		System.out.println(ck.toString());

		String collectionName = name.getMethodName();
		mongoDatabase.createCollection(collectionName);

		JsonObject genericVnfsObj = new JsonObject();
		JsonArray genericVnfsArray = new JsonArray();
		String genericVnfTemplate = "{" +
				"'vnf-id':'%s'," +
				"'vnf-name':'vn2f0-SDN'," +
				"'vnf-type':'test-gvnf-type'," +
				"'service-id':'a92a77d5a0-123e-4'," +
				"'orchestration-status':'active'," +
				"'in-maint':true," +
				"'is-closed-loop-disabled':false," +
				"'resource-version':'1506978203538'" +
				"}";
		String vnfId;
		JsonObject genericVnf;
		for (int i = 0; i < 20; i++) {
			vnfId = UUID.randomUUID().toString();
			genericVnf = parser.parse(String.format(genericVnfTemplate, vnfId)).getAsJsonObject();
			genericVnfsArray.add(genericVnf);
		}

		JsonObject vfModulesObj = new JsonObject();
		JsonArray vfModulesArray = new JsonArray();
		String vfModuleTemplate = "{" +
					"'vf-module-id':'%s'," +
					"'vf-module-name':'example-vf-module-name'," +
					"'heat-stack-id':'example-heat-stack-id'," +
					"'orchestration-status':'example-orchestration-status'," +
					"'is-base-vf-module':true," +
					"'automated-assignment':true" +
				"}";
		String vfModuleId;
		JsonObject vfModule;
		for (int i = 0; i < 70000; i++) {
			vfModuleId = UUID.randomUUID().toString();
			vfModule = parser.parse(String.format(vfModuleTemplate, vfModuleId)).getAsJsonObject();
			vfModulesArray.add(vfModule);
		}

		vfModulesObj.add("vf-module", vfModulesArray);
		genericVnfsArray.get(0).getAsJsonObject().add("vf-modules", vfModulesObj);
		genericVnfsObj.add("generic-vnf", genericVnfsArray);

		cacheHelperService.populateCache(ck, genericVnfsObj.toString());

		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("Updated cache key");
		System.out.println(ck.toString());

		Response response = cacheHelperService.getData(ck);
		assertEquals("Get is Successful", 200, response.getStatus());
		JSONAssert.assertEquals(genericVnfsObj.toString(), response.getEntity().toString(), false);

	}

	@Test
	public void testBsonLimitWhereCacheContainsOneSmallObj() throws JSONException {

		mongoDatabase.createCollection(AAIConstants.COLLECTION_CACHEKEY);
		String genericVnfCacheKey = "{" +
				"'cacheKey': 'generic-vnf'," +
				"'baseUrl': 'https://localhost:8443'," +
				"'module': '/aai/v14/'," +
				"'URI': 'network/generic-vnf?depth=0'" +
				"}";
		JsonObject ckJson = (JsonObject) parser.parse(genericVnfCacheKey);
		CacheKey ck = CacheKey.fromJson(ckJson);
		ck.setParserStrategy(PayloadParserType.AAI_RESOURCE_GET_ALL.getValue());
		cacheHelperService.addCacheKey(ck);
		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("\n\nCache key after insert");
		System.out.println(ck.toString());

		String collectionName = name.getMethodName();
		mongoDatabase.createCollection(collectionName);

		JsonObject genericVnfsObj = new JsonObject();
		JsonArray genericVnfsArray = new JsonArray();
		String genericVnfTemplate = "{" +
				"'vnf-id':'%s'," +
				"'vnf-name':'vn2f0-SDN'," +
				"'vnf-type':'test-gvnf-type'," +
				"'service-id':'a92a77d5a0-123e-4'," +
				"'orchestration-status':'active'," +
				"'in-maint':true," +
				"'is-closed-loop-disabled':false," +
				"'resource-version':'1506978203538'" +
				"}";
		String vnfId;
		JsonObject genericVnf;
		for (int i = 0; i < 2; i++) {
			vnfId = UUID.randomUUID().toString();
			genericVnf = parser.parse(String.format(genericVnfTemplate, vnfId)).getAsJsonObject();
			genericVnfsArray.add(genericVnf);
		}

		JsonObject vfModulesObj = new JsonObject();
		JsonArray vfModulesArray = new JsonArray();
		String vfModuleTemplate = "{" +
				"'vf-module-id':'%s'," +
				"'vf-module-name':'example-vf-module-name'," +
				"'heat-stack-id':'example-heat-stack-id'," +
				"'orchestration-status':'example-orchestration-status'," +
				"'is-base-vf-module':true," +
				"'automated-assignment':true" +
				"}";
		String vfModuleId;
		JsonObject vfModule;
		for (int i = 0; i < 2; i++) {
			vfModuleId = UUID.randomUUID().toString();
			vfModule = parser.parse(String.format(vfModuleTemplate, vfModuleId)).getAsJsonObject();
			vfModulesArray.add(vfModule);
		}

		JsonArray addrArray = new JsonArray();
		String addrTemplate = "{" +
				"'vip-ipv4-address': '%s'" +
				"}";
		for (int i = 0; i < 2; i++) {
			String addr = UUID.randomUUID().toString();
			JsonObject addrObj = parser.parse(String.format(addrTemplate, addr)).getAsJsonObject();
			addrArray.add(addrObj);
		}
		vfModulesArray.get(0).getAsJsonObject().add("vip-ipv4-address-list", addrArray);
		vfModulesObj.add("vf-module", vfModulesArray);
		genericVnfsArray.get(0).getAsJsonObject().add("vf-modules", vfModulesObj);
		genericVnfsObj.add("generic-vnf", genericVnfsArray);

		System.out.println(genericVnfsObj.toString());
		cacheHelperService.populateCache(ck, genericVnfsObj.toString());

		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("Updated cache key");
		System.out.println(ck.toString());

		Response response = cacheHelperService.getData(ck);
		assertEquals("Get is Successful", 200, response.getStatus());
		JSONAssert.assertEquals(genericVnfsObj.toString(), response.getEntity().toString(), false);

	}

	@Test
	public void testOneCrWithNestedDupeGetAll() throws JSONException, IOException {

		mongoDatabase.createCollection(AAIConstants.COLLECTION_CACHEKEY);
		String crKey = "{" +
				"'cacheKey': 'cloud-region'," +
				"'baseUrl': 'https://localhost:8443'," +
				"'module': '/aai/v14/'," +
				"'URI': '/cloud-infrastructure/cloud-regions'" +
				"}";
		JsonObject ckJson = (JsonObject) parser.parse(crKey);
		CacheKey ck = CacheKey.fromJson(ckJson);
		ck.setParserStrategy(PayloadParserType.AAI_RESOURCE_GET_ALL.getValue());
		cacheHelperService.addCacheKey(ck);
		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("\n\nCache key after insert");
		System.out.println(ck.toString());

		String collectionName = name.getMethodName();
		mongoDatabase.createCollection(collectionName);

		String crs = getJsonPayload("one-cr-with-nested-dupe-get-all");

		cacheHelperService.populateCache(ck, crs);

		assertEquals(5, mongoHelperSingleton.findAllWithIdsStartingWith("cloud-region", "/cloud-infrastructure/cloud-regions/cloud-region/cloud-owner").size());

		ck = cacheHelperService.retrieveCacheKeyObject(ck);
		System.out.println("Updated cache key");
		System.out.println(ck.toString());

		Response response = cacheHelperService.getData(ck);
		assertEquals("Get is Successful", 200, response.getStatus());
		System.out.println("*********\n" + response.getEntity().toString() + "\n********");

		assertThat("Stored does not contain empty array", response.getEntity().toString(), not(containsString("[]")));

	}

	private String getJsonPayload(String payload) throws IOException {
		return getPayload("test/payloads/json/" + payload + ".json");
	}

	private String getPayload(String filename) throws IOException {

		InputStream inputStream = getClass()
				.getClassLoader()
				.getResourceAsStream(filename);

		String message = String.format("Unable to find the %s in src/test/resources", filename);
		assertNotNull(message, inputStream);

		return IOUtils.toString(inputStream);
	}

}