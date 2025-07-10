package io.curity.identityserver.plugins.oauth

import io.curity.identityserver.test.utils.TestJson
import io.curity.identityserver.test.utils.TestSessionManager
import se.curity.identityserver.sdk.attribute.Attribute
import se.curity.identityserver.sdk.config.EncryptedString
import se.curity.identityserver.sdk.http.HttpRequest
import se.curity.identityserver.sdk.http.HttpResponse
import se.curity.identityserver.sdk.service.ExceptionFactory
import se.curity.identityserver.sdk.service.HttpClient
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider
import spock.lang.Specification

import java.nio.charset.Charset

class CodeFlowClientTest extends Specification {
    static AUTHZ_ENDPOINT = "https://authz,example.com"
    static AUTHN_ENDPOINT = "https://authn,example.com"
    static TOKEN_ENDPOINT = "https://token.example.com"

    def "Test that the client creates a valid authorization URL"() {
        given: "A mocked configuration that captures the parameters in the redirect exception"
        def capturedParameters = [:]
        def mockedExceptionFactory = createMockedExceptionFactory(capturedParameters)

        def sessionManager = new TestSessionManager()
        def mockConfiguration = mockedConfiguration(scope, false, mockedExceptionFactory, sessionManager)

        def client = new CodeFlowClient(mockConfiguration,
                URI.create(AUTHZ_ENDPOINT), null, null)

        when: "Creating the redirectException"
        client.createAuthorizationUrlRedirect(additionalParameters)

        then: "Parameters added to the returned exception are a map of string/singleton lists and contain the expected values"
        capturedParameters.remove("response_type") == ["code"]
        capturedParameters.remove("redirect_uri") == ["$AUTHN_ENDPOINT/${CodeFlowClient.REDIRECT_URI_ENDPOINT_SUFFIX}"]
        capturedParameters.remove("client_id") == ["test-client"]
        capturedParameters.remove("scope") == [scope]
        capturedParameters["state"].size() == 1
        capturedParameters.remove("state").first().length() > 1

        and: "When OpenID is not used, nonce parameter is not present"
        !capturedParameters.containsKey("nonce")

        and: "Nonce is not stored to session"
        sessionManager.nonce == null

        and: "Since PKCE is not used, code_challenge and code_challenge_method are not present"
        !capturedParameters.containsKey("code_challenge")
        !capturedParameters.containsKey("code_challenge_method")

        and: "Code Verifier is not stored to session"
        sessionManager.code_verifier == null

        and: "Additional parameters are present"
        for (def entry : additionalParameters.entrySet()) {
            assert capturedParameters.remove(entry.key) == [entry.value]
        }

        and: "No other parameters are present"
        capturedParameters.isEmpty()


        where:
        scope        | additionalParameters
        "read"       | [:]
        "read write" | [:]
        ""           | [:]
        "read"       | ["foo": "bar", "baz": "zort"]
    }

    def "The client creates valid authorization URL when OpenID scope is used"() {
        given: "A mocked configuration that captures the parameters in the redirect exception"
        def capturedParameters = [:]
        def mockedExceptionFactory = createMockedExceptionFactory(capturedParameters)
        def sessionManager = new TestSessionManager()
        def mockConfiguration = mockedConfiguration(scope, false, mockedExceptionFactory, sessionManager)

        def client = new CodeFlowClient(mockConfiguration,
                URI.create(AUTHZ_ENDPOINT), null, null)

        when: "Creating the redirectException"
        client.createAuthorizationUrlRedirect([:])

        then: "Since OpenID is used, the client sends a nonce parameter"
        capturedParameters["nonce"].size() == 1
        and: "Nonce is stored to session"
        sessionManager.nonce != null

        where:
        scope << ["openid", "openid read"]
    }

    def "The client creates valid authorization URL when PKCE is used"() {
        given: "A mocked configuration that captures the parameters in the redirect exception"
        def capturedParameters = [:]
        def mockedExceptionFactory = createMockedExceptionFactory(capturedParameters)
        def sessionManager = new TestSessionManager()
        def mockConfiguration = mockedConfiguration("read", true, mockedExceptionFactory, sessionManager)

        def client = new CodeFlowClient(mockConfiguration,
                URI.create(AUTHZ_ENDPOINT), null, null)

        when: "Creating the redirectException"
        client.createAuthorizationUrlRedirect([:])

        then: "Since PKCE is used, the client sends code_challenge parameters"
        capturedParameters["code_challenge"]?.size() == 1
        capturedParameters.remove("code_challenge").first().length() == 43
        capturedParameters.remove("code_challenge_method") == ["S256"]

        and: "Code Verifier is stored to session"
        sessionManager.code_verifier != null
    }

