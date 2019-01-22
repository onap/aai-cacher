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

import com.github.fakemongo.Fongo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.MongodProcess;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.egestion.printer.PayloadPrinterService;
import org.onap.aai.cacher.egestion.printer.strategy.PayloadPrinterType;
import org.onap.aai.cacher.injestion.parser.InjestionTestComponent;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
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
	@Autowired PayloadPrinterService payloadPrinterService;
	@Autowired PayloadParserService payloadParserService;
	@Autowired MongoHelperSingleton mongoHelperSingleton;

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
	public AAIDmaapEventProcessor aaiDmaapEventProcessor(MongoHelperSingleton mongoHelperSingleton, PayloadParserService payloadParserService, PayloadPrinterService payloadPrinterService) {
		return new AAIDmaapEventProcessor(mongoHelperSingleton, payloadParserService, payloadPrinterService);
	}

	@BeforeClass
	public static void setup() throws IOException, InterruptedException {
		Fongo fongo = new Fongo(DB_NAME);
		mongoDb = fongo.getDatabase(DB_NAME);
		db = fongo.getDB(DB_NAME);
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
		String pserverCreate = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v14','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false}}";

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
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String crWithNestingAndWithRels = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'cloud-region','top-entity-type':'cloud-region','entity-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'3d567832-df00-49b5-b862-4d3a341dbec1','source-name':'JUNIT','version':'v14','timestamp':'20180515-10:57:55:750'},'entity':{'tenants':{'tenant':[{'vservers':{'vserver':[{'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]},'l-interfaces':{'l-interface':[{'interface-name':'l-int-1','interface-id':'l-int-1','l3-interface-ipv4-address-list':[{'neutron-network-id':'93fb399c-9bfc-4234-b2bb-a76eda38f117','neutron-subnet-id':'79e5bb69-24bb-4ea3-8d1d-c04fca5f5e1e','l3-interface-ipv4-address':'192.168.70.3'}],'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]}}]},'vserver-id':'vserver-1'}]},'tenant-id':'tenenat-1'}]},'cloud-owner':'onap-cloud-owner','cloud-region-id':'mtn6'}}";

		aaiDmaapEventProcessor.process(crWithNestingAndWithRels);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);
		assertTrue("Now pserver has relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertTrue("Now pserver has relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);
	}


	@Test
	public void createCRWithNestingCreatePserverRelationshipsToNestedTest() throws Exception {
		String crWithNesting = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'cloud-region','top-entity-type':'cloud-region','entity-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'3d567832-df00-49b5-b862-4d3a341dbec1','source-name':'JUNIT','version':'v14','timestamp':'20180515-10:57:55:750'},'entity':{'tenants':{'tenant':[{'vservers':{'vserver':[{'l-interfaces':{'l-interface':[{'interface-name':'l-int-1','interface-id':'l-int-1','l3-interface-ipv4-address-list':[{'neutron-network-id':'93fb399c-9bfc-4234-b2bb-a76eda38f117','neutron-subnet-id':'79e5bb69-24bb-4ea3-8d1d-c04fca5f5e1e','l3-interface-ipv4-address':'192.168.70.3'}]}]},'vserver-id':'vserver-1'}]},'tenant-id':'tenenat-1'}]},'cloud-owner':'onap-cloud-owner','cloud-region-id':'mtn6'}}";

		aaiDmaapEventProcessor.process(crWithNesting);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);

		String pserverWithRelsToNested = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v14','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false,'relationship-list':{'relationship':[{'related-to':'vserver','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'}]},{'related-to':'l-interface','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'},{'relationship-key':'l-interface.interface-name','relationship-value':'l-int-1'}]}]}}}";

		aaiDmaapEventProcessor.process(pserverWithRelsToNested);
		assertTrue("Now cloud-region->tenant->vserver now has relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
							"'_id':'/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'," +
							"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1'" +
							"}"))
						.iterator().hasNext()
		);
		assertTrue("Now cloud-region->tenant->vserver->l-interface now has relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'_id':'/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
	}


	@Test
	public void createPserverCreateCRWithNestingAndRelsToUpdateRemovingARelTest() throws Exception {

		JsonObject payloads = parser.parse(getEventPayload("createPserverCreateCRWithNestingAndRelsToUpdateRemovingARelTest")).getAsJsonObject();
		String pserverCreate = payloads.get("pserverCreate").toString();

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
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String crWithNestingAndWithRels = payloads.get("crWithNestingAndWithRels").toString();

		aaiDmaapEventProcessor.process(crWithNestingAndWithRels);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);
		assertTrue("Now pserver has relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertTrue("Now pserver has relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String updatePserverWithoutInterfaceRel = payloads.get("updatePserverWithoutInterfaceRel").toString();

		aaiDmaapEventProcessor.process(updatePserverWithoutInterfaceRel);

		assertTrue("Now cloud-region->tenant->vserver should still have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'_id':'/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("Now cloud-region->tenant->vserver->l-interface should not have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'_id':'/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
	}

	@Test
	public void createPserverCreateCRWithNestingAndRelationshipsToThenDeletePserverTest() throws Exception {
		String pserverCreate = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v14','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false}}";

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
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("pserver should not have relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);


		String crWithNestingAndWithRels = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'cloud-region','top-entity-type':'cloud-region','entity-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6','event-type':'AAI-EVENT','domain':'JUNIT','action':'CREATE','sequence-number':'0','id':'3d567832-df00-49b5-b862-4d3a341dbec1','source-name':'JUNIT','version':'v14','timestamp':'20180515-10:57:55:750'},'entity':{'tenants':{'tenant':[{'vservers':{'vserver':[{'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]},'l-interfaces':{'l-interface':[{'interface-name':'l-int-1','interface-id':'l-int-1','l3-interface-ipv4-address-list':[{'neutron-network-id':'93fb399c-9bfc-4234-b2bb-a76eda38f117','neutron-subnet-id':'79e5bb69-24bb-4ea3-8d1d-c04fca5f5e1e','l3-interface-ipv4-address':'192.168.70.3'}],'relationship-list':{'relationship':[{'related-to':'pserver','relationship-data':[{'relationship-value':'pserver-1','relationship-key':'pserver.hostname'}],'related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','relationship-label':'tosca.relationships.HostedOn'}]}}]},'vserver-id':'vserver-1'}]},'tenant-id':'tenenat-1'}]},'cloud-owner':'onap-cloud-owner','cloud-region-id':'mtn6'}}";

		aaiDmaapEventProcessor.process(crWithNestingAndWithRels);

		assertNotEquals("cloud-region collection exists", mongoDatabase().getCollection("cloud-region"), null);
		assertTrue("Now pserver has relationship to vserver",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertTrue("Now pserver has relationship to l-interface",
				mongoDatabase().getCollection("pserver")
						.find(Document.parse("{" +
								"'hostname':'pserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'" +
								"}"))
						.iterator().hasNext()
		);

		String pserverDelete = "{'cambria.partition':'AAI','event-header':{'severity':'NORMAL','entity-type':'pserver','top-entity-type':'pserver','entity-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1','event-type':'AAI-EVENT','domain':'JUNIT','action':'DELETE','sequence-number':'0','id':'0c3b336d-6554-4ddf-a4d7-90f97876a966','source-name':'JUNIT','version':'v14','timestamp':'20180209-21:02:20:344'},'entity':{'hostname':'pserver-1','in-maint':false,'relationship-list':{'relationship':[{'related-to':'vserver','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'}]},{'related-to':'l-interface','relationship-label':'tosca.relationships.HostedOn','related-link':'/aai/v14/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1','relationship-data':[{'relationship-key':'cloud-region.cloud-owner','relationship-value':'onap-cloud-owner'},{'relationship-key':'cloud-region.cloud-region-id','relationship-value':'mtn6'},{'relationship-key':'tenant.tenant-id','relationship-value':'tenenat-1'},{'relationship-key':'vserver.vserver-id','relationship-value':'vserver-1'},{'relationship-key':'l-interface.interface-name','relationship-value':'l-int-1'}]}]}}}";

		aaiDmaapEventProcessor.process(pserverDelete);
		assertNotEquals("pserver collection exists", mongoDatabase().getCollection("pserver"), null);
		assertEquals("pserver collection contains 1", mongoDatabase().getCollection("pserver").count(), 0);

		assertFalse("Now cloud-region->tenant->vserver should not have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'_id':'/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("Now cloud-region->tenant->vserver->l-interface should not have relationship to pserver",
				mongoDatabase().getCollection("cloud-region")
						.find(Document.parse("{" +
								"'_id':'/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/mtn6/tenants/tenant/tenenat-1/vservers/vserver/vserver-1/l-interfaces/l-interface/l-int-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/cloud-infrastructure/pservers/pserver/pserver-1'" +
								"}"))
						.iterator().hasNext()
		);

	}


	@Test
	public void createPserverRelationshipsToNonExistingTest() throws Exception {

		String pserverWithRelsToNested = getEventPayload("createPserverRelationshipsToNonExistingTest");

		aaiDmaapEventProcessor.process(pserverWithRelsToNested);

	}

	@Test
	public void linterfaceWithLinterfaceTest() throws Exception {

		String linterfaceWithLinterface = getEventPayload("linterfaceWithLinterfaceTest");
		aaiDmaapEventProcessor.process(linterfaceWithLinterface);

	}

	@Test
	public void createGenericVnfWithChildrenUpdateGenericVnfProperty() throws Exception {

		JsonObject testPayloads = parser.parse(getEventPayload("create-generic-vnf-with-children-update-generic-vnf-property")).getAsJsonObject();

		String createGvnfMsg = testPayloads.get("create-generic-vnf").getAsJsonObject().toString();
		aaiDmaapEventProcessor.process(createGvnfMsg);
		assertTrue("generic-vnf in collection with vnf-name",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id':'/network/generic-vnfs/generic-vnf/generic-vnf-987654321-9-cleanup-later-PS2418'," +
								"'vnf-name':'example-vnf-name-val-45282'" +
								"}"))
						.iterator().hasNext()
		);

		String updateGvnfMsg = testPayloads.get("update-generic-vnf").getAsJsonObject().toString();
		aaiDmaapEventProcessor.process(updateGvnfMsg);
		assertTrue("generic-vnf updated vnf-name",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id':'/network/generic-vnfs/generic-vnf/generic-vnf-987654321-9-cleanup-later-PS2418'," +
								"'vnf-name':'example-vnf-name-val-45282-generic-vnf-987654321-9-cleanup-later-PS2418-patched'" +
								"}"))
						.iterator().hasNext()
		);
		assertFalse("generic-vnf with vnf-name not in collection",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id':'/network/generic-vnfs/generic-vnf/generic-vnf-987654321-9-cleanup-later-PS2418'," +
								"'vnf-name':'example-vnf-name-val-45282'" +
								"}"))
						.iterator().hasNext()
		);

	}

	@Test
	public void nosTest() throws Exception {

		String nos = getEventPayload("nos");

		aaiDmaapEventProcessor.process(nos);

	}

	@Test()
	public void addressListTest() throws Exception {

		String event = getEventPayload("address-list");
		aaiDmaapEventProcessor.process(event);

		// verifies that the uri is valid
		List<String> ids = payloadParserService.doParse("dmaap", event, PayloadParserType.AAI_RESOURCE_DMAAP)
				.stream().map(entry -> entry.getFindQuery().get("_id").getAsString()).collect(Collectors.toList());

		for (String id : ids) {
			new URI(id);
		}

		assertEquals("No id should contain '//'", 0L, ids.stream().filter(id -> id.contains("//")).count());



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
	    public void cvlanTagTest() throws Exception {

	        String event = getEventPayload("cvlan-tag");
	        aaiDmaapEventProcessor.process(event);
	        assertNotEquals("customer collection exists", mongoDatabase().getCollection("customer"), null);
	        assertEquals("customer collection contains 1", mongoDatabase().getCollection("customer").count(), 1);
	    }

	   @Test
	   public void cvlanTagEntryTest() throws Exception {

	       String event = getEventPayload("cvlan-tag-entry");

	       aaiDmaapEventProcessor.process(event);
	   }

	   @Test
	   public void cvlanTagEntryCreateTest() throws Exception {

	       String event = getEventPayload("cvlan-tag-entry-create");

	       aaiDmaapEventProcessor.process(event);
	   }
	   
       @Test
       public void allottedResourceUpdateTest() throws Exception {

           String event = getEventPayload("allotted-resource");

           aaiDmaapEventProcessor.process(event);
       }	   

	@Test
	public void delRelTest() throws Exception {

		String event = getEventPayload("deleteRelationship/1-create-logical-link");
		aaiDmaapEventProcessor.process(event);
		assertTrue("Now logical-link should exist",
				mongoDatabase().getCollection("logical-link")
						.find(Document.parse("{" +
								"'_id': '/network/logical-links/logical-link/logical-link'" +
								"}"))
						.iterator().hasNext());

		event = getEventPayload("deleteRelationship/2-create-generic-vnf");
		aaiDmaapEventProcessor.process(event);
		assertTrue("Now generic-vnf with nested l-interface with nested vlan should exist",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id': '/network/generic-vnfs/generic-vnf/generic-vnf-id'" +
								"}"))
						.iterator().hasNext());
		assertTrue("Now nested l-interface should exist",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id': '/network/generic-vnfs/generic-vnf/generic-vnf-id/l-interfaces/l-interface/l-interface-name-1'" +
								"}"))
						.iterator().hasNext());
		assertTrue("Now nested vlan should exist",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id': '/network/generic-vnfs/generic-vnf/generic-vnf-id/l-interfaces/l-interface/l-interface-name-1/vlans/vlan/vlan-1'" +
								"}"))
						.iterator().hasNext());

		event = getEventPayload("deleteRelationship/3-create-rel-generic-vnf-vlan-to-logical-link");
		aaiDmaapEventProcessor.process(event);
		assertTrue("Now generic-vnf->l-interface->vlan should have relationship to logical-link",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id': '/network/generic-vnfs/generic-vnf/generic-vnf-id/l-interfaces/l-interface/l-interface-name-1/vlans/vlan/vlan-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/network/logical-links/logical-link/logical-link'" +
								"}"))
						.iterator().hasNext());
		assertTrue("Now logical-link should have relationship to generic-vnf->l-interface->vlan",
				mongoDatabase().getCollection("logical-link")
						.find(Document.parse("{" +
								"'_id': '/network/logical-links/logical-link/logical-link'," +
								"'relationship-list.relationship.related-link':'/aai/v14/network/generic-vnfs/generic-vnf/generic-vnf-id/l-interfaces/l-interface/l-interface-name-1/vlans/vlan/vlan-1'" +
								"}"))
						.iterator().hasNext());

		event = getEventPayload("deleteRelationship/4-delete-rel-to-generic-vnf-vlan-from-logical-link");
		aaiDmaapEventProcessor.process(event);

		assertFalse("Now generic-vnf->l-interface->vlan should not have relationship to logical-link",
				mongoDatabase().getCollection("generic-vnf")
						.find(Document.parse("{" +
								"'_id': '/network/generic-vnfs/generic-vnf/generic-vnf-id/l-interfaces/l-interface/l-interface-name-1/vlans/vlan/vlan-1'," +
								"'relationship-list.relationship.related-link':'/aai/v14/network/logical-links/logical-link/logical-link'" +
								"}"))
						.iterator().hasNext());
		assertFalse("Now logical-link should not have relationship to generic-vnf->l-interface->vlan",
				mongoDatabase().getCollection("logical-link")
						.find(Document.parse("{" +
								"'_id': '/network/logical-links/logical-link/logical-link'," +
								"'relationship-list.relationship.related-link':'/aai/v14/network/generic-vnfs/generic-vnf/generic-vnf-id/l-interfaces/l-interface/l-interface-name-1/vlans/vlan/vlan-1'" +
								"}"))
						.iterator().hasNext());

	}


	@Test
	public void createPserverWithNestedDeleteNestedTest() throws Exception {

		String type = "pserver";
		String topUri = "/cloud-infrastructure/pservers/pserver/pserver-1";

		JsonObject testPayloads = parser.parse(getEventPayload("createPserverWithNestedDeleteNested")).getAsJsonObject();
		String createPserver = testPayloads.get("createPserverWithNested").toString();

		aaiDmaapEventProcessor.process(createPserver);
		assertTrue("Now pserver should be in the collections",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "'" +
								"}"))
						.iterator().hasNext());
		assertTrue("Now nested p-interface should exist",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "/p-interfaces/p-interface/interface-1'" +
								"}"))
						.iterator().hasNext());

		JsonObject existing = readObj(type, topUri);
		assertThat("Stored contains the nested", existing.toString(), containsString("\"interface-name\":\"interface-1\""));

		String deleteNested = testPayloads.get("deletedNested").toString();

		aaiDmaapEventProcessor.process(deleteNested);
		assertTrue("Now pserver should still be in the collections",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "'" +
								"}"))
						.iterator().hasNext());
		assertFalse("Now nested p-interface should not exist",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "/p-interfaces/p-interface/interface-1'" +
								"}"))
						.iterator().hasNext());

		existing = readObj(type, topUri);
		assertThat("Stored does not contain the nested", existing.toString(), not(containsString("\"interface-name\":\"interface-1\"")));
		assertThat("Stored does not contain the wrapping", existing.toString(), not(containsString("[]")));

	}

	@Test
	public void createPserverWithoutNestedAddNestedTest() throws Exception {

		String type = "pserver";
		String topUri = "/cloud-infrastructure/pservers/pserver/pserver-1";

		JsonObject testPayloads = parser.parse(getEventPayload("createPserverWithoutNestedAddNested")).getAsJsonObject();
		String createPserver = testPayloads.get("createPserverWithoutNested").toString();

		aaiDmaapEventProcessor.process(createPserver);
		assertTrue("Now pserver should be in the collections",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "'" +
								"}"))
						.iterator().hasNext());
		assertFalse("Now nested p-interface should not exist",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "/p-interfaces/p-interface/interface-1'" +
								"}"))
						.iterator().hasNext());

		JsonObject existing = readObj(type, topUri);
		assertThat("Stored does not contain the nested", existing.toString(), not(containsString("\"interface-name\":\"interface-1\"")));


		String addNested = testPayloads.get("addNested").toString();

		aaiDmaapEventProcessor.process(addNested);
		assertTrue("Now pserver should still be in the collections",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "'" +
								"}"))
						.iterator().hasNext());
		assertTrue("Now nested p-interface should exist",
				mongoDatabase().getCollection(type)
						.find(Document.parse("{" +
								"'_id': '" + topUri + "/p-interfaces/p-interface/interface-1'" +
								"}"))
						.iterator().hasNext());
		existing = readObj(type, topUri);
		assertThat("Stored does contain the nested", existing.toString(), containsString("\"interface-name\":\"interface-1\""));

	}

	private JsonObject readObj(String type, String topUri) {
		List<JsonObject> found = mongoHelperSingleton.findAllWithIdsStartingWith(type, topUri);
		JsonObject existing = new JsonObject();
		if (!found.isEmpty()) {
			JsonArray ja = new JsonArray();
			found.forEach(ja::add);
			existing = payloadPrinterService.createJson(type, ja, PayloadPrinterType.AAI_RESOURCE_GET_ALL_PRINTER);
		}
		return existing;
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


	@Test
	public void getAllPrinterObjectWithoutPropertiesButWithEmptyNestedObjectTest() throws IOException {
		String payload = getPayload("test/payloads/json/test-empty.json");
		System.out.println(payload);

		JsonObject jsonObject = parser.parse(payload).getAsJsonObject();
		System.out.println(jsonObject.toString());

		JsonArray jsonArray = new JsonArray();
		jsonArray.add(jsonObject);
		JsonObject customer = new JsonObject();
		customer.addProperty("_id","/business/customers/customer/test");
		customer.addProperty("global-customer-id","test");
		jsonArray.add(customer);
		JsonObject recreated = payloadPrinterService.createJson("customer", jsonArray, PayloadPrinterType.AAI_RESOURCE_GET_ALL_PRINTER);
		System.out.println(recreated.toString());

		assertThat("Reconstructed get all should not have empty object. ", recreated.toString(), not(Matchers.containsString("{}")));

	}
}