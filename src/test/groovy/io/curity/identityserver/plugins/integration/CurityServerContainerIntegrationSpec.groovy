/*
 * Copyright 2026 Curity AB
 *
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
 */

package io.curity.identityserver.plugins.integration

import io.curity.identityserver.test.utils.CurityServerContainer
import spock.lang.Shared
import spock.lang.Specification

abstract class CurityServerContainerIntegrationSpec extends Specification {
    @Shared
    CurityServerContainer container

    def setupSpec() {
        container = CurityServerContainer.withVersion("11.1")
        container.start()
    }

    def "A Curity container can be started with base config for integration test purposes"() {
        expect: "It is running"
        container.isRunning()
    }

    def cleanupSpec() {
        container?.close()
    }
}
