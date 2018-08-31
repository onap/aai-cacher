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

import com.google.gson.JsonParser;
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
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.injestion.parser.InjestionTestComponent;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@ContextConfiguration(classes = {InjestionTestComponent.class, AAIDmaapEventProcessorScenariosTest.class})
public class AAIDmaapEventProcessorScenariosTest {

	private static final String DB_NAME = AAIDmaapEventProcessorScenariosTest.class.getSimpleName();
	private static MongoDatabase mongoDb;
	private static DB db;
	private static MongodProcess mongod;
	private static MongoClient mongoC;

	private JsonParser parser = new JsonParser();

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
	public MongoHelperSingleton mongoHelperSingleton(DB db, MongoDatabase mongoDb) {
		return new MongoHelperSingleton(db, mongoDb);
	}

	@Bean
	public AAIDmaapEventProcessor aaiDmaapEventProcessor(MongoHelperSingleton mongoHelperSingleton, PayloadParserService payloadParserService) {
		return new AAIDmaapEventProcessor(mongoHelperSingleton, payloadParserService);
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
	public void createPserverCreateCRWithNestingAndRelationshipsToTest() throws Exception {
		String pserverCreate = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false}}";

		aaiDmaapEventProcessor.process(pserverCreate);
		assertNotEquals("pserver collection exists", mongoDatabase().getCollection("pserver"), null);
		assertEquals("pserver collection contains 1", mongoDatabase().getCollection("pserver").count(), 1);
		assertTrue("pserver collection contains the pserver in the event",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String crWithNestingAndWithRels = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'cloud-region','top-entity-type':'cloud-region','entity-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'3d567832-df00-49b5-b862-4d3a341dbec1','source-name':'JUNIT','version':'v13','timestamp':'20180515-10:57:55:750'},'entity':{'tenants':{'tenant':[{'vservers':{'vserver':[{'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]},'l-interfaces':{'l-interface':[{'interface-name':'l-int-1','interface-id':'l-int-1','l3-interface-ipv4-address-list':[{'neutron-network-id':'93fb399c-9bfc-4234-b2bb-a76eda38f117','neutron-subnet-id':'79e5bb69-24bb-4ea3-8d1d-c04fca5f5e1e','l3-interface-ipv4-address':'192.168.70.3'}],'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]}}]},'vserver-id':'vserver-1'}]},'tenant-id':'tenenat-1'}]},'cloud-owner':'onap-cloud-owner','cloud-region-id':'mtn6'}}";

		aaiDmaapEventProcessor.process(crWithNestingAndWithRels);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);
		assertTrue("Now pserver has relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertTrue("Now pserver has relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);
	}


	@Test
	public void createCRWithNestingCreatePserverRelationshipsToNestedTest() throws Exception {
		String crWithNesting = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'cloud-region','top-entity-type':'cloud-region','entity-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'3d567832-df00-49b5-b862-4d3a341dbec1','source-name':'JUNIT','version':'v13','timestamp':'20180515-10:57:55:750'},'entity':{'tenants':{'tenant':[{'vservers':{'vserver':[{'l-interfaces':{'l-interface':[{'interface-name':'l-int-1','interface-id':'l-int-1','l3-interface-ipv4-address-list':[{'neutron-network-id':'93fb399c-9bfc-4234-b2bb-a76eda38f117','neutron-subnet-id':'79e5bb69-24bb-4ea3-8d1d-c04fca5f5e1e','l3-interface-ipv4-address':'192.168.70.3'}]}]},'vserver-id':'vserver-1'}]},'tenant-id':'tenenat-1'}]},'cloud-owner':'onap-cloud-owner','cloud-region-id':'mtn6'}}";

		aaiDmaapEventProcessor.process(crWithNesting);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);

		String pserverWithRelsToNested = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false,'relationship-list':{'relationship':[{'related-to':'vserver','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'}]},{'related-to':'l-interface','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'},{'relationship-key':'l-interface.interface-name','relationship-value':'l-int-1'}]}]}}}";

		aaiDmaapEventProcessor.process(pserverWithRelsToNested);
		assertTrue("Now cloud-region->tenant->vserver now has relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'cloud-owner':'onap-cloud-owner'," +
								"'cloud-region-id':'mtn6'," +
								"'tenants.tenant.tenant-id':'tenenat-1'," +
								"'tenants.tenant.vservers.vserver.vserver-id':'vserver-1'," +
								"'tenants.tenant.vservers.vserver.relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertTrue("Now cloud-region->tenant->vserver->l-interface now has relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'cloud-owner':'onap-cloud-owner'," +
								"'cloud-region-id':'mtn6'," +
								"'tenants.tenant.tenant-id':'tenenat-1'," +
								"'tenants.tenant.vservers.vserver.vserver-id':'vserver-1'," +
								"'tenants.tenant.vservers.vserver.l-interfaces.l-interface.interface-name':'l-int-1'," +
								"'tenants.tenant.vservers.vserver.relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
	}

    @Ignore
	@Test
	public void createPserverCreateCRWithNestingAndRelsToUpdateRemovingARelTest() throws Exception {
		String pserverCreate = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false}}";

		aaiDmaapEventProcessor.process(pserverCreate);
		assertNotEquals("pserver collection exists", mongoDatabase().getCollection("pserver"), null);
		assertEquals("pserver collection contains 1", mongoDatabase().getCollection("pserver").count(), 1);
		assertTrue("pserver collection contains the pserver in the event",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String crWithNestingAndWithRels = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'cloud-region','top-entity-type':'cloud-region','entity-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'3d567832-df00-49b5-b862-4d3a341dbec1','source-name':'JUNIT','version':'v13','timestamp':'20180515-10:57:55:750'},'entity':{'tenants':{'tenant':[{'vservers':{'vserver':[{'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]},'l-interfaces':{'l-interface':[{'interface-name':'l-int-1','interface-id':'l-int-1','l3-interface-ipv4-address-list':[{'neutron-network-id':'93fb399c-9bfc-4234-b2bb-a76eda38f117','neutron-subnet-id':'79e5bb69-24bb-4ea3-8d1d-c04fca5f5e1e','l3-interface-ipv4-address':'192.168.70.3'}],'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]}}]},'vserver-id':'vserver-1'}]},'tenant-id':'tenenat-1'}]},'cloud-owner':'onap-cloud-owner','cloud-region-id':'mtn6'}}";

		aaiDmaapEventProcessor.process(crWithNestingAndWithRels);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);
		assertTrue("Now pserver has relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertTrue("Now pserver has relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String updatePserverWithoutInterfaceRel = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'UPDATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false,'relationship-list':{'relationship':[{'related-to':'vserver','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'}]}]}}}";

		aaiDmaapEventProcessor.process(updatePserverWithoutInterfaceRel);

		assertTrue("Now cloud-region->tenant->vserver should still have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'cloud-owner':'onap-cloud-owner'," +
								"'cloud-region-id':'mtn6'," +
								"'tenants.tenant.tenant-id':'tenenat-1'," +
								"'tenants.tenant.vservers.vserver.vserver-id':'vserver-1'," +
								"'tenants.tenant.vservers.vserver.relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("Now cloud-region->tenant->vserver->l-interface should not have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'cloud-owner':'onap-cloud-owner'," +
								"'cloud-region-id':'mtn6'," +
								"'tenants.tenant.tenant-id':'tenenat-1'," +
								"'tenants.tenant.vservers.vserver.vserver-id':'vserver-1'," +
								"'tenants.tenant.vservers.vserver.l-interfaces.l-interface.interface-name':'l-int-1'," +
								"'tenants.tenant.vservers.vserver.l-interfaces.l-interface.relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
	}

	@Test
	public void createPserverCreateCRWithNestingAndRelationshipsToThenDeletePserverTest() throws Exception {
		String pserverCreate = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false}}";

		aaiDmaapEventProcessor.process(pserverCreate);
		assertNotEquals("pserver collection exists", mongoDatabase().getCollection("pserver"), null);
		assertEquals("pserver collection contains 1", mongoDatabase().getCollection("pserver").count(), 1);
		assertTrue("pserver collection contains the pserver in the event",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String crWithNestingAndWithRels = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'cloud-region','top-entity-type':'cloud-region','entity-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'3d567832-df00-49b5-b862-4d3a341dbec1','source-name':'JUNIT','version':'v13','timestamp':'20180515-10:57:55:750'},'entity':{'tenants':{'tenant':[{'vservers':{'vserver':[{'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]},'l-interfaces':{'l-interface':[{'interface-name':'l-int-1','interface-id':'l-int-1','l3-interface-ipv4-address-list':[{'neutron-network-id':'93fb399c-9bfc-4234-b2bb-a76eda38f117','neutron-subnet-id':'79e5bb69-24bb-4ea3-8d1d-c04fca5f5e1e','l3-interface-ipv4-address':'192.168.70.3'}],'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]}}]},'vserver-id':'vserver-1'}]},'tenant-id':'tenenat-1'}]},'cloud-owner':'onap-cloud-owner','cloud-region-id':'mtn6'}}";

		aaiDmaapEventProcessor.process(crWithNestingAndWithRels);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);
		assertTrue("Now pserver has relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertTrue("Now pserver has relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);

		String pserverDelete = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'DELETE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false,'relationship-list':{'relationship':[{'related-to':'vserver','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'}]},{'related-to':'l-interface','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'},{'relationship-key':'l-interface.interface-name','relationship-value':'l-int-1'}]}]}}}";

		aaiDmaapEventProcessor.process(pserverDelete);
		assertNotEquals("pserver collection exists", mongoDatabase().getCollection("pserver"), null);
		assertEquals("pserver collection contains 1", mongoDatabase().getCollection("pserver").count(), 0);

		assertFalse("Now cloud-region->tenant->vserver should not have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'cloud-owner':'onap-cloud-owner'," +
								"'cloud-region-id':'mtn6'," +
								"'tenants.tenant.tenant-id':'tenenat-1'," +
								"'tenants.tenant.vservers.vserver.vserver-id':'vserver-1'," +
								"'tenants.tenant.vservers.vserver.relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("Now cloud-region->tenant->vserver->l-interface should not have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'cloud-owner':'onap-cloud-owner'," +
								"'cloud-region-id':'mtn6'," +
								"'tenants.tenant.tenant-id':'tenenat-1'," +
								"'tenants.tenant.vservers.vserver.vserver-id':'vserver-1'," +
								"'tenants.tenant.vservers.vserver.l-interfaces.l-interface.interface-name':'l-int-1'," +
								"'tenants.tenant.vservers.vserver.l-interfaces.l-interface.relationship-list.relationship.related-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);

	}


	@Test
	public void createPserverRelationshipsToNonExistingTest() throws Exception {

		String pserverWithRelsToNested = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v13','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false,'relationship-list':{'relationship':[{'related-to':'cloud-region','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'}]},{'related-to':'vserver','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'}]},{'related-to':'l-interface','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v13/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'},{'relationship-key':'l-interface.interface-name','relationship-value':'l-int-1'}]}]}}}";

		aaiDmaapEventProcessor.process(pserverWithRelsToNested);

	}

	@Test
	public void linterfaceWithLinterfaceTest() throws Exception {

		String linterfaceWithLinterface = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'l-interface','top-entity-type':'pserver','entity-link':'/aai/v13/cloud-infrastructure/pservers/pserver/c9e8ffb6-a360-4f9c-96c3-f5f0244dfe55-jenkins/lag-interfaces/lag-interface/8806d30d-e5f5-409e-9e9e-9b1c1943058d-jenkins/l-interfaces/l-interface/f4f9b9c7-eb83-4622-9d6f-14027a556ff5-jenkins/l-interfaces/l-interface/89796dd1-89a5-4ddc-bd13-324ba9bce3b6-jenkins','event-type':'AAI-EVENT','domain':'uINT1','action':'DELETE','sequence-number':'0','id':'9060077e-00a3-4239-80ed-855331b4d551','source-name':'FitNesse-Test-jenkins','version':'v13','timestamp':'20180625-01:24:04:857'},'entity':{'pserver-name2':'iqFmGNmNLM6','hostname':'c9e8ffb6-a360-4f9c-96c3-f5f0244dfe55-jenkins','lag-interfaces':{'lag-interface':[{'l-interfaces':{'l-interface':[{'l-interfaces':{'l-interface':[{'v6-wan-link-ip':'PuNFKRUUpd3','interface-name':'89796dd1-89a5-4ddc-bd13-324ba9bce3b6-jenkins','allowed-address-pairs':'RGo6MaADK','prov-status':'uot','macaddr':'xUj8TGre','interface-role':'SyT0hd9Uu4b','selflink':'HxDI','in-maint':false,'admin-status':'GDgD','is-port-mirrored':true,'resource-version':'1529889840462','is-ip-unnumbered':false,'network-name':'RXCo3p3p5BhBS','management-option':'jNiTd','interface-id':'4n8niH','interface-description':'drnTF3'}]},'interface-name':'f4f9b9c7-eb83-4622-9d6f-14027a556ff5-jenkins'}]},'interface-name':'8806d30d-e5f5-409e-9e9e-9b1c1943058d-jenkins'}]}}}";

		aaiDmaapEventProcessor.process(linterfaceWithLinterface);

	}

	@Test
	public void nosTest() throws Exception {

		String nos = getEventPayload("nos");

		aaiDmaapEventProcessor.process(nos);

	}

	@Test
	public void addressListTest() throws Exception {

		String event = getEventPayload("address-list");
		aaiDmaapEventProcessor.process(event);

	}

	@Test
	public void vceTest() throws Exception {

		String event = getEventPayload("vce");
		aaiDmaapEventProcessor.process(event);

	}

	@Test
	public void largePserverTest() throws Exception {

		String event = getEventPayload("large-pserver");
		aaiDmaapEventProcessor.process(event);

	}

	@Test
	public void delRel() throws Exception {

		String event = getEventPayload("deleteRelationship/1-create-logical-link");
		aaiDmaapEventProcessor.process(event);

		event = getEventPayload("deleteRelationship/2-create-generic-vnf");
		aaiDmaapEventProcessor.process(event);

		event = getEventPayload("deleteRelationship/3-create-rel-generic-vnf-vlan-to-logical-link");
		aaiDmaapEventProcessor.process(event);

		event = getEventPayload("deleteRelationship/4-delete-rel-to-generic-vnf-vlan-from-logical-link");
		aaiDmaapEventProcessor.process(event);

		assertFalse("Now generic-vnf->l-interface->vlan should not have relationship to logical-link",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id': '/network/generic-vnfs/generic-vnf/generic-vnf-id'," +
								"'vnf-id': 'generic-vnf-id'," +
								"'l-interfaces.l-interface.interface-name': 'l-interface-name-1'," +
								"'l-interfaces.l-interface.vlans.vlan.vlan-interface': 'vlan-1'," +
								"'l-interfaces.l-interface.vlans.vlan.relationship-list.relationship.related-link':'/aai/v13/network/logical-links/logical-link/logical-link'" +
								"}"))
						.iterator().hasNext());

	}

	protected String getEventPayload(String eventpayloadName) throws IOException {
		return getPayload("test/payloads/dmaapEvents/" + eventpayloadName + ".json");
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