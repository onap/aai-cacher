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
package org.onap.aai.cacher.service.tasks;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import org.onap.aai.cacher.service.helper.CacheHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Date;

@Configuration
public class ScheduledTaskConfig {

    private static final EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(ScheduledTaskConfig.class);
    private static final int THREAD_POOL_SIZE = 10;
    private static final String THREAD_POOL_PREFIX = "poolScheduler";
    private static final int TASK_INTERVAL_TIME = 30000;
    private static final int TASK_DELAY_TIME = 0;

    @Configuration
    static class RegisterTaskSchedulerViaSchedulingConfigurer implements SchedulingConfigurer {

        @Autowired
        protected CacheHelperService chs;

        @Override
        public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
            taskRegistrar.setTaskScheduler(poolScheduler());
            taskRegistrar.addFixedRateTask(new IntervalTask(() -> {
                EELF_LOGGER.info(
                        "Job @ fixed rate " + new Date() + ", Thread name is " + Thread.currentThread().getName());
                chs.checkAndInitTasks();
            }, TASK_INTERVAL_TIME, TASK_DELAY_TIME));
        }

        @Bean
        public TaskScheduler poolScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setThreadNamePrefix(THREAD_POOL_PREFIX);
            scheduler.setPoolSize(THREAD_POOL_SIZE);
            return scheduler;
        }
    }
}
