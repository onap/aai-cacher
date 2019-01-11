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