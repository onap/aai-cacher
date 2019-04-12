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
import de.flapdoodle.embed.mongo.config.*;
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
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;


@Configuration
public class MongoConfig {

    private final static EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(MongoConfig.class);

    @Value("${mongodb.host}")
    private String MONGO_DB_HOST;
    @Value("${mongodb.dbName}")
    private String MONGO_DB_NAME;
    @Value("${mongodb.port}")
    private int MONGO_DB_PORT;

    private MongodProcess mongod;

    @Bean("mongo-client")
    public MongoClient mongoClient(MongodProcess mongodProcess) {
        try {
            // To connect to mongodb server
            MongoClient mongoC = new MongoClient(MONGO_DB_HOST, MONGO_DB_PORT);

            // Now connect to your databases
            EELF_LOGGER.info("Connect to database successfully");

            return mongoC;

        } catch (Exception e) {
            AAIException aaiException = new AAIException("AAI_4000");
            ErrorLogHelper.logException(aaiException);
        }

        return null;
    }

    @Bean
    @DependsOn({"mongo-embedded", "mongo-client"})
    public DB db(MongoClient mongoClient) {
        return mongoClient.getDB(MONGO_DB_NAME);
    }

    @Bean
    @DependsOn({"mongo-embedded", "mongo-client"})
    public MongoDatabase mongoDatabase(MongoClient mongoClient) {
        return mongoClient.getDatabase(MONGO_DB_NAME);
    }

    @Bean("mongo-embedded")
    public MongodProcess mongoEmbedded() throws IOException {

        Logger logger = LoggerFactory.getLogger("mongo");
        int port = MONGO_DB_PORT;

        IMongodConfig mongoConfigConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(port, Network.localhostIsIPv6()))
                .cmdOptions(new MongoCmdOptionsBuilder().enableTextSearch(true).useNoPrealloc(false).build())
                .configServer(false)
                .build();

        ProcessOutput processOutput = new ProcessOutput(Processors.logTo(logger, Slf4jLevel.WARN), Processors.logTo(logger,
                Slf4jLevel.WARN), Processors.logTo(logger, Slf4jLevel.WARN));

        MongodExecutable mongodExecutable = MongodStarter
                .getInstance((new RuntimeConfigBuilder())
                        .defaults(Command.MongoD)
                        .processOutput(processOutput)
                        .build())
                .prepare(mongoConfigConfig);

        mongod = mongodExecutable.start();

        if (mongod.isProcessRunning()) {
            System.out.println("Embedded mongo RUNNING");
            logger.info("Embedded mongo RUNNING");
        }
        return mongod;
    }
}