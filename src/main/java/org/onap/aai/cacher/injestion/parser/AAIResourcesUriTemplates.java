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
package org.onap.aai.cacher.injestion.parser;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.onap.aai.cacher.util.AAIConstants;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.util.UriUtils;

import javax.ws.rs.core.UriBuilder;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AAIResourcesUriTemplates {

    private final Map<String, String> typeToUriTemplate;

    public AAIResourcesUriTemplates() throws IOException {
        InputStream inputStream = new FileInputStream(AAIConstants.AAI_RESOURCES_URI_TEMPLATES);
        Properties prop = new Properties();
        prop.load(inputStream);

        typeToUriTemplate = new HashMap<>(prop.size() + 1);
        for (final String type : prop.stringPropertyNames()) {
            typeToUriTemplate.put(type, prop.getProperty(type));
            if (!typeToUriTemplate.containsKey("relationship")) {
                typeToUriTemplate.put("relationship", "/relationship-list/relationship/{related-link}");
            }
        }
    }

    /**
     * Get templated aai uri segment by type.
     * 
     * @param type
     * @return
     */
    public String getUriTemplateByType(String type) {
        return typeToUriTemplate.get(type);
    }

    public Map<String, String> getUriTemplateMappings(String uri, String template) {

        UriTemplate uriTemplate = new UriTemplate(template);
        Map<String, String> mappings = uriTemplate.match(uri);

        mappings.replaceAll((k, v) -> this.decodeProp(v));

        return mappings;
    }

    /**
     * For a given uri get an ordered list of templates.
     * 
     * @param uri
     * @return
     */
    public List<String> uriToTemplates(String uri) {
        List<String> uriTemplateList = new ArrayList<>();
        String template = "";
        String truncatedUri = uri;

        while (truncatedUri.contains("/")) {
            template = this.getMatchingStartingTemplate(truncatedUri).get();
            uriTemplateList.add(template);
            int count = StringUtils.countMatches(template, "/");
            if (count < StringUtils.countMatches(truncatedUri, "/")) {
                truncatedUri = StringUtils.substring(truncatedUri,
                        StringUtils.ordinalIndexOf(truncatedUri, "/", count + 1));
            } else {
                truncatedUri = "";
            }
        }

        return uriTemplateList;
    }

    /**
     * For a given uri get an ordered list of templates.
     * 
     * @param uri
     * @return
     */
    public List<String> uriToSegments(String uri) {
        List<String> uriList = new ArrayList<>();
        String template = "";
        String truncatedUri = uri;

        while (truncatedUri.contains("/")) {
            template = this.getMatchingStartingTemplate(truncatedUri).get();
            int count = StringUtils.countMatches(template, "/");
            int cutIndex = truncatedUri.length();
            if (count != StringUtils.countMatches(truncatedUri, "/")) {
                cutIndex = StringUtils.ordinalIndexOf(truncatedUri, "/", count + 1);
            }
            uriList.add(StringUtils.substring(truncatedUri, 0, cutIndex));
            truncatedUri = StringUtils.substring(truncatedUri, cutIndex);
        }

        return uriList;
    }

    /**
     * returns the template matching the start of the uri.
     * 
     * @param uri
     * @return @see java.util.Optional
     */
    public Optional<String> getMatchingStartingTemplate(String uri) {
        return typeToUriTemplate.values().stream().filter(s -> uri.startsWith(s.split("/\\{")[0])).findFirst();
    }

    /**
     * Given aai type and json object generate the uri for it.
     * 
     * @param type
     * @param jo
     * @return
     */
    public String getUri(String type, JsonObject jo) {
        String uriTemplate = getUriTemplateByType(type);
        UriBuilder uriBuilder = UriBuilder.fromPath(uriTemplate);
        List<String> keys = getUriKeys(uriTemplate);
        Map<String, String> mapping = getEncodedMapping(keys, jo);

        return uriBuilder.buildFromEncodedMap(mapping).toString();
    }

    /**
     * Get encoded values from json object for each key in keys
     * 
     * @param keys
     * @param jo
     * @return
     */
    private Map<String, String> getEncodedMapping(List<String> keys, JsonObject jo) {
        final Map<String, String> mapping = new HashMap<>();
        keys.forEach(key -> mapping.put(key, encodeProp(jo.get(key).getAsString())));

        return mapping;
    }

    /**
     * extract uri keys from the templated uri
     * 
     * @param template
     * @return
     */
    private List<String> getUriKeys(String template) {

        UriTemplate uriTemplate = new UriTemplate(template);
        return uriTemplate.getVariableNames();
    }

    /**
     * UTF-8 encoding of @param string
     * 
     * @param string string to be encoded
     * @return
     */
    public String encodeProp(String string) {
        try {
            return UriUtils.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * UTF-8 decoding of @param string
     * 
     * @param string string to be encoded
     * @return
     */
    public String decodeProp(String string) {
        try {
            return UriUtils.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}