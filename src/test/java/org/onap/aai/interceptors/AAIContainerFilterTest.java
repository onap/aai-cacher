/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 *
 * Copyright © 2019 IBM Intellectual Property. All rights reserved.
 *
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
package org.onap.aai.interceptors;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AAIContainerFilterTest {

    AAIContainerFilter aaiContainerFilter;

    @Before
    public void setUp() throws Exception {
        aaiContainerFilter = new AAIContainerFilter(){};
    }

    @Test
    public void isValidUUID() {
        assertTrue(aaiContainerFilter.isValidUUID(UUID.randomUUID().toString()));
        assertFalse(aaiContainerFilter.isValidUUID("test"));
    }
}