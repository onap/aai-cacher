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
package org.onap.aai.cacher.dmaap.consumer;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.att.nsa.mr.client.MRClientFactory;
import com.att.nsa.mr.client.MRConsumer;
import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.util.security.Password;
import org.onap.aai.cacher.util.AAIConstants;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.logging.ErrorLogHelper;
import org.onap.aai.util.AAIConfig;

import java.io.*;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

public class AAIParentEventConsumer {

    protected String fromAppId = "AAIEventConsumerScheduledTask";
    protected String COMPONENT = "DMAAP-AAI-EVENT";
    private static EELFLogger LOGGER = EELFManager.getInstance().getLogger(AAIParentEventConsumer.class);

    protected String preferredRouterFilePath;
    protected String aaiDmaapEventConsumerPropertiesFile;

    protected String dmaapPropertyHome = "";
    protected String dmaapConusmerId = "";
    private String dmaapGroup;
    protected String transId = "";

    protected Properties aaiDmaapEventConsumerProperties = new Properties();

    protected MRConsumer aaiDmaapEventConsumer;

    protected DmaapConsumerSingleton dmaapConsumerSingleton;

    /*
     * Change the client consumer implementation from RestDmaapClientConsumer to
     * DmaapClientConsumer when the bug that is making dme2 connections in dev,
     * testINT, testEXT is fixed
     */
    protected ClientConsumer clientConsumer;

    public AAIParentEventConsumer(String consumerPropFile, boolean injestConsumer) throws Exception {
        this.transId = UUID.randomUUID().toString();
        LOGGER.debug("Initalize the AAIParentEventConsumer");

        DmaapConsumerSingleton dmaapConsumerSingleton = DmaapConsumerSingleton.getInstance();

        this.dmaapPropertyHome = AAIConstants.AAI_HOME_ETC_APP_PROPERTIES;

        if (dmaapConsumerSingleton.getDmaapConsumerId() == null) {
            dmaapConsumerSingleton.setDmaapConsumerId(UUID.randomUUID().toString());
        }
        this.dmaapConusmerId = dmaapConsumerSingleton.getDmaapConsumerId();

        if (dmaapConsumerSingleton.getDmaapGroup() == null) {
            dmaapConsumerSingleton.setDmaapGroup("cacher-" + UUID.randomUUID().toString());
        }
        this.dmaapGroup = dmaapConsumerSingleton.getDmaapGroup();

        processPropertyFiles(consumerPropFile);
        if (!injestConsumer) {
            this.aaiDmaapEventConsumer = MRClientFactory.createConsumer(this.aaiDmaapEventConsumerProperties.toString());
            setConsumer(aaiDmaapEventConsumer);
        }
        LOGGER.debug("Initialization completed.");

    }

    public void setConsumer(MRConsumer aaiDmaapEventConsumer) {
        this.aaiDmaapEventConsumer = aaiDmaapEventConsumer;
        this.clientConsumer = new RestDmaapClientConsumer(this.aaiDmaapEventConsumer,
                this.aaiDmaapEventConsumerProperties);
    }

    public Properties getDmaapEventConsumerProperties() {
        return aaiDmaapEventConsumerProperties;
    }

    private void processPropertyFiles(String consumerPropFile) throws IOException, ConfigurationException {

        this.preferredRouterFilePath = this.dmaapPropertyHome + "preferredRoute.txt";
        this.aaiDmaapEventConsumerPropertiesFile = this.dmaapPropertyHome + consumerPropFile;

        LOGGER.debug("Preferred router file path: " + this.preferredRouterFilePath);
        LOGGER.debug("AAI Dmaap Event Consumer Properties path: " + this.aaiDmaapEventConsumerPropertiesFile);

        File fo = new File(this.preferredRouterFilePath);
        if (!fo.exists()) {
            FileNotFoundException ex = new FileNotFoundException(
                    "Dmaap Route file " + preferredRouterFilePath + " does not exist");
            ErrorLogHelper.logException(new AAIException("AAI_4000", ex));
            throw ex;
        }

        fo = new File(this.aaiDmaapEventConsumerPropertiesFile);
        if (!fo.exists()) {
            FileNotFoundException ex = new FileNotFoundException(
                    "Dmaap consumer property file " + aaiDmaapEventConsumerPropertiesFile + " does not exist.");
            ErrorLogHelper.logException(new AAIException("AAI_4000", ex));
            throw ex;
        }

        modifyProperties();

    }

