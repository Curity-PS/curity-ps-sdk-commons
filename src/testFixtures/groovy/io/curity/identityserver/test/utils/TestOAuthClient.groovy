/*
 *  Copyright 2026 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.test.utils

import io.curity.identityserver.test.utils.constants.TestConstants
import io.curity.identityserver.test.utils.crypto.InsecureSslContext
import org.htmlunit.WebRequest
import org.htmlunit.WebResponse
import org.htmlunit.WebResponseData
import org.htmlunit.util.NameValuePair
import org.htmlunit.util.WebConnectionWrapper
import org.jose4j.json.JsonUtil
import org.slf4j.LoggerFactory

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
/**
 * A test OAuth client that uses a {@link HeadlessBrowser} to drive the
 * authorization code flow against a Curity Identity Server.
 *
 * <p>The client navigates to the authorization endpoint in the headless browser,
 * follows redirects to the authentication page, and intercepts the final redirect
 * to the {@code redirect_uri} to capture the authorization code. The code is then
 * exchanged for tokens via a direct HTTP POST.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * def client = TestOAuthClient.defaultClient(browser, authorizeUrl, tokenUrl)
 *
 * // Start the code flow – browser follows redirects to the authentication page
 * client.codeFlow()
 * assert client.browser.currentUrl.contains("/authn/authentication/")
 *
 * // Interact with the authentication page
 * client.browser.typeByCss("#userName", "testuser")
 * client.browser.typeByCss("#password", "secret")
 * client.browser.clickByCss("#login-btn")
 *
 * // Exchange the captured authorization code for tokens
 * def tokens = client.exchangeCode()
 * assert tokens.accessToken != null
 * </pre>
 */
class TestOAuthClient implements Closeable {

    private static final def logger = LoggerFactory.getLogger(TestOAuthClient.class)
    /** @deprecated Use {@link io.curity.identityserver.test.utils.constants.TestConstants.CodeFlow#REDIRECT_URI} */
    @Deprecated
    public static final String DEFAULT_REDIRECT_URI = TestConstants.CodeFlow.REDIRECT_URI
    /** @deprecated Use {@link io.curity.identityserver.test.utils.constants.TestConstants.CodeFlow#CLIENT_ID} */
    @Deprecated
    public static final String DEFAULT_CLIENT_ID = TestConstants.CodeFlow.CLIENT_ID
    /** @deprecated Use {@link io.curity.identityserver.test.utils.constants.TestConstants.CodeFlow#CLIENT_SECRET} */
    @Deprecated
    public static final String DEFAULT_CLIENT_SECRET = TestConstants.CodeFlow.CLIENT_SECRET
    private static final String DEFAULT_SCOPE = ""

    private final HeadlessBrowser browser
    private final String clientId
    private final String clientSecret
    private final String scope
    private final String authorizeEndpointUrl
    private final String tokenEndpointUrl
    private final String redirectUri
    private final String acrValues
    private final HttpClient httpClient
    private String capturedCode
    private String capturedError
    private boolean flowComplete

    private TestOAuthClient(HeadlessBrowser browser, String clientId, String clientSecret,
                            String scope, String authorizeEndpointUrl, String tokenEndpointUrl,
                            String redirectUri, String acrValues) {
        this.browser = browser
        this.clientId = clientId
        this.clientSecret = clientSecret
        this.scope = scope
        this.authorizeEndpointUrl = authorizeEndpointUrl
        this.tokenEndpointUrl = tokenEndpointUrl
        this.redirectUri = redirectUri
        this.acrValues = acrValues

        this.httpClient = HttpClient.newBuilder()
                .sslContext(InsecureSslContext.instance)
                .build()

        if (browser != null) {
            installRedirectInterceptor()
        }
    }

    private void installRedirectInterceptor() {
        def wrapper = new WebConnectionWrapper(browser.client) {
            @Override
            WebResponse getResponse(WebRequest request) throws IOException {
                def url = request.url.toString()
                if (url.startsWith(redirectUri)) {
                    capturedCode = extractQueryParam(url, "code")
                    capturedError = extractQueryParam(url, "error")
                    flowComplete = true
                    logger.info("Flow complete – redirected to redirect_uri (code={}, error={})",
                            capturedCode != null, capturedError)
                    def responseData = new WebResponseData(
                            "OK".getBytes(StandardCharsets.UTF_8),
                            200, "OK", Collections.<NameValuePair> emptyList()
                    )
                    return new WebResponse(responseData, request, 0)
                }
                return super.getResponse(request)
            }
        }
        browser.client.setWebConnection(wrapper)
    }

