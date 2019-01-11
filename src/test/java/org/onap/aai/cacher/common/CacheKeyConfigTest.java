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
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.aai.cacher.common;


import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.onap.aai.cacher.model.CacheKey;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Bogumil Zebek
 */
public class CacheKeyConfigTest {

    @Test
    public void shouldProcessCacheConfigurationStoredInFile() throws IOException {

        // given
        String cacheKeyConfigJson = loadFile("test/etc/initialcachekeyconfig.json");
        CacheKeyConfig cacheKeyConfig = new CacheKeyConfig(cacheKeyConfigJson);

        // when
        List<CacheKey> cacheKeys = cacheKeyConfig.populateCacheKeyList();

        // then
        assertThat(cacheKeys.size()).isEqualTo(4);
        assertThat(
                cacheKeys.stream().allMatch(
                        it -> Lists.newArrayList("cloud-region", "complex", "pserver", "generic-vnf").contains(it.cacheKey)
                )
        ).isTrue();

    }

    private String loadFile(String pathToFile) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(pathToFile);
        if (resource == null) {
            throw new InvalidPathException("Missing file.", String.format("File '%s' not found.", pathToFile));
        }
        File file = new File(resource.getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }
}