    private void modifyProperties() throws ConfigurationException, IOException {

        try (Reader reader = new FileReader(new File(this.aaiDmaapEventConsumerPropertiesFile))) {
            this.aaiDmaapEventConsumerProperties.load(reader);
        }


        aaiDmaapEventConsumerProperties.setProperty("id", this.dmaapConusmerId);
        aaiDmaapEventConsumerProperties.setProperty("group", this.dmaapGroup);
        LOGGER.debug("Updated " + this.aaiDmaapEventConsumerPropertiesFile + " group" + this.dmaapGroup + " id " + this.dmaapConusmerId);

        aaiDmaapEventConsumerProperties.setProperty("DME2preferredRouterFilePath", this.preferredRouterFilePath);
        if (aaiDmaapEventConsumerProperties.getProperty("password") != null
                && aaiDmaapEventConsumerProperties.getProperty("password").startsWith("OBF:")) {
            aaiDmaapEventConsumerProperties.setProperty("password",
                    Password.deobfuscate(aaiDmaapEventConsumerProperties.getProperty("password")));
        }
        LOGGER.debug("Updated " + this.aaiDmaapEventConsumerPropertiesFile + " DME2preferredRouterFilePath property to "
                + this.preferredRouterFilePath);

        if (getIsInitialCheck()) {
            aaiDmaapEventConsumerProperties.setProperty("limit", "1");
        }
        LOGGER.debug("Using limit " + aaiDmaapEventConsumerProperties.getProperty("limit"));
        LOGGER.debug("Using filter " + aaiDmaapEventConsumerProperties.getProperty("filter"));

        LOGGER.debug("Dmaap Properties = " + aaiDmaapEventConsumerProperties);
    }

    public void startProcessing(DmaapProcessor dmaapProcessor) throws Exception {
        int fetchFailCounter = 0;
        while (AAIConfig.get("aai.cacher.dmaap.consumer.enableEventProcessing").equals("true")) {
            try {
                LOGGER.debug("processEvents=" + dmaapConsumerSingleton.getProcessEvents() + " isInitialized="
                        + dmaapConsumerSingleton.getIsInitialized());
                if (dmaapConsumerSingleton.getProcessEvents() || !dmaapConsumerSingleton.getIsInitialized()) {
                    Iterable<String> eventMessages = clientConsumer.process();
                    if (dmaapConsumerSingleton.getFirstEventMessage() != null) {
                        String firstMessage = getFirstMessage();
                        if (firstMessage != null) {
                            LOGGER.debug("Processing held dmaap message from the aaiDmaapEvent topic." + transId);
                            LOGGER.debug("Processing held dmaap message from the aaiDmaapEvent topic: " + firstMessage);
                            dmaapProcessor.process(firstMessage);
                        }
                    }
                    for (String eventMessage : eventMessages) {
                        if (!dmaapConsumerSingleton.getProcessEvents()) {
                            // hold message until app is ready for dmaap processing
                            setFirstMessage(eventMessage);
                            LOGGER.debug("Holding new dmaap message from the aaiDmaapEvent topic: " + eventMessage);
                            dmaapConsumerSingleton.setIsInitialized(true);
                            continue;
                        }
                        LOGGER.debug("Processing held dmaap message from the aaiDmaapEvent topic: " + eventMessage);
                        dmaapProcessor.process(eventMessage);
                    }
                    fetchFailCounter = 0;
                } else {
                    // not processing events
                    this.aaiDmaapEventConsumer.close();
                    return;
                }
                break;
            } catch (IOException e) {
                fetchFailCounter++;
                if (fetchFailCounter > 10) {
                    ErrorLogHelper.logException(new AAIException("AAI_4000", e));
                    this.aaiDmaapEventConsumer.close();
                    throw e;
                }
                LOGGER.info("ignoring IOException, count is at." + fetchFailCounter);
            } catch (Exception e) {
                ErrorLogHelper.logException(new AAIException("AAI_4000", e));

                e.printStackTrace();
                this.aaiDmaapEventConsumer.close();
                e.printStackTrace();
                throw e;
            }
        }
        this.aaiDmaapEventConsumer.close();
    }

    /**
     * checks on processing events flag
     * 
     * @return
     */
    private boolean getIsInitialCheck() {
        dmaapConsumerSingleton = DmaapConsumerSingleton.getInstance();
        if (dmaapConsumerSingleton.getProcessEvents()) {
            return false;
        }
        return !dmaapConsumerSingleton.getIsInitialized();
    }

    /**
     * used to hold the first event message received before the app is ready to
     * process
     */

    private void setFirstMessage(String message) {
        dmaapConsumerSingleton.setFirstEventMessage(message);
    }

    /**
     * used to get the first event message being held before the app is ready to
     * process
     */

    private String getFirstMessage() {
        String message = dmaapConsumerSingleton.getFirstEventMessage();
        dmaapConsumerSingleton.setFirstEventMessage(null);
        return message;
    }

}
