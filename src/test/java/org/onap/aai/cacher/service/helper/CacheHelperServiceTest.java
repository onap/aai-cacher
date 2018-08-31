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
package org.onap.aai.cacher.service.helper;

import org.junit.Test;
import org.onap.aai.cacher.model.CacheKey;

import java.text.SimpleDateFormat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheHelperServiceTest {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss.SSSZ");

	@Test
	public void isShouldTrigger1() throws Exception {

		CacheKey cacheKey = new CacheKey("test");
		cacheKey.setLastSyncStartTime("-1");
		cacheKey.setLastSyncEndTime("-1");
		cacheKey.setLastSyncSuccessTime("-1");
		cacheKey.setSyncInterval("1");

		CacheHelperService cacheHelperService = new CacheHelperService();
		assertTrue("No timings set (should trigger)", cacheHelperService.isShouldTrigger(cacheKey));
	}

	@Test
	public void isShouldTrigger2() throws Exception {

		CacheKey cacheKey = new CacheKey("test");
		cacheKey.setLastSyncStartTime(sdf.format(System.currentTimeMillis()-300000)); //5 mins ago
		cacheKey.setLastSyncEndTime("-1"); // has not ended
		cacheKey.setLastSyncSuccessTime("-1");
		cacheKey.setSyncInterval("1"); //1 min interval

		CacheHelperService cacheHelperService = new CacheHelperService();
		assertFalse("Start time set (should not trigger)", cacheHelperService.isShouldTrigger(cacheKey));
	}

	@Test
	public void isShouldTrigger3() throws Exception {

		CacheKey cacheKey = new CacheKey("test");
		cacheKey.setLastSyncStartTime(sdf.format(System.currentTimeMillis()-300000)); //5 mins ago
		cacheKey.setLastSyncEndTime(sdf.format(System.currentTimeMillis()-240000)); // 4 mins ago
		cacheKey.setLastSyncSuccessTime("-1");
		cacheKey.setSyncInterval("1"); //1 min interval

		CacheHelperService cacheHelperService = new CacheHelperService();
		assertTrue("Start time less than endtime (should trigger)", cacheHelperService.isShouldTrigger(cacheKey));
	}

	@Test
	public void isShouldTrigger4() throws Exception {

		CacheKey cacheKey = new CacheKey("test");
		cacheKey.setLastSyncStartTime(sdf.format(System.currentTimeMillis()-300000)); //5 mins ago
		cacheKey.setLastSyncEndTime(sdf.format(System.currentTimeMillis()-360000)); // 6 mins ago
		cacheKey.setLastSyncSuccessTime("-1");
		cacheKey.setSyncInterval("1"); //1 min interval

		CacheHelperService cacheHelperService = new CacheHelperService();
		assertFalse("Start time greater than endtime (should not trigger)", cacheHelperService.isShouldTrigger(cacheKey));
	}



}