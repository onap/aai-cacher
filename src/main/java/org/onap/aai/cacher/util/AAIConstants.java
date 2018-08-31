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
package org.onap.aai.cacher.util;

public final class AAIConstants {
    public static final String COLLECTION_CACHEKEY = "cacheKey";

    /** Default to unix file separator if system property file.separator is null */
    public static final String FILESEP = (System.getProperty("file.separator") == null) ? "/"
            : System.getProperty("file.separator");

    public static final String AAI_BUNDLECONFIG_NAME = (System.getProperty("BUNDLECONFIG_DIR") == null) ? "resources"
            : System.getProperty("BUNDLECONFIG_DIR");
    public static final String AAI_HOME_BUNDLECONFIG = (System.getProperty("AJSC_HOME") == null)
            ? FILESEP + "opt" + FILESEP + "app" + FILESEP + "aai" + FILESEP + AAI_BUNDLECONFIG_NAME
            : System.getProperty("AJSC_HOME") + FILESEP + AAI_BUNDLECONFIG_NAME;

    public static final String AAI_HOME_ETC = AAI_HOME_BUNDLECONFIG + FILESEP + "etc" + FILESEP;
    public static final String AAI_HOME_ETC_APP_PROPERTIES = AAI_HOME_ETC + "appprops" + FILESEP;
    public static final String INITIAL_CACHEKEY_CONFIG_FILENAME = AAI_HOME_ETC_APP_PROPERTIES
            + "initialcachekeyconfig.json";
    public static final String AAI_RESOURCES_URI_TEMPLATES = AAI_HOME_ETC_APP_PROPERTIES
            + "aai-resources-uri-templates.properties";
    public static final String AAI_HOME_ETC_AUTH = AAI_HOME_ETC + "auth" + FILESEP;

    public static final String AAI_TRUSTSTORE_FILENAME = "aai.truststore.filename";
    public static final String AAI_TRUSTSTORE_PASSWD = "aai.truststore.passwd";
    public static final String AAI_KEYSTORE_FILENAME = "aai.keystore.filename";
    public static final String AAI_KEYSTORE_PASSWD = "aai.keystore.passwd";

    private AAIConstants() {
        // prevent instantiation
    }
}
