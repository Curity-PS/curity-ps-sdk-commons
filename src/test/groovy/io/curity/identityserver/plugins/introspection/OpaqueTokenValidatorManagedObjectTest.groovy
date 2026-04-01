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

package io.curity.identityserver.plugins.introspection

import io.curity.identityserver.test.utils.TestJson
import se.curity.identityserver.sdk.config.EncryptedString
import se.curity.identityserver.sdk.http.HttpRequest
import se.curity.identityserver.sdk.http.HttpResponse
import se.curity.identityserver.sdk.service.HttpClient
import spock.lang.Specification

class OpaqueTokenValidatorManagedObjectTest extends Specification {

    static def activeIntrospectionResponse = '{"active": true, "sub": "johndoe", "scope": "read write", ' +
            '"client_id": "my-client", "iss": "https://issuer.example.com", "aud": "api.example.com", ' +
            '"token_type": "bearer"}'
    static def inactiveIntrospectionResponse = '{"active": false}'
    static token = "_0XBPWQQ_788ebf40-68fe-4cc8-af18-df7d2a588431"

    def "Should validate active token via introspection"() {
        given:
        def config = mockedConfiguration(mockedIntrospectionResponse(activeIntrospectionResponse))
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        def attributes = validator.validate(token)

        then:
        attributes.subject != null
    }

    def "Should return correct claims from introspection response"() {
        given:
        def httpClient = mockedIntrospectionResponse(activeIntrospectionResponse)
        def config = mockedConfiguration(httpClient)
        def client = new AccessTokenValidator(
                config.getIntrospectionEndpoint(),
                httpClient,
                config.getJson(),
                config.getClientId(),
                config.getClientSecret().getValue()
        )

        when:
        def attributes = client.validateToken(token, "https://issuer.example.com", "api.example.com", ["read", "write"])

        then:
        attributes.isActive()
        attributes.subject == "johndoe"
        attributes.scope == "read write"
        attributes.clientId == "my-client"
        attributes.issuer == "https://issuer.example.com"
        attributes.audiences == ["api.example.com"]
        attributes.tokenType == "bearer"
    }

    def "Should throw IntrospectionException when token is inactive"() {
        given:
        def config = mockedConfiguration(mockedIntrospectionResponse(inactiveIntrospectionResponse))
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate("inactive-token")

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message == "Token is not active"
    }

    def "Should throw IntrospectionException when endpoint returns error status"() {
        given:
        def config = mockedConfiguration(mockedIntrospectionResponse('{"error": "unauthorized"}', 401))
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate("bad-token")

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message.contains("401")
    }

    def "Should throw IntrospectionException when issuer does not match"() {
        given:
        def config = mockedConfiguration(mockedIntrospectionResponse(activeIntrospectionResponse), "api.example.com", "https://wrong-issuer.example.com")
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate(token)

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message.contains("Issuer mismatch")
    }

    def "Should throw IntrospectionException when audience does not match"() {
        given:
        def config = mockedConfiguration(mockedIntrospectionResponse(activeIntrospectionResponse), "wrong-audience", "https://issuer.example.com")
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate(token)

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message.contains("Audience mismatch")
    }

    def "Should throw IntrospectionException when issuer is missing from introspection response"() {
        given:
        def responseWithoutIssuer = '{"active": true, "sub": "johndoe", "aud": "api.example.com"}'
        def config = mockedConfiguration(mockedIntrospectionResponse(responseWithoutIssuer))
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate(token)

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message.contains("missing required 'iss' claim")
    }

    def "Should throw IntrospectionException when audience is missing from introspection response"() {
        given:
        def responseWithoutAudience = '{"active": true, "sub": "johndoe", "iss": "https://issuer.example.com"}'
        def config = mockedConfiguration(mockedIntrospectionResponse(responseWithoutAudience))
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate(token)

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message.contains("missing required 'aud' claim")
    }

    def "Should throw IntrospectionException when required scopes are missing"() {
        given:
        def config = mockedConfiguration(mockedIntrospectionResponse(activeIntrospectionResponse),
                "api.example.com", "https://issuer.example.com", ["read", "admin"])
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate(token)

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message.contains("Scope mismatch")
        thrown.message.contains("admin")
    }

    def "Should throw IntrospectionException when scope claim is missing from introspection response"() {
        given:
        def responseWithoutScope = '{"active": true, "sub": "johndoe", "iss": "https://issuer.example.com", "aud": "api.example.com"}'
        def config = mockedConfiguration(mockedIntrospectionResponse(responseWithoutScope),
                "api.example.com", "https://issuer.example.com", ["read"])
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate(token)

        then:
        def thrown = thrown(IntrospectionException)
        thrown.message.contains("missing required 'scope' claim")
    }

    def "Should pass validation when expected scopes list is empty"() {
        given:
        def config = mockedConfiguration(mockedIntrospectionResponse(activeIntrospectionResponse),
                "api.example.com", "https://issuer.example.com", [])
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        def attributes = validator.validate(token)

        then:
        attributes.subject != null
    }

    def "Should send correct request format to introspection endpoint"() {
        given:
        def expectedBasicAuth = "Basic " + Base64.encoder.encodeToString("test-client:test-secret".bytes)
        def requestParameters = [:]
        HttpClient httpClient = Mock(HttpClient) {
            1 * request(URI.create("https://example.com/introspect")) >> Mock(HttpRequest.Builder) {
                1 * contentType("application/x-www-form-urlencoded") >> it
                1 * header("Authorization", expectedBasicAuth) >> it
                1 * body({ HttpRequest.BodyProcessor bodyProcessor ->
                    def requestBody = new String(bodyProcessor.bytes)
                    requestBody.split("&").each { param ->
                        def parts = param.split("=", 2)
                        requestParameters.put(parts[0], parts[1])
                    }
                    return true
                }) >> it
                1 * method("POST") >> Mock(HttpRequest) {
                    1 * response() >> Mock(HttpResponse) {
                        1 * statusCode() >> 200
                        1 * body(_) >> activeIntrospectionResponse
                    }
                }
            }
        }

        def config = mockedConfiguration(httpClient)
        def validator = new OpaqueTokenValidatorManagedObject(config)

        when:
        validator.validate(token)

        then:
        requestParameters.token == token
    }

    private OpaqueTokenValidatorConfiguration mockedConfiguration(HttpClient httpClient,
                                                                  String audience = "api.example.com",
                                                                  String issuer = "https://issuer.example.com",
                                                                  List<String> scopes = ["read", "write"]) {
        return Mock(OpaqueTokenValidatorConfiguration) {
            getIntrospectionEndpoint() >> URI.create("https://example.com/introspect")
            getClientId() >> "test-client"
            getClientSecret() >> Mock(EncryptedString) {
                getValue() >> "test-secret"
            }
            getExpectedAudience() >> audience
            getExpectedIssuer() >> issuer
            getExpectedScopes() >> scopes
            getHttpClient() >> httpClient
            getJson() >> new TestJson()
        }
    }

    private HttpClient mockedIntrospectionResponse(String responseBody, int expectedStatusCode = 200) {
        def httpResponse = Stub(HttpResponse) {
            statusCode() >> expectedStatusCode
            body(_) >> responseBody
        }
        def httpRequest = Stub(HttpRequest) {
            response() >> httpResponse
        }
        def builder = Stub(HttpRequest.Builder) {
            contentType(_) >> it
            header(_, _) >> it
            body(_) >> it
            method("POST") >> httpRequest
        }
        return Stub(HttpClient) {
            request(_) >> builder
        }
    }
}
