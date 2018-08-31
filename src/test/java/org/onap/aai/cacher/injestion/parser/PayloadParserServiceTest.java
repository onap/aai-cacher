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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.aai.cacher.injestion.parser.strategy.PayloadParserType;
import org.onap.aai.cacher.model.CacheEntry;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InjestionTestComponent.class)
public class PayloadParserServiceTest {

	@Autowired
	private PayloadParserService parserService;

	private JsonParser parser = new JsonParser();

	private String aaiGetAllServiceResponse =
			"{" +
			"	'service': [" +
			"		{" +
			"			'service-id': 'service-id-1:1'," +
			"			'service-description': 'A'," +
			"			'resource-version': '1'" +
			"		}," +
			"		{" +
			"			'service-id': 'service-id-2'," +
			"			'service-description': 'B'," +
			"			'resource-version': '2'" +
			"		}" +
			"	]" +
			"}";
	private JsonObject aaiGetAllServiceResponseJson = parser.parse(aaiGetAllServiceResponse).getAsJsonObject();



	private void print(List<CacheEntry> result) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		result.forEach(e -> System.out.println("Collection: " + e.getCollection() + "\nKey: " + e.getId() + "\n" + gson.toJson(e.getPayload())));
	}

	@Test
	public void testGetAllAAIResourceTest() throws JSONException {
		String expectedUri = "/service-design-and-creation/services/service/service-id-1%3A1";

		List<CacheEntry> result = parserService.doParse("service", aaiGetAllServiceResponseJson, "aai-resource-get-all");
		print(result);

		assertTrue(result.stream().map(cacheIdentifier -> cacheIdentifier.getId()).anyMatch(id -> id.equals(expectedUri)));
		JSONAssert.assertEquals(
				new JSONObject(aaiGetAllServiceResponseJson.getAsJsonArray("service").get(0).getAsJsonObject().toString()),
				new JSONObject(result.stream().filter(e -> e.getId().equals(expectedUri)).map(CacheEntry::getPayload).findFirst().get().toString()),
				false);

	}

	@Test
	public void testGetAllAAIResourceStringTest() throws JSONException {
		String expectedUri = "/service-design-and-creation/services/service/service-id-2";

		List<CacheEntry> result = parserService.doParse("service", aaiGetAllServiceResponse, PayloadParserType.AAI_RESOURCE_GET_ALL);
		print(result);

		assertTrue(result.stream().map(cacheIdentifier -> cacheIdentifier.getId()).anyMatch(id -> id.equals(expectedUri)));
		JSONAssert.assertEquals(
				new JSONObject(aaiGetAllServiceResponseJson.getAsJsonArray("service").get(1).getAsJsonObject().toString()),
				new JSONObject(result.stream().filter(e -> e.getId().equals(expectedUri)).map(CacheEntry::getPayload).findFirst().get().toString()),
				false);
	}

	@Test
	public void testNoneStrategyTest1() throws JSONException {
		String cacheKey = "service";
		List<CacheEntry> result = parserService.doParse(cacheKey, aaiGetAllServiceResponse);
		print(result);
		noneTests(cacheKey, result);
	}

	@Test
	public void testNoneStrategyTest2() throws JSONException {
		String cacheKey = "service";
		List<CacheEntry> result = parserService.doParse(cacheKey, aaiGetAllServiceResponseJson);
		print(result);
		noneTests(cacheKey, result);
	}

	@Test
	public void testNoneStrategyTest3() throws JSONException {
		String cacheKey = "service";
		List<CacheEntry> result = parserService.doParse(cacheKey, aaiGetAllServiceResponse, PayloadParserType.NONE);
		print(result);
		noneTests(cacheKey, result);
	}

	@Test
	public void testNoneStrategyTest4() throws JSONException {
		String cacheKey = "service";
		List<CacheEntry> result = parserService.doParse(cacheKey, aaiGetAllServiceResponse, "none");
		print(result);
		noneTests(cacheKey, result);
	}

	private void noneTests(String cacheKey, List<CacheEntry> result) throws JSONException {
		assertThat(result.size(), is(1));
		assertTrue(result.stream().map(cacheIdentifier -> cacheIdentifier.getId()).anyMatch(id -> id.equals(cacheKey)));
		JSONAssert.assertEquals(
				new JSONObject(aaiGetAllServiceResponse),
				new JSONObject(result.get(0).getPayload().toString()),
				false);
	}


}

