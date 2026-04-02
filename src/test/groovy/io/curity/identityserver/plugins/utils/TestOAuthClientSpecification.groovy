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
import io.curity.identityserver.test.utils.TestOAuthClient
import spock.lang.Specification

class TestOAuthClientSpecification extends Specification {

    def "defaultClient creates a client with default credentials"() {
        given:
        def browser = HeadlessBrowser.create()

        when:
        def client = TestOAuthClient.defaultClient(browser, "https://example.com/authorize", "https://example.com/token")

        then:
        client.browser == browser
        !client.flowComplete
        client.error == null

        cleanup:
        client?.close()
    }

    def "redirect interceptor captures authorization code from redirect URI"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "https://example.com/authorize", "https://example.com/token")

        when: "The browser's WebClient hits a URL matching the redirect URI with a code"
        browser.client.getPage("${TestOAuthClient.DEFAULT_REDIRECT_URI}?code=test-auth-code")

        then: "The flow is marked as complete"
        client.flowComplete
        client.error == null

        cleanup:
        client?.close()
    }

    def "redirect interceptor captures error from redirect URI"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "https://example.com/authorize", "https://example.com/token")

        when: "The browser's WebClient hits the redirect URI with an error"
        browser.client.getPage("${TestOAuthClient.DEFAULT_REDIRECT_URI}?error=access_denied")

        then: "The flow is complete with an error"
        client.flowComplete
        client.error == "access_denied"

        cleanup:
        client?.close()
    }

    def "exchangeCode throws when no code has been captured"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "https://example.com/authorize", "https://example.com/token")

        when:
        client.exchangeCode()

        then:
        thrown(IllegalStateException)

        cleanup:
        client?.close()
    }

    def "codeFlow resets state from a previous flow"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "data:text/html,<title>Auth</title>", "https://example.com/token")

        and: "A previous redirect has been captured"
        browser.client.getPage("${TestOAuthClient.DEFAULT_REDIRECT_URI}?code=old-code")
        assert client.flowComplete

        when: "A new code flow is started"
        client.codeFlow()

        then: "The previous state is cleared"
        !client.flowComplete
        client.error == null

        cleanup:
        client?.close()
    }

    def "reset clears captured state"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "https://example.com/authorize", "https://example.com/token")

        and: "A redirect has been captured"
        browser.client.getPage("${TestOAuthClient.DEFAULT_REDIRECT_URI}?error=access_denied")
        assert client.flowComplete
        assert client.error == "access_denied"

        when:
        client.reset()

        then:
        !client.flowComplete
        client.error == null

        cleanup:
        client?.close()
    }

    def "reset allows reuse after a completed flow"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "data:text/html,<title>Auth</title>", "https://example.com/token")

        and: "A first flow completes"
        browser.client.getPage("${TestOAuthClient.DEFAULT_REDIRECT_URI}?code=first-code")
        assert client.flowComplete

        when: "Reset and simulate a second flow"
        client.reset()

        then: "State is clean"
        !client.flowComplete
        client.error == null

        when: "A second redirect is captured"
        browser.client.getPage("${TestOAuthClient.DEFAULT_REDIRECT_URI}?code=second-code")

        then: "The new flow completes"
        client.flowComplete

        cleanup:
        client?.close()
    }

    def "codeFlow passes extra parameters to the authorization request"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "data:text/html,<title>Auth</title>", "https://example.com/token")
        def extraParams = [login_hint: "user@example.com", prompt: "login"]

        when:
        client.codeFlow(null, null, extraParams)

        then: "The browser navigated to a URL containing the extra parameters"
        def url = browser.currentUrl
        url.contains("login_hint=user%40example.com")
        url.contains("prompt=login")

        cleanup:
        client?.close()
    }

    def "codeFlow works without extra parameters"() {
        given:
        def browser = HeadlessBrowser.create()
        def client = TestOAuthClient.defaultClient(browser, "data:text/html,<title>Auth</title>", "https://example.com/token")

        when:
        client.codeFlow()

        then: "The browser navigated to the authorize URL with standard parameters"
        def url = browser.currentUrl
        url.contains("client_id=")
        url.contains("response_type=code")

        cleanup:
        client?.close()
    }

    def "builder validates required fields"() {
        when: "Building without clientId"
        new TestOAuthClient.Builder()
                .clientSecret("secret")
                .authorizeEndpointUrl("https://example.com/authorize")
                .tokenEndpointUrl("https://example.com/token")
                .build()

        then:
        thrown(AssertionError)
    }

    def "builder validates required authorizeEndpointUrl"() {
        when:
        new TestOAuthClient.Builder()
                .clientId("client")
                .clientSecret("secret")
                .tokenEndpointUrl("https://example.com/token")
                .build()

        then:
        thrown(AssertionError)
    }

    def "builder validates required tokenEndpointUrl"() {
        when:
        new TestOAuthClient.Builder()
                .clientId("client")
                .clientSecret("secret")
                .authorizeEndpointUrl("https://example.com/authorize")
                .build()

        then:
        thrown(AssertionError)
    }

    def "TokenResponse parses JSON correctly"() {
        given:
        def json = '{"access_token":"at","refresh_token":"rt","id_token":"idt","token_type":"bearer","expires_in":300,"scope":"openid"}'

        when:
        def response = TestOAuthClient.TokenResponse.fromJson(json)

        then:
        response.accessToken == "at"
        response.refreshToken == "rt"
        response.idToken == "idt"
        response.tokenType == "bearer"
        response.expiresIn == 300
        response.scope == "openid"
        response.rawResponse != null
    }

    def "TokenResponse handles missing optional fields"() {
        given:
        def json = '{"access_token":"at","token_type":"bearer"}'

        when:
        def response = TestOAuthClient.TokenResponse.fromJson(json)

        then:
        response.accessToken == "at"
        response.tokenType == "bearer"
        response.refreshToken == null
        response.idToken == null
        response.expiresIn == null
        response.scope == null
    }
}
