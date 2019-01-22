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

package org.onap.aai.cacher.injestion.parser.strategy.aai;

import java.util.Map;
import java.util.Optional;

public class AAIUriSegment {

	private String segment;
	private String segmentTemplate;
	private Optional<String> segmentPlural = Optional.empty();
	private String segmentSingular;
	private Map<String, String> segmentKeyValues;

	public AAIUriSegment(String segment, String template) {
		this.segment = segment;
		this.segmentTemplate = template;
		String[] segmentSplit = segment.split("/");
		String[] templateSplit = template.split("/");
		for (int i = 0; i < templateSplit.length; i++) {
			if (templateSplit[i].contains("{")) {
				segmentSingular = segmentSplit[i - 1];
				if (!"".equals(segmentSplit[i - 2])) {
					segmentPlural = Optional.of(segmentSplit[i - 2]);
				}
				break;
			}
		}
	}

	public String getSegment() {
		return segment;
	}

	public String getSegmentTemplate() {
		return segmentTemplate;
	}

	public Map<String, String> getSegmentKeyValues() {
		return segmentKeyValues;
	}

	public void setSegmentKeyValues(Map<String, String> segmentKeyValues) {
		this.segmentKeyValues = segmentKeyValues;
	}

	public Optional<String> getSegmentPlural() {
		return segmentPlural;
	}

	public String getSegmentSingular() {
		return segmentSingular;
	}
}