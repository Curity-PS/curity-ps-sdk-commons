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

package io.curity.identityserver.plugins.utils

import io.curity.identityserver.test.utils.HeadlessBrowser
import spock.lang.Specification

class HeadlessBrowserSpecification extends Specification {

    def "headless browser can load a data URL"() {
        given:
        def browser = HeadlessBrowser.create()

        when:
        browser.navigate("data:text/html,<title>Test Page</title><h1>Login</h1>")

        then:
        browser.waitForTitle("Test Page")
        browser.textByCss("h1") == "Login"

        cleanup:
        browser.close()
    }
}
