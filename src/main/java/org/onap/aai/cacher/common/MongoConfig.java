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
package org.onap.aai.cacher.common;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.runtime.Network;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class MongoConfig {

    private static final EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.host}")
    private String mongoDbHost;
    @Value("${spring.data.mongodb.database}")
    private String mongoDbName;
    @Value("${spring.data.mongodb.port}")
    private int mongoDbPort;

    @Bean
    public MongoClient mongoClient(MongodProcess mongodProcess) {
        // To connect to mongodb server
        try (MongoClient mongoC = new MongoClient(mongoDbHost, mongoDbPort)) {

            // Now connect to your databases
            EELF_LOGGER.info("Connect to database successfully");

            return mongoC;

        } catch (Exception e) {
            AAIException aaiException = new AAIException(MongoHelperSingleton.AAI_4000_LBL);
            ErrorLogHelper.logException(aaiException);
        }

        return null;
    }

    /**
     * @deprecated replaced by {@link #mongoDatabase(MongoClient mongoClient)}
     * @param mongoClient mongo client
     * @return DB
     */
    @Bean
    @Deprecated
    public DB db(MongoClient mongoClient) {
        return mongoClient.getDB(mongoDbName);
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        return mongoClient.getDatabase(mongoDbName);
    }

    @Bean
    public MongodProcess mongoEmbedded() throws IOException {

        Logger logger = LoggerFactory.getLogger("mongo");
        int port = mongoDbPort;

        MongodConfig mongoConfigConfig = MongodConfig.builder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(port, Network.localhostIsIPv6()))
                .cmdOptions(MongoCmdOptions.builder().enableTextSearch(true).useNoPrealloc(false).build())
                .isConfigServer(false)
                .build();

        ProcessOutput processOutput = new ProcessOutput(Processors.logTo(logger, Slf4jLevel.WARN), Processors.logTo(logger,
                Slf4jLevel.WARN), Processors.logTo(logger, Slf4jLevel.WARN));

        MongodExecutable mongodExecutable = MongodStarter
                .getInstance(Defaults.runtimeConfigFor(Command.MongoD)
                        .processOutput(processOutput)
                        .build())
                .prepare(mongoConfigConfig);

        MongodProcess mongod = mongodExecutable.start();

        if (mongod.isProcessRunning()) {
            System.out.println("Embedded mongo RUNNING");
            logger.info("Embedded mongo RUNNING");
        }
        return mongod;
    }
}