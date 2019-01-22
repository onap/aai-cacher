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

package org.onap.aai.cacher.injestion.parser.strategy.aai.dmaap;

public class AAIRelatedToDetails {
	private String fullUri;
	private String label;
	private DmaapAction actionType;

	public AAIRelatedToDetails(String fullUri, String label, DmaapAction actionType) {
		this.fullUri = fullUri;
		this.label = label;
		this.actionType = actionType;
	}

	public String getFullUri() {
		return fullUri;
	}

	public String getLabel() {
		return label;
	}

	public DmaapAction getActionType() {
		return actionType;
	}

	public void setActionType(DmaapAction actionType) {
		this.actionType = actionType;
	}

	@Override
	public String toString() {
		return fullUri + " : " + label;
	}
}