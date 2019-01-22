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

import com.google.gson.JsonSyntaxException;
import org.junit.Before;
import org.junit.Test;


public class AAIDmaapEventProcessorTest {
    private AAIDmaapEventProcessor eventProcessor;

	private String validEventMessage = "{'cambria.partition': 'AAI','event-header': {'id': 'ABC','source-name': 'sourceName'},'entity': {'hostname': 'hostName'}}";
	private String invalidEventMessageHeader = "{'cambria.partition': 'AAI','Xevent-header': {'id': 'ABC','source-name': 'sourceName'},'entity': {'hostname': 'hostName'}}";
	private String invalidEventMessageHeaderMissingId = "{'cambria.partition': 'AAI','event-header': {'idX': 'ABC','source-name': 'sourceName'},'entity': {'hostname': 'hostName'}}";
	private String invalidEventMessageHeaderMissingSourceName = "{'cambria.partition': 'AAI','event-header': {'id': 'ABC','source-nameX': 'sourceName'},'entity': {'hostname': 'hostName'}}";
	private String invalidEventMessageBody = "{'cambria.partition': 'AAI','event-header': {'id': 'ABC','source-name': 'sourceName'},'Xentity': {'hostname': 'hostName'}}";


    @Before
    public void setUp() throws Exception {
    	eventProcessor = new AAIDmaapEventProcessor();
    }

    @Test(expected = IllegalStateException.class)
    public void testJSONException() throws Exception {
    	eventProcessor.process("invalidJson");
    }
    
    @Test(expected = JsonSyntaxException.class)
    public void testInvalidHeader() throws Exception {
    	eventProcessor.process(invalidEventMessageHeader);
    }
    
    @Test(expected = JsonSyntaxException.class)
    public void testInvalidHeaderMissingId() throws Exception {
    	eventProcessor.process(invalidEventMessageHeaderMissingId);
    }
    
    @Test(expected = JsonSyntaxException.class)
    public void testInvalidHeaderMissingSourceName() throws Exception {
    	eventProcessor.process(invalidEventMessageHeaderMissingSourceName);
    }
    
    @Test(expected = JsonSyntaxException.class)
    public void testInvalidEventMessageBody() throws Exception {
    	eventProcessor.process(invalidEventMessageBody);
    }
}
