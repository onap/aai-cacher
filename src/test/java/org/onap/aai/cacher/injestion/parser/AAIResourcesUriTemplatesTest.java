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
package org.onap.aai.cacher.injestion.parser;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import de.flapdoodle.embed.mongo.MongodProcess;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.cacher.common.MongoHelperSingleton;
import org.onap.aai.cacher.injestion.parser.strategy.aai.AAIResourcesUriTemplates;
import org.onap.aai.cacher.injestion.parser.strategy.aai.AAIUriSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {InjestionTestComponent.class, AAIResourcesUriTemplatesTest.class})
public class AAIResourcesUriTemplatesTest {

	private static final String DB_NAME = AAIResourcesUriTemplatesTest.class.getSimpleName();
	private static MongoDatabase mongoDb;
	private static DB db;
	private static MongodProcess mongod;
	private static MongoClient mongoC;

	@Autowired
	AAIResourcesUriTemplates aaiResourcesUriTemplates;


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

	@BeforeClass
	public static void setup() throws IOException, InterruptedException {
		MongoServer mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress serverAddress = mongoServer.bind();

		MongoClient client = new MongoClient(new ServerAddress(serverAddress));
		mongoDb = client.getDatabase(DB_NAME);
		db = client.getDB(DB_NAME);
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
	public void getUriTemplateByType() throws Exception {

		assertEquals("Service template is returned",
				"/service-design-and-creation/services/service/{service-id}",
				aaiResourcesUriTemplates.getUriTemplateByType("service"));

		assertNull(aaiResourcesUriTemplates.getUriTemplateByType("does not exist"));

	}

	@Test
	public void getMatchingStartingTemplate() throws Exception {
		String uri = "/service-design-and-creation/services/service/id/l-interfaces/l-interface/name/p-interfaces/p-interface/name2";
		assertEquals("Service template is returned",
				"/service-design-and-creation/services/service/{service-id}",
				aaiResourcesUriTemplates.getMatchingStartingTemplate(uri).get());

		uri = "/l-interfaces/l-interface/name/p-interfaces/p-interface/name2";
		assertEquals("l-interface template is returned",
				"/l-interfaces/l-interface/{interface-name}",
				aaiResourcesUriTemplates.getMatchingStartingTemplate(uri).get());

		uri = "/l-interface/name/p-interfaces/p-interface/name2";
		assertFalse(aaiResourcesUriTemplates.getMatchingStartingTemplate(uri).isPresent());
	}

	@Test
	public void uriToTemplatesValidTest() throws Exception {
		String uri = "/service-design-and-creation/services/service/id/l-interfaces/l-interface/name/p-interfaces/p-interface/name2";
		List<String> expected = Arrays.asList(
				"/service-design-and-creation/services/service/{service-id}",
				"/l-interfaces/l-interface/{interface-name}",
				"/p-interfaces/p-interface/{interface-name}"
		);

		assertThat(aaiResourcesUriTemplates.uriToTemplates(uri), is(expected));
	}

	@Test
	public void uriToTemplatesRepeatedValidTest() throws Exception {
		String uri = "/service-design-and-creation/services/service/id/l-interfaces/l-interface/name/l-interfaces/l-interface/name2";
		List<String> expected = Arrays.asList(
				"/service-design-and-creation/services/service/{service-id}",
				"/l-interfaces/l-interface/{interface-name}",
				"/l-interfaces/l-interface/{interface-name}"
		);

		assertThat(aaiResourcesUriTemplates.uriToTemplates(uri), is(expected));
	}

	@Test
	public void uriToSegmentsValidTest() throws Exception {
		String uri = "/service-design-and-creation/services/service/id/l-interfaces/l-interface/name/p-interfaces/p-interface/name2";
		List<String> expected = Arrays.asList(
				"/service-design-and-creation/services/service/id",
				"/l-interfaces/l-interface/name",
				"/p-interfaces/p-interface/name2"
		);

		assertThat(aaiResourcesUriTemplates.uriToSegments(uri), is(expected));
	}

	@Test
	public void uriAndTemplateToKeyValueMappingTest() throws Exception {
		String uri = "/service-design-and-creation/services/service/id";
		String template = "/service-design-and-creation/services/service/{service-id}";
		Map<String, String> expected = new HashMap<>();
		expected.put("service-id", "id");

		assertThat(aaiResourcesUriTemplates.getUriTemplateMappings(uri, template), is(expected));
	}

	@Test
	public void uriAndTemplateToKeyValueMappingWithEncodingTest() throws Exception {
		String uri = "/service-design-and-creation/services/service/i%3Ad";
		String template = "/service-design-and-creation/services/service/{service-id}";
		Map<String, String> expected = new HashMap<>();
		expected.put("service-id", "i:d");

		assertThat(aaiResourcesUriTemplates.getUriTemplateMappings(uri, template), is(expected));
	}

	@Test
	public void uriAndTemplateToKeyValueMappingWihtMultipleTest() throws Exception {
		String uri = "/cloud-infrastructure/cloud-regions/cloud-region/owner/i%3Ad";
		String template = "/cloud-infrastructure/cloud-regions/cloud-region/{cloud-owner}/{cloud-region-id}";
		Map<String, String> expected = new HashMap<>();
		expected.put("cloud-owner", "owner");
		expected.put("cloud-region-id", "i:d");

		assertThat(aaiResourcesUriTemplates.getUriTemplateMappings(uri, template), is(expected));
	}

	@Test
	public void getAaiUriSegmentsTest() {

		String uri = "/service-design-and-creation/services/service/id/l-interfaces/l-interface/name/p-interfaces/p-interface/name2";

		List<AAIUriSegment> segments = aaiResourcesUriTemplates.getAaiUriSegments(uri);

		assertEquals("3 segments are generated", 3, segments.size());

		assertEquals("Segment 1 plural is services", "services", segments.get(0).getSegmentPlural().get());
		assertEquals("Segment 2 plural is l-interfaces", "l-interfaces", segments.get(1).getSegmentPlural().get());
		assertEquals("Segment 3 plural is p-interfaces", "p-interfaces", segments.get(2).getSegmentPlural().get());

		assertEquals("Segment 1 singular is service", "service", segments.get(0).getSegmentSingular());
		assertEquals("Segment 2 singular is l-interface", "l-interface", segments.get(1).getSegmentSingular());
		assertEquals("Segment 3 singular is p-interface", "p-interface", segments.get(2).getSegmentSingular());

		assertEquals("Segment 1 template",
				"/service-design-and-creation/services/service/{service-id}",
				segments.get(0).getSegmentTemplate());
		assertEquals("Segment 2 template",
				"/l-interfaces/l-interface/{interface-name}",
				segments.get(1).getSegmentTemplate());
		assertEquals("Segment 3 template",
				"/p-interfaces/p-interface/{interface-name}",
				segments.get(2).getSegmentTemplate());

		assertEquals("Segment 1 uri",
				"/service-design-and-creation/services/service/id",
				segments.get(0).getSegment());
		assertEquals("Segment 2 uri",
				"/l-interfaces/l-interface/name",
				segments.get(1).getSegment());
		assertEquals("Segment 3 uri",
				"/p-interfaces/p-interface/name2",
				segments.get(2).getSegment());

		Map<String, String> expected = new HashMap<>();
		expected.put("service-id", "id");
		assertEquals("Segment 1 mapping", expected, segments.get(0).getSegmentKeyValues());
		expected = new HashMap<>();
		expected.put("interface-name", "name");
		assertEquals("Segment 2 mapping", expected, segments.get(1).getSegmentKeyValues());
		expected = new HashMap<>();
		expected.put("interface-name", "name2");
		assertEquals("Segment 3 mapping", expected, segments.get(2).getSegmentKeyValues());
	}

	@Test
	public void getFullUriPrefixTest() throws Exception {
		assertEquals("/aai/v12", aaiResourcesUriTemplates.getAAIUriFromEntityUriPrefix(("/aai/v12/network/pnfs/pnf/pnf-name-value/p-interfaces/p-interface/xe-10%2F3%2F2/l-interfaces/l-interface/l-interface-name")));
		assertEquals("/aai/v4", aaiResourcesUriTemplates.getAAIUriFromEntityUriPrefix("/aai/v4/names"));
	}

}