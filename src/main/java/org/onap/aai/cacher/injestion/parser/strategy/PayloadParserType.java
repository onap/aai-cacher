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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum PayloadParserType {

	NONE("none"),
	AAI_RESOURCE_GET_ALL("aai-resource-get-all"),
	AAI_RESOURCE_DMAAP("aai-resource-dmaap");

	private static final Map<String, PayloadParserType> MAP;
	static {
		MAP = Arrays.stream(values()).collect(Collectors.toMap(PayloadParserType::getValue, Function.identity()));
	}

	private final String value;

	PayloadParserType(String input) {
		this.value = input;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static PayloadParserType fromString(String text) {
		return MAP.get(text);
	}
}

