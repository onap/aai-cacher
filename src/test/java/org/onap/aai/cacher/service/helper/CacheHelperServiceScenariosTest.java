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

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.egestion.printer.EgestionTestComponent;
import org.onap.aai.cacher.injestion.parser.InjestionTestComponent;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserType;
import org.onap.aai.cacher.model.CacheKey;
import org.onap.aai.cacher.util.AAIConstants;
import org.onap.aai.exceptions.AAIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ContextConfiguration(classes = {InjestionTestComponent.class, EgestionTestComponent.class, CacheHelperServiceScenariosTest.class})
public class CacheHelperServiceScenariosTest {
	
	private String aaiGetAllComplexResponse =
			"{" +
			"	'complex': [" +
			"		{" +
			"			'physical-location-id': 'physical-location-id-1'," +
			"			'resource-version': '1'" +
			"		}," +
			"		{" +
			"			'physical-location-id': 'physical-location-id-2'," +
			"			'resource-version': '2'" +
			"		}" +
			"	]" +
			"}";
	
	private String idForDeleteCache = "/cloud-infrastructure/complexes/complex/physical-location-id-2";
	

	private static final String DB_NAME = CacheHelperServiceScenariosTest.class.getSimpleName();
	private static MongoDatabase mongoDb;
	private static RestClientHelperService restClientHelperService;
	private static DB db;
	private static MongodProcess mongod;
	private static MongoClient mongoC;


	@Autowired
	private CacheHelperService cacheHelperService;

	@Bean
	public DB db() {
		return db;
	}

	@Bean
	public MongoDatabase mongoDatabase() {
		return mongoDb;
	}

	@Bean
	public RestClientHelperService restClientHelperService() {
		return restClientHelperService;
	}

	@Bean
	public MongoHelperSingleton mongoHelperSingleton(DB db, MongoDatabase mongoDb) {
		return new MongoHelperSingleton(db, mongoDb);
	}

	@Bean
	public CacheHelperService cacheHelperService() {
		return new CacheHelperService();
	}

	@BeforeClass
	public static void setup() throws IOException, InterruptedException {

		String bindIp = "localhost";
		int port = 27017;
		startEmbedded(port);

		mongoC = new MongoClient(bindIp, port);
		mongoDb = mongoC.getDatabase(DB_NAME);
		db = mongoC.getDB(DB_NAME);

	}

	protected static void startEmbedded(int port) throws IOException {
		IMongodConfig mongoConfigConfig = new MongodConfigBuilder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(port, Network.localhostIsIPv6()))
				.cmdOptions(new MongoCmdOptionsBuilder().verbose(true).build())
				.configServer(false)
				.build();

		MongodExecutable mongodExecutable = MongodStarter.getDefaultInstance().prepare(mongoConfigConfig);

		mongod = mongodExecutable.start();
	}

	@AfterClass
	public static void tearDown() {
		if (mongod != null && mongod.isProcessRunning()) {
			mongod.stop();
		}
	}

	@After
	public void cleanup() {
		final List<String> collectionNames = new ArrayList<>();
		mongoDb.listCollections().iterator().forEachRemaining(document -> collectionNames.add(document.getString("name")));
		collectionNames.forEach(collectionName -> mongoDb.getCollection(collectionName).drop());
	}


	@Test
	public void cacheKeyProcessingTest() throws Exception {
		
		CacheKey ck = new CacheKey("complex");
		cacheHelperService.addCacheKey(ck);
		Response resp = cacheHelperService.getAllKeys();
		assertEquals("getAllKeys", 200, resp.getStatus());
		CacheKey ck1 = new CacheKey("pserver");
		ck1.timingIndicator = "scheduled";
		cacheHelperService.addCacheKey(ck1);
		resp = cacheHelperService.getAllKeys();
		assertEquals("getAllKeys", 200, resp.getStatus());
		CacheKey retrieveCk = cacheHelperService.retrieveCacheKeyObject(ck);
		assertEquals("retrieved cacheKey complex", "complex", retrieveCk.getCacheKey());

		retrieveCk.setParserStrategy(PayloadParserType.AAI_RESOURCE_GET_ALL.toString());
		cacheHelperService.updateCacheKey(retrieveCk);
		assertEquals("retrieved cacheKey complex", PayloadParserType.AAI_RESOURCE_GET_ALL.toString(), retrieveCk.getParserStrategy());
		assertEquals("getScheduledCaches", 1, cacheHelperService.getScheduledCaches().size());
		
		resp = cacheHelperService.populateCache(retrieveCk, aaiGetAllComplexResponse);
		assertEquals("populateCache", 201, resp.getStatus());
		resp = cacheHelperService.getData(retrieveCk);
		assertEquals("getData", 200, resp.getStatus());
		resp = cacheHelperService.retrieveCollectionByKey(retrieveCk, AAIConstants.COLLECTION_CACHEKEY);
		assertEquals("retrieveCollectionByKey", 200, resp.getStatus());
		resp = cacheHelperService.deleteCache(idForDeleteCache, "complex");
		assertEquals("deleteCache1", 204, resp.getStatus());
		resp = cacheHelperService.deleteCache("noSuchId", "complex");
		assertEquals("deleteCache2", 404, resp.getStatus());

		
		assertTrue("isShouldTrigger1", cacheHelperService.isShouldTrigger(retrieveCk));
		long current = System.currentTimeMillis();
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ");
        String syncStartTime = formatter.format(current - 61000);
        String syncEndTime = formatter.format(current - 60500);
        // setup sync in progress
        retrieveCk.syncInterval = "1";
        retrieveCk.lastSyncStartTime = syncEndTime;
        retrieveCk.lastSyncEndTime = syncStartTime;
        cacheHelperService.updateCacheKey(retrieveCk);

        resp = cacheHelperService.forceSync(retrieveCk);
        assertEquals("forceSync", 500, resp.getStatus());
        retrieveCk.lastSyncStartTime = syncStartTime;
        retrieveCk.lastSyncEndTime = syncEndTime;
        assertTrue("isShouldTrigger2", cacheHelperService.isShouldTrigger(retrieveCk));
        
        resp = cacheHelperService.deleteCacheKeyAndAssociatedCache("complex");
        assertEquals("deleteCacheKeyAndAssociatedCache", 204, resp.getStatus());
        List<CacheKey> ckList = new ArrayList<CacheKey>();
        ck = new CacheKey("pnf");
        ck1 = new CacheKey("logical-link");
        ckList.add(ck);
        ckList.add(ck1);
        assertTrue(cacheHelperService.bulkAddCacheKeys(ckList));
        
	}
	
	@Test
	public void buildResponseTest() throws Exception {
		List<String> issueList = Arrays.asList("First Issue", "Second Issue");
		Response resp = cacheHelperService.buildValidationResponse(issueList);
		assertEquals("buildValidationResponse", 400, resp.getStatus());
		AAIException ex = new AAIException("AAI_4000");
		resp = cacheHelperService.buildExceptionResponse(ex);
		assertEquals("buildExceptionResponse", 500, resp.getStatus());
		
	}
}
