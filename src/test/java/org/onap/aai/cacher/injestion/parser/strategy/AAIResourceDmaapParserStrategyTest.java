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
package org.onap.aai.cacher.injestion.parser.strategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.cacher.injestion.parser.InjestionTestComponent;
import org.onap.aai.cacher.injestion.parser.PayloadParserService;
import org.onap.aai.cacher.model.CacheEntry;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InjestionTestComponent.class)
public class AAIResourceDmaapParserStrategyTest {

	@Autowired
	private PayloadParserService parserService;

	@Autowired
	@Qualifier("aai-resource-dmaap")
	private AAIResourceDmaapParserStrategy aaiResourceDmaapParserStrategy;

	private JsonParser parser = new JsonParser();

	private JsonObject pserverCreateEvent = parser.parse(
			new FileReader("./src/test/resources/test/payloads/dmaap-pserver-create.json")).getAsJsonObject();

	public AAIResourceDmaapParserStrategyTest() throws FileNotFoundException {}

	private void print(List<CacheEntry> result) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		result.forEach(e -> System.out.println("\n\nCollection: " + e.getCollection() +
				"\nKey: " + e.getId() +
				"\nFind: " + gson.toJson(e.getFindQuery()) +
				"\nNestedFind: " + gson.toJson(e.getNestedFind()) +
				"\nNestedField: " + e.getNestedField() +
				"\nNestedFieldIdentifier: " + gson.toJson(e.getNestedFieldIdentifierObj()) +
				"\nPayload: " + gson.toJson(e.getPayload())));
	}

	@Test
	public void test(){
		List<CacheEntry> result =  parserService.doParse("dmaapEvent", pserverCreateEvent, PayloadParserType.AAI_RESOURCE_DMAAP);
		print(result);
	}


	@Test
	public void getUriTest() {
		String fullUri = aaiResourceDmaapParserStrategy.getFullUri(pserverCreateEvent.getAsJsonObject("event-header"));
		assertEquals("/aai/v12/cloud-infrastructure/pservers/pserver/dmaap-pserver-create", fullUri);
		String uri = aaiResourceDmaapParserStrategy.getUri(fullUri);
		assertEquals("/cloud-infrastructure/pservers/pserver/dmaap-pserver-create", uri);
	}

	@Test
	public void getAaiUriSegmentsTest() {

		String uri = "/service-design-and-creation/services/service/id/l-interfaces/l-interface/name/p-interfaces/p-interface/name2";

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);

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
	public void getEntityBodyChildTest() throws Exception {
		String uri = "/cloud-infrastructure/cloud-regions/cloud-region/onap-cloud-owner/ams1b/tenants/tenant/52fd05137ab4453bb53084a13c7bb7a4/vservers/vserver/vs-id";
		String entityString =
				"{" +
				"	'tenants':" +
				"	{" +
				"		'tenant': [" +
				"			{" +
				"				'vservers':" +
				"				{" +
				"					'vserver': [" +
				"						{" +
				"							'in-maint': false," +
				"							'resource-version': '1525978690717'," +
				"							'vserver-name': 'slaa-regression-cr-id-api-server-449704329'," +
				"							'vserver-id': 'vs-id'" +
				"						}" +
				"					]" +
				"				}," +
				"				'tenant-id': 'ten-id'," +
				"				'tenant-name': 'name'" +
				"			}" +
				"		]" +
				"	}," +
				"	'cloud-owner': 'cr-o'," +
				"	'owner-defined-type': 'lcp'," +
				"	'cloud-region-id': 'cr-id'" +
				"}";

		JsonObject entity = parser.parse(entityString).getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);

		JsonObject result = aaiResourceDmaapParserStrategy.getEntityBody(entity, segments);

		JSONAssert.assertEquals(
				new JSONObject(entity
						.getAsJsonObject("tenants").getAsJsonArray("tenant").get(0)	.getAsJsonObject()
						.getAsJsonObject("vservers").getAsJsonArray("vserver").get(0).getAsJsonObject()
						.toString()),
				new JSONObject(result.toString()),
				true);

	}

	@Test
	public void getEntityBodyTopTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn";
		String entityString = "{'hostname':'hn','in-maint':false,'resource-version':'1525801811662','pserver-id':'0A47B945-9C74-4CBE-AD72-0DECB966EB94'}";

		JsonObject entity = parser.parse(entityString).getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);

		JsonObject result = aaiResourceDmaapParserStrategy.getEntityBody(entity, segments);

		JSONAssert.assertEquals(
				new JSONObject(entity.toString()),
				new JSONObject(result.toString()),
				true);

	}

	@Test
	public void getFindQueryTopTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn";
		JsonObject expected = parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/hn'," +
				"'hostname':'hn'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject findQuery = aaiResourceDmaapParserStrategy.getFindQuery(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(findQuery.toString()),
				true);
	}

	@Test
	public void getFindQueryOneLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1";
		JsonObject expected = parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/hn'," +
				"'hostname':'hn'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject findQuery = aaiResourceDmaapParserStrategy.getFindQuery(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(findQuery.toString()),
				true);
	}

	@Test
	public void getFindQueryTwoLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1/l-interfaces/l-interface/interface-2";
		JsonObject expected = parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/hn'," +
				"'hostname':'hn'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject findQuery = aaiResourceDmaapParserStrategy.getFindQuery(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(findQuery.toString()),
				true);
	}

	@Test
	public void getNestedFindQueryTopTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn";
		JsonObject expected = parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/hn'," +
				"'hostname':'hn'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject findQuery = aaiResourceDmaapParserStrategy.getNestedFindQuery(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(findQuery.toString()),
				true);
	}

	@Test
	public void getNestedFindQueryOneLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1";
		JsonObject expected = parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/hn'," +
				"'hostname':'hn'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject findQuery = aaiResourceDmaapParserStrategy.getNestedFindQuery(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(findQuery.toString()),
				true);
	}

	@Test
	public void getNestedFindQueryTwoLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1/l-interfaces/l-interface/interface-2";
		JsonObject expected = parser.parse("{'_id':'/cloud-infrastructure/pservers/pserver/hn'," +
				"'hostname':'hn'," +
				"'p-interfaces.p-interface.interface-name':'interface-1'," +
				"'p-interfaces.p-interface.l-interfaces.l-interface.interface-name':'interface-2'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject findQuery = aaiResourceDmaapParserStrategy.getNestedFindQuery(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(findQuery.toString()),
				true);
	}

	@Test
	public void getNestedFieldTopTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn";
		String expected = "";

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		String nestedField = aaiResourceDmaapParserStrategy.getNestedField(segments);

		assertEquals("Top nested field", expected, nestedField);
	}

	@Test
	public void getNestedFieldOneLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1";
		String expected = "p-interfaces.p-interface";

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		String nestedField = aaiResourceDmaapParserStrategy.getNestedField(segments);

		assertEquals("Top nested field", expected, nestedField);
	}

	@Test
	public void getNestedFieldTwoLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1/l-interfaces/l-interface/interface-2";
		String expected = "p-interfaces.p-interface.$.l-interfaces.l-interface";

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		String nestedField = aaiResourceDmaapParserStrategy.getNestedField(segments);

		assertEquals("Top nested field", expected, nestedField);
	}

	@Test
	public void getNestedFieldThreeLevelOddCaseTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1/l-interfaces/l-interface/interface-2/l3-interface-ipv4-address-list/addressA";
		String expected = "p-interfaces.p-interface.$.l-interfaces.l-interface.$.l3-interface-ipv4-address-list";

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		String nestedField = aaiResourceDmaapParserStrategy.getNestedField(segments);

		assertEquals("Top nested field", expected, nestedField);
	}




	@Test
	public void getNestedIdentifierTopTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn";
		JsonObject expected = parser.parse("{}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject nestedIdentifier = aaiResourceDmaapParserStrategy.getNestedIdentifier(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(nestedIdentifier.toString()),
				true);
	}

	@Test
	public void getNestedIdentifierOneLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1";
		JsonObject expected = parser.parse("{'interface-name':'interface-1'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject nestedIdentifier = aaiResourceDmaapParserStrategy.getNestedIdentifier(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(nestedIdentifier.toString()),
				true);
	}

	@Test
	public void getNestedIdentifierTwoLevelTest() throws Exception {
		String uri = "/cloud-infrastructure/pservers/pserver/hn/p-interfaces/p-interface/interface-1/l-interfaces/l-interface/interface-2";
		JsonObject expected = parser.parse("{'interface-name':'interface-2'}").getAsJsonObject();

		List<AAIResourceDmaapParserStrategy.AAIUriSegment> segments = aaiResourceDmaapParserStrategy.getAaiUriSegments(uri);
		JsonObject nestedIdentifier = aaiResourceDmaapParserStrategy.getNestedIdentifier(segments);

		JSONAssert.assertEquals(
				new JSONObject(expected.toString()),
				new JSONObject(nestedIdentifier.toString()),
				true);
	}


	@Test
	public void getFullUriPrefixTest() throws Exception {
		assertEquals("/aai/v12", aaiResourceDmaapParserStrategy.getFullUriPrefix("/aai/v12/network/pnfs/pnf/pnf-name-value/p-interfaces/p-interface/xe-10%2F3%2F2/l-interfaces/l-interface/l-interface-name"));
		assertEquals("/aai/v4", aaiResourceDmaapParserStrategy.getFullUriPrefix("/aai/v4/names"));
	}


	@Test
	public void fullUriToRelationshipObj() throws Exception {
		String fullUri = AAIResourceDmaapParserStrategyTestConstants.VSERVER_URI;
		String expectedRelObj = AAIResourceDmaapParserStrategyTestConstants.VSERVER_RELATIONSHIP_OBJ;
		JsonObject relObj = aaiResourceDmaapParserStrategy.fullUriToRelationshipObj(fullUri, "tosca.relationships.HostedOn");

		JSONAssert.assertEquals(new JSONObject(expectedRelObj), new JSONObject(relObj.toString()), true);
	}

	@Test
	public void verifyRelationshipEntriesOnUpdateTest() throws Exception {
		List<CacheEntry> result = aaiResourceDmaapParserStrategy
				.process("TEST", parser.parse(AAIResourceDmaapParserStrategyTestConstants.GENERIC_VNF_EVENT_WITH_2_RELAT).getAsJsonObject());

		assertEquals(3, result.size());

	}
	@Test
	public void verifyRelationshipEntriesSimpleEvent() throws Exception {
		List<CacheEntry> result = aaiResourceDmaapParserStrategy
				.process("TEST", parser.parse(AAIResourceDmaapParserStrategyTestConstants.GENERIC_VNF_EVENT).getAsJsonObject());

		assertEquals(1, result.size());

	}

	@Test
	public void getFromRelationshipFullUriToRelationshipObjTest() throws Exception {
				String entity = AAIResourceDmaapParserStrategyTestConstants.FULL_PSERVER;
				String fullUri = AAIResourceDmaapParserStrategyTestConstants.FULL_PSERVER_URI;
				MultiValueMap<String, AAIResourceDmaapParserStrategy.AAIRelatedToDetails> result = aaiResourceDmaapParserStrategy.getFromRelationshipFullUriToRelationshipObj(parser.parse(entity).getAsJsonObject(), fullUri);

				assertEquals(3, result.size());

	}
}