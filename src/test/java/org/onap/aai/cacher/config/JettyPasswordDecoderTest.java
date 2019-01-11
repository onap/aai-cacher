/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Nokia Intellectual Property. All rights reserved.
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
package org.onap.aai.cacher.config;

import org.eclipse.jetty.util.security.Password;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Bogumil Zebek
 */
public class JettyPasswordDecoderTest {

    private JettyPasswordDecoder jettyPasswordDecoder;

    @Before
    public void setUp(){
        this.jettyPasswordDecoder = new JettyPasswordDecoder();
    }

    @Test
    public void shouldDecodeObfuscatedPassword(){
        String encoded = Password.obfuscate("password");
        assertEquals("password", jettyPasswordDecoder.decode(encoded));
    }

    @Test
    public void shouldDecodeValueOfObfuscatedPassword(){
        String encoded = "1v2j1uum1xtv1zej1zer1xtn1uvk1v1v";
        assertEquals("password", jettyPasswordDecoder.decode(encoded));
    }
}