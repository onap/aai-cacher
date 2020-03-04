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

package org.onap.aai.cacher;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.onap.aai.cacher.config.PropertyPasswordConfiguration;
import org.onap.aai.logging.LoggingContext;
import org.onap.aai.logging.LoggingContext.StatusCode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import javax.annotation.PreDestroy;
import java.util.UUID;

@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@ComponentScan(basePackages = { "org.onap.aai.cacher", "com" })
public class Application extends SpringBootServletInitializer {

    private static final String APP_NAME = "cacher";
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(Application.class.getName());


    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) throws Exception {
        setDefaultProps();

        LoggingContext.save();
        LoggingContext.component("init");
        LoggingContext.partnerName("NA");
        LoggingContext.targetEntity(APP_NAME);
        LoggingContext.requestId(UUID.randomUUID().toString());
        LoggingContext.serviceName(APP_NAME);
        LoggingContext.targetServiceName("contextInitialized");
        LoggingContext.statusCode(StatusCode.COMPLETE);

        SpringApplication app = new SpringApplication(Application.class);
        app.setLogStartupInfo(false);
        app.setRegisterShutdownHook(true);
        app.addInitializers(new PropertyPasswordConfiguration());
        app.run(args);

        logger.info("Cacher MicroService Started");
        logger.error("Cacher MicroService Started");
        logger.debug("Cacher MicroService Started");

        System.out.println("Cacher MicroService Started");

    }

    @PreDestroy
    public void cleanup(){
        logger.info("Cacher MicroService shutting down.");
        logger.error("Cacher MicroService shutting down.");
        logger.debug("Cacher MicroService shutting down.");

        System.out.println("Cacher MicroService shutting down.");
    }

    public static void setDefaultProps() {

        if (System.getProperty("file.separator") == null) {
            System.setProperty("file.separator", "/");
        }

        if (System.getProperty("AJSC_HOME") == null) {
            System.setProperty("AJSC_HOME", ".");
        }

        if (System.getProperty("BUNDLECONFIG_DIR") == null) {
            System.setProperty("BUNDLECONFIG_DIR", "src/main/resources");
        }
    }
}