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
package org.onap.aai.cacher.egestion.printer.strategy;

import java.util.Arrays;

public enum PayloadPrinterType {

    NONE_PRINTER("none-printer"), AAI_RESOURCE_GET_ALL_PRINTER("aai-resource-get-all-printer");

    private final String value;

    PayloadPrinterType(String input) {
        this.value = input;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static PayloadPrinterType fromString(String text) {
        return Arrays.stream(values()).filter(bl -> bl.getValue().equalsIgnoreCase(text)).findFirst().orElse(null);
    }
}
