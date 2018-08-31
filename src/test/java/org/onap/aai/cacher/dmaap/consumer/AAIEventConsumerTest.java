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
package org.onap.aai.cacher.dmaap.consumer;

import com.att.nsa.mr.client.MRConsumer;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.egestion.printer.EgestionTestComponent;
import org.onap.aai.cacher.injestion.parser.InjestionTestComponent;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.service.helper.RestClientHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

//@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ContextConfiguration(classes = {InjestionTestComponent.class, AAIEventConsumerTest.class})
public class AAIEventConsumerTest  {
	private static final String DB_NAME = AAIEventConsumerTest.class.getSimpleName();
	private static MongoDatabase mongoDb;
	private static RestClientHelperService restClientHelperService;
	private static DB db;
	private static MongodProcess mongod;
	private static MongoClient mongoC;

	private AAIEventConsumer aaiEventConsumer;
	
	@Autowired
	private AAIDmaapEventProcessor aaiDmaapEventProcessor;

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
	public AAIDmaapEventProcessor aaiDmaapEventProcessor(MongoHelperSingleton mongoHelperSingleton, PayloadParserService payloadParserService) {
		return new AAIDmaapEventProcessor(mongoHelperSingleton, payloadParserService);
	}


	MRConsumer client;
	private String validEventMessage = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false}}";
	private String validHeldEventMessage = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false}}";
	DmaapConsumerSingleton singleton = DmaapConsumerSingleton.getInstance();
	List<String> eventMessageList = new ArrayList<>();

   
	@BeforeClass
    public static void setUp() throws Exception {
		String bindIp = "localhost";
		int port = 27017;
		startEmbedded(port);

		mongoC = new MongoClient(bindIp, port);
		mongoDb = mongoC.getDatabase(DB_NAME);
		db = mongoC.getDB(DB_NAME);
    	
    }
	
	@Before
    public void init() throws Exception {
		eventMessageList.add(validEventMessage);
    	//super.setupBundleconfig();
    	aaiEventConsumer = new AAIEventConsumer("aaiDmaaPEventConsumer.properties", true);
    	Properties prop = aaiEventConsumer.getDmaapEventConsumerProperties();
    	client = Mockito.mock(MRConsumer.class);
    	aaiEventConsumer.setConsumer(client);

    	
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
    public void startProcessing() throws IOException, Exception {
    	Mockito.when(client.fetch()).thenReturn(eventMessageList);
    	aaiEventConsumer.startProcessing(aaiDmaapEventProcessor);
    }
    
    @Test
    public void startProcessingWaitWithHeldEventMessage() throws IOException, Exception {
    	singleton.setIsInitialized(true);
    	singleton.setFirstEventMessage(validHeldEventMessage);
    	Mockito.when(client.fetch()).thenReturn(eventMessageList);
    	aaiEventConsumer.startProcessing(aaiDmaapEventProcessor);
    }
    
    @Test
    public void startProcessingNoWaitWithHeldEventMessage() throws IOException, Exception {
    	singleton.setProcessEvents(true);
    	singleton.setFirstEventMessage(validHeldEventMessage);
    	Mockito.when(client.fetch()).thenReturn(eventMessageList);
    	aaiEventConsumer.startProcessing(aaiDmaapEventProcessor);
    }
    
}

