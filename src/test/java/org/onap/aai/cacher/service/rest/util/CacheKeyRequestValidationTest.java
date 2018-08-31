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
package org.onap.aai.cacher.service.rest.util;

import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.aai.cacher.service.helper.CacheHelperService;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CacheKeyRequestValidationTest {
	
    protected CacheHelperService cacheHelperService;
    protected CacheKeyRequestValidation addCacheKeyRequestValidation;
    protected CacheKeyRequestValidation updateCacheKeyRequestValidation;
    
	private String emptyPayload = "{}";
	private String nonEmptyPayload = "{\"cacheKey\" : \"complex\"}";
	private JsonParser parser;
    
    @Before
    public void setup() {
    	cacheHelperService = Mockito.mock(CacheHelperService.class);
    	addCacheKeyRequestValidation = new CacheKeyRequestValidation(CacheKeyRequestValidationType.ADD);
    	updateCacheKeyRequestValidation = new CacheKeyRequestValidation(CacheKeyRequestValidationType.UPDATE);
    	parser = new JsonParser();
    }
    
    @Test
    public void testNullPayload() {
    	List<String> results = addCacheKeyRequestValidation.validateCacheKeyRequest(null, cacheHelperService);
    	assertEquals("null payload ok", "Unsupported CacheKey request format, empty payload.", results.get(0));
    }
    
    @Test
    public void testEmptyPayload() {
    	List<String> results = addCacheKeyRequestValidation.validateCacheKeyRequest(parser.parse(emptyPayload).getAsJsonObject(), cacheHelperService);
    	assertEquals("empty payload ok", "Unsupported CacheKey request format, unspecified cacheKey.", results.get(0));
    }
    
    @Test
    public void testAddNewCacheKey() {
    	Mockito.when(cacheHelperService.isKeyPresent(Mockito.any(), Mockito.anyString())).thenReturn(false);
    	List<String> results = addCacheKeyRequestValidation.validateCacheKeyRequest(parser.parse(nonEmptyPayload).getAsJsonObject(), cacheHelperService);
    	assertEquals("add new CacheKey ok", 0, results.size());
    }
    
    @Test
    public void testAddExistingCacheKey() {
    	Mockito.when(cacheHelperService.isKeyPresent(Mockito.any(), Mockito.anyString())).thenReturn(true);
    	List<String> results = addCacheKeyRequestValidation.validateCacheKeyRequest(parser.parse(nonEmptyPayload).getAsJsonObject(), cacheHelperService);
    	assertEquals("add existing CacheKey ok", "Invalid request to add cacheKey complex, cacheKey exists.", results.get(0));
    }
    
    
    @Test
    public void testUpdateNewCacheKey() {
    	Mockito.when(cacheHelperService.isKeyPresent(Mockito.any(), Mockito.anyString())).thenReturn(false);
    	List<String> results = updateCacheKeyRequestValidation.validateCacheKeyRequest(parser.parse(nonEmptyPayload).getAsJsonObject(), cacheHelperService);
    	assertEquals("update new CacheKey ok", "Invalid request to update cacheKey complex, cacheKey does not exist.", results.get(0));
    }
    
    @Test
    public void testUpdateExistingCacheKey() {
    	Mockito.when(cacheHelperService.isKeyPresent(Mockito.any(), Mockito.anyString())).thenReturn(true);
    	List<String> results = updateCacheKeyRequestValidation.validateCacheKeyRequest(parser.parse(nonEmptyPayload).getAsJsonObject(), cacheHelperService);
    	assertEquals("update existing CacheKey ok", 0, results.size());
    }   
}
