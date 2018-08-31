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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InjestionTestComponent.class)
public class AAIResourcesUriTemplatesTest {

	@Autowired
	AAIResourcesUriTemplates aaiResourcesUriTemplates;

	@Test
	public void getUriTemplateByType() throws Exception {

		assertEquals("Service template is returned",
				"/service-design-and-creation/services/service/{service-id}",
				aaiResourcesUriTemplates.getUriTemplateByType("service"));

		assertFalse(aaiResourcesUriTemplates.getUriTemplateByType("does not exist") != null);

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

}