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

public class DmaapConsumerSingleton {

    private boolean processEvents;
    private boolean isInitialized;

    private String dmaapConsumerId;
    private String dmaapGroup;


    private String firstEventMessage;

    private static class Helper {
        private static final DmaapConsumerSingleton INSTANCE = new DmaapConsumerSingleton();
    }

    public static DmaapConsumerSingleton getInstance() {
        return Helper.INSTANCE;
    }

    private DmaapConsumerSingleton() {
        processEvents = false;
        isInitialized = false;
        firstEventMessage = null;
        dmaapConsumerId = null;
    }

    public void setProcessEvents(boolean processEvents) {
        this.processEvents = processEvents;
    }

    public boolean getProcessEvents() {
        return processEvents;
    }

    public void setIsInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    public boolean getIsInitialized() {
        return isInitialized;
    }

    public void setFirstEventMessage(String firstEventMessage) {
        this.firstEventMessage = firstEventMessage;
    }

    public String getFirstEventMessage() {
        return firstEventMessage;
    }

    public void setDmaapConsumerId(String dmaapConsumerId) {
        this.dmaapConsumerId = dmaapConsumerId;
    }
    public String getDmaapGroup() {
        return dmaapGroup;
    }

    public void setDmaapGroup(String dmaapGroup) {
        this.dmaapGroup = dmaapGroup;
    }

    public String getDmaapConsumerId() {
        return dmaapConsumerId;
    }
}
