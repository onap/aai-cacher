/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2020 Bell Intellectual Property. All rights reserved.
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
package org.onap.aai.cacher.exceptions;

public class MissingTemplateException extends RuntimeException {

    public static final String DEFAULT_EXCEPTION_MESSAGE = "Failed in uriToTemplates for truncatedUri '%s'";

    public MissingTemplateException(String uri) {
        super(String.format(DEFAULT_EXCEPTION_MESSAGE, uri));
    }
}