    def "The client is able to send the correct parameters to the token endpoint when redeeming code"() {
        given: "A mocked sessionmanager that previously stored the code_verifier if pkce is used"
        def sessionManager = new TestSessionManager()

        if (usePkce) {
            and: "The session manager contains a code_verifier"
            sessionManager.put(Attribute.of("code_verifier", "test-code-verifier"))
        }

        and: "A mocked configuration"
        def mockConfiguration = mockedConfiguration("openid", usePkce, Mock(ExceptionFactory), sessionManager)

        and: "A mocked HTTP client that collects the request body parameters"
        def requestParameters = [:]
        HttpClient httpClientMock = successfulResponseMock(requestParameters)

        def client = new CodeFlowClient(mockConfiguration,
                URI.create(AUTHZ_ENDPOINT), URI.create(TOKEN_ENDPOINT), httpClientMock)

        when: "Redeeming the code"
        def tokenResponse = client.redeemCodeForTokens("my-good-code")

        then: "The token endpoint is called with the correct parameters"
        requestParameters.grant_type == "authorization_code"
        requestParameters.client_id == "test-client"
        requestParameters.redirect_uri == URLEncoder.encode("$AUTHN_ENDPOINT/${CodeFlowClient.REDIRECT_URI_ENDPOINT_SUFFIX}", Charset.defaultCharset())
        requestParameters.code == "my-good-code"

        and: "The session is empty"
        sessionManager.sessionAttributes.size() == 0

        where:
        usePkce << [true, false]
    }

    def "Test that the client can validate a state parameter against its session"() {
        given: "A mocked configuration that captures the parameters in the redirect exception"
        def capturedParameters = [:]
        def mockedExceptionFactory = createMockedExceptionFactory(capturedParameters)
        def sessionManager = new TestSessionManager()
        def mockConfiguration = mockedConfiguration("read", true, mockedExceptionFactory, sessionManager)

        def client = new CodeFlowClient(mockConfiguration,
                URI.create(AUTHZ_ENDPOINT), null, null)

        when: "Creating the redirectException"
        client.createAuthorizationUrlRedirect([:])

        then: "The state parameter is captured and can be validated against the session"
        client.validateState(capturedParameters["state"]?.first() as String)

        and: "An invalid state parameter cannot be validated"
        !client.validateState("my-invalid-state")
    }

    private ExceptionFactory createMockedExceptionFactory(Map outParameters) {
        def mockedExceptionFactory = Mock(ExceptionFactory) {
            redirectException(_, _, { params ->
                outParameters.putAll(params)
                return null
            }, _) >> new RuntimeException()
        }
        return mockedExceptionFactory
    }

    private HttpClient successfulResponseMock(Map outParameters) {
        return Mock(HttpClient) {
            1 * request(_) >> Mock(HttpRequest.Builder) {
                1 * contentType("application/x-www-form-urlencoded") >> it
                1 * body({ HttpRequest.BodyProcessor bodyProcessor ->
                    def requestBody = new String(bodyProcessor.bytes)
                    collectBodyParameters(requestBody, outParameters)
                    return true
                }) >> it
                1 * method("POST") >> Mock(HttpRequest) {
                    1 * response() >> Mock(HttpResponse) {
                        1 * statusCode() >> 200
                        1 * body(_) >> '{"access_token": "123-qwe-321-ewq"}'
                    }
                }
            }
        }
    }

    private static boolean collectBodyParameters(String requestBody, Map outParameters) {
        requestBody.split("&").each { param ->
            def parts = param.split("=")
            outParameters.put(parts[0], parts[1])
        }
        return true
    }

    private OAuthClientConfig mockedConfiguration(String scope,
                                                  boolean usePkceParameters,
                                                  ExceptionFactory factory,
                                                  TestSessionManager testSessionManager) {
        def config = Mock(OAuthClientConfig) {
            exceptionFactory() >> factory
            authenticatorInformationProvider() >> Mock(AuthenticatorInformationProvider) {
                getFullyQualifiedAuthenticationUri() >> URI.create(AUTHN_ENDPOINT)
            }
            sessionManager() >> testSessionManager
            getClientId() >> "test-client"
            getClientSecret() >> Mock(EncryptedString) {
                getValue() >> "test-client-secret"
            }
            getScope() >> scope
            usePkce() >> usePkceParameters
            json() >> new TestJson()
        }
        return config
    }

}