    /**
     * Create a TestOAuthClient with default client credentials and scope.
     * Uses client ID {@code integration-test-client}, client secret
     * {@code integration-test-secret}, and empty scope.
     *
     * @param authorizeEndpointUrl the authorization endpoint URL
     * @param tokenEndpointUrl the token endpoint URL
     * @param acrValues optional acr_values parameter for the authorization request
     * @param scope optional scope parameter (defaults to empty)
     * @return a new TestOAuthClient configured with defaults
     */
    static TestOAuthClient defaultClient(HeadlessBrowser browser, String authorizeEndpointUrl, String tokenEndpointUrl, String acrValues = null, String scope = DEFAULT_SCOPE) {
        def builder = new Builder()
                .browser(browser)
                .clientId(DEFAULT_CLIENT_ID)
                .clientSecret(DEFAULT_CLIENT_SECRET)
                .scope(scope)
                .authorizeEndpointUrl(authorizeEndpointUrl)
                .tokenEndpointUrl(tokenEndpointUrl)
        if (acrValues) {
            builder.acrValues(acrValues)
        }
        return builder.build()
    }

    /**
     * Create a TestOAuthClient for the client credentials flow (no browser needed).
     *
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param tokenEndpointUrl the token endpoint URL
     * @param scope optional scope parameter (defaults to empty)
     * @return a new TestOAuthClient configured for client credentials
     */
    static TestOAuthClient clientCredentialsClient(String clientId, String clientSecret, String tokenEndpointUrl, String scope = DEFAULT_SCOPE) {
        return new TestOAuthClient(null, clientId, clientSecret, scope,
                null, tokenEndpointUrl, null, null)
    }

    /**
     * Whether the flow has completed, i.e. the server redirected back to the redirect URI.
     * When {@code true}, either {@code code} or {@code error} will be present.
     */
    boolean isFlowComplete() {
        return flowComplete
    }

    /**
     * The OAuth error parameter from the redirect URI, if the flow ended with an error.
     */
    String getError() {
        return capturedError
    }

    /**
     * Get the embedded headless browser instance.
     */
    HeadlessBrowser getBrowser() {
        return browser
    }

    /**
     * Start the authorization code flow.
     *
     * <p>Navigates to the authorization endpoint. The browser follows redirects
     * to the authentication page. After this call returns, inspect
     * {@code browser.currentUrl} and {@code browser.lastStatusCode} to verify
     * the authentication page was reached, then interact with the browser to
     * complete authentication. Once the server redirects to the redirect URI,
     * the authorization code is captured automatically. Call {@link #exchangeCode()}
     * to exchange it for tokens.</p>
     *
     * @param scope optional scope to use for this request (defaults to the client's configured scope)
     * @param acrValues optional acr_values to use for this request (defaults to the client's configured acr_values)
     */
    void codeFlow(String scope = this.scope, String acrValues = this.acrValues) {
        capturedCode = null
        capturedError = null
        flowComplete = false
        browser.startCodeFlow(authorizeEndpointUrl, clientId, scope, redirectUri, acrValues)
    }

    /**
     * Exchange the captured authorization code for tokens.
     *
     * <p>Call this after {@link #codeFlow()} and completing the authentication
     * interaction in the browser. The redirect to {@code redirect_uri} must have
     * occurred (the code is captured automatically by the redirect interceptor).</p>
     *
     * @return the token response containing access token and other fields
     * @throws IllegalStateException if no authorization code has been captured yet
     */
    TokenResponse exchangeCode() {
        if (capturedCode == null) {
            throw new IllegalStateException(
                    "Authorization code was not captured. The authentication flow did not " +
                            "result in a redirect to ${redirectUri}. Current URL: ${browser.currentUrl}"
            )
        }
        return doTokenExchange(capturedCode)
    }

