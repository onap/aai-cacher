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

package org.onap.aai.cacher.injestion.parser.strategy.aai.dmaap;

import org.onap.aai.cacher.model.DBAction;

public enum DmaapAction {
        DELETE(DBAction.DELETE),
        UPDATE(DBAction.UPDATE),
        CREATE(DBAction.INSERT_REPLACE);

        private final DBAction dbAction;
        DmaapAction(DBAction dbAction) {
            this.dbAction = dbAction;
        }

        public DBAction getDbAction() {
            return dbAction;
        }
    }