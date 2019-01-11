package org.onap.aai.cacher.common;


import com.google.common.collect.Lists;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.onap.aai.cacher.model.CacheKey;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

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
        Assertions.assertThat(cacheKeys.size()).isEqualTo(4);
        Assertions.assertThat(
                cacheKeys.stream().allMatch(
                        it -> Lists.newArrayList("cloud-region", "complex", "pserver", "generic-vnf").contains(it.cacheKey)
                )
        ).isTrue();

    }

    private String loadFile(String pathToFile) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(pathToFile).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }
}