    /**
     * Execute the client credentials grant.
     *
     * @param scope optional scope to use for this request (defaults to the client's configured scope)
     * @return the token response containing access token and other fields
     */
    TokenResponse clientCredentials(String scope = this.scope) {
        logger.info("Executing client credentials grant at {}", tokenEndpointUrl)

        def body = "grant_type=client_credentials"
        if (scope) {
            body += "&scope=${URLEncoder.encode(scope, StandardCharsets.UTF_8)}"
        }

        def credentials = Base64.encoder.encodeToString(
                "${clientId}:${clientSecret}".getBytes(StandardCharsets.UTF_8)
        )

        def request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpointUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic ${credentials}")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Client credentials grant failed with status ${response.statusCode()}: ${response.body()}"
            )
        }

        return TokenResponse.fromJson(response.body())
    }

    private TokenResponse doTokenExchange(String code) {
        logger.info("Exchanging authorization code for tokens at {}", tokenEndpointUrl)

        def body = "grant_type=authorization_code" +
                "&code=${URLEncoder.encode(code, StandardCharsets.UTF_8)}" +
                "&redirect_uri=${URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)}"

        def credentials = Base64.encoder.encodeToString(
                "${clientId}:${clientSecret}".getBytes(StandardCharsets.UTF_8)
        )

        def request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpointUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic ${credentials}")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Token exchange failed with status ${response.statusCode()}: ${response.body()}"
            )
        }

        return TokenResponse.fromJson(response.body())
    }

    /**
     * Introspect an access token.
     *
     * @param accessToken the access token to introspect
     * @return the introspection response as a map of claims
     */
    Map<String, Object> introspect(String accessToken) {
        def introspectUrl = tokenEndpointUrl.replace("/oauth-token", "/oauth-introspect")
        logger.info("Introspecting token at {}", introspectUrl)

        def body = "token=${URLEncoder.encode(accessToken, StandardCharsets.UTF_8)}"

        def credentials = Base64.encoder.encodeToString(
                "${clientId}:${clientSecret}".getBytes(StandardCharsets.UTF_8)
        )

        def request = HttpRequest.newBuilder()
                .uri(URI.create(introspectUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic ${credentials}")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Introspection failed with status ${response.statusCode()}: ${response.body()}"
            )
        }

        return JsonUtil.parseJson(response.body())
    }

    private static String extractQueryParam(String url, String param) {
        def uri = new URI(url)
        def query = uri.query
        if (query == null) return null
        return query.split("&").collect { it.split("=", 2) }
                .find { it[0] == param }
                ?.with { it.length > 1 ? URLDecoder.decode(it[1], StandardCharsets.UTF_8) : null }
    }

    @Override
    void close() {
        browser?.close()
        httpClient?.close()
    }

    /**
     * Token response from the authorization server.
     */
    static class TokenResponse {
        final String accessToken
        final String refreshToken
        final String idToken
        final String tokenType
        final Integer expiresIn
        final String scope
        final Map<String, Object> rawResponse

        private TokenResponse(Map<String, Object> json) {
            this.rawResponse = json
            this.accessToken = json.get("access_token") as String
            this.refreshToken = json.get("refresh_token") as String
            this.idToken = json.get("id_token") as String
            this.tokenType = json.get("token_type") as String
            this.expiresIn = json.get("expires_in") as Integer
            this.scope = json.get("scope") as String
        }

        static TokenResponse fromJson(String json) {
            def parsed = JsonUtil.parseJson(json)
            return new TokenResponse(parsed)
        }
    }

    /**
     * Builder for {@link TestOAuthClient}.
     */
    static class Builder {
        private String _clientId
        private String _clientSecret
        private String _scope
        private String _authorizeEndpointUrl
        private String _tokenEndpointUrl
        private String _redirectUri = DEFAULT_REDIRECT_URI
        private String _acrValues
        private HeadlessBrowser _browser

        Builder clientId(String clientId) {
            _clientId = clientId
            return this
        }

        Builder clientSecret(String clientSecret) {
            _clientSecret = clientSecret
            return this
        }

        Builder scope(String scope) {
            _scope = scope
            return this
        }

        Builder authorizeEndpointUrl(String url) {
            _authorizeEndpointUrl = url
            return this
        }

        Builder tokenEndpointUrl(String url) {
            _tokenEndpointUrl = url
            return this
        }

        Builder redirectUri(String uri) {
            _redirectUri = uri
            return this
        }

        Builder acrValues(String acrValues) {
            _acrValues = acrValues
            return this
        }

        Builder browser(HeadlessBrowser browser) {
            _browser = browser
            return this
        }

        TestOAuthClient build() {
            assert _clientId: "clientId is required"
            assert _clientSecret: "clientSecret is required"
            assert _authorizeEndpointUrl: "authorizeEndpointUrl is required"
            assert _tokenEndpointUrl: "tokenEndpointUrl is required"

            def browser = _browser ?: HeadlessBrowser.create()
            return new TestOAuthClient(browser, _clientId, _clientSecret, _scope,
                    _authorizeEndpointUrl, _tokenEndpointUrl, _redirectUri, _acrValues)
        }
    }
}
