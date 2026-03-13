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

    def "can load a data URL and read page title"() {
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

    def "tracks the current URL after navigation"() {
        given:
        def browser = HeadlessBrowser.create()

        when:
        browser.navigate("data:text/html,<title>Hello</title>")

        then:
        browser.currentUrl.startsWith("data:")

        cleanup:
        browser.close()
    }

    def "tracks last status code"() {
        given:
        def browser = HeadlessBrowser.create()

        when:
        browser.navigate("data:text/html,<title>OK</title>")

        then:
        browser.lastStatusCode == 200

        cleanup:
        browser.close()
    }

    def "can type into input fields"() {
        given:
        def browser = HeadlessBrowser.create()
        browser.navigate("data:text/html,<form><input id='name' type='text'/></form>")

        when:
        browser.typeByCss("#name", "testuser")

        then:
        browser.waitForElement("#name").getAttribute("value") == "testuser"

        cleanup:
        browser.close()
    }

    def "can type into textarea fields"() {
        given:
        def browser = HeadlessBrowser.create()
        browser.navigate("data:text/html,<form><textarea id='msg'></textarea></form>")

        when:
        browser.typeByCss("#msg", "hello world")

        then:
        browser.waitForElement("#msg").textContent == "hello world"

        cleanup:
        browser.close()
    }

    def "typeByCss throws on non-input element"() {
        given:
        def browser = HeadlessBrowser.create()
        browser.navigate("data:text/html,<div id='notinput'>text</div>")

        when:
        browser.typeByCss("#notinput", "value")

        then:
        thrown(IllegalStateException)

        cleanup:
        browser.close()
    }

    def "exposes the underlying WebClient"() {
        given:
        def browser = HeadlessBrowser.create()

        expect:
        browser.client != null

        cleanup:
        browser.close()
    }

    def "currentUrl is null before any navigation"() {
        given:
        def browser = HeadlessBrowser.create()

        expect:
        browser.currentUrl == null

        cleanup:
        browser.close()
    }
}
