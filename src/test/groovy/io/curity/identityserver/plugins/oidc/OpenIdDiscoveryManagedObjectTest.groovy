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

package io.curity.identityserver.plugins.oidc

import io.curity.identityserver.test.utils.TestJson
import se.curity.identityserver.sdk.config.Configuration
import se.curity.identityserver.sdk.http.HttpRequest
import se.curity.identityserver.sdk.http.HttpResponse
import se.curity.identityserver.sdk.service.HttpClient
import spock.lang.Specification

class OpenIdDiscoveryManagedObjectTest extends Specification {

    private static String discoveryMetadata = new File("src/test/resources/discovery-metadata.json").text
    private static Map<String, Object> discoveryMetadataMap = new TestJson().fromJson(discoveryMetadata)
    private static URI issuerUri = URI.create(discoveryMetadataMap.issuer as String)

    def "Test that the managed object is able to parse the token endpoint from  a given metadata doc"() {
        given:
        def openIdDiscoveryConfiguration = validConfigurationMock(issuerUri)
        def managedObject = new OpenIdDiscoveryManagedObject(Mock(Configuration), openIdDiscoveryConfiguration)

        when:
        def tokenEndpoint = managedObject.getTokenEndpoint()

        then:
        tokenEndpoint.toString() == discoveryMetadataMap.token_endpoint
    }

    def "Test that the managed object is able to parse the authorization endpoint from a given metadata doc"() {
        given:
        def openIdDiscoveryConfiguration = validConfigurationMock(issuerUri)
        def managedObject = new OpenIdDiscoveryManagedObject(Mock(Configuration), openIdDiscoveryConfiguration)

        when:
        def authorizationEndpoint = managedObject.getAuthorizeEndpoint()

        then:
        authorizationEndpoint.toString() == discoveryMetadataMap.authorization_endpoint
    }

    def "Test that values of different types can be retrieved from the configuration"() {
        given:
        def openIdDiscoveryConfiguration = validConfigurationMock(issuerUri)
        def managedObject = new OpenIdDiscoveryManagedObject(Mock(Configuration), openIdDiscoveryConfiguration)

        when:
        def value = managedObject.getConfigurationValueOfType(type, key)

        then:
        value == expectedValue

        where:
        key                               | type          | expectedValue
        "claims_supported"                | List.class    | ["degreeGrade"]
        "request_uri_parameter_supported" | Boolean.class | true
        "mtls_endpoint_aliases"           | Map.class     | ["token_endpoint": discoveryMetadataMap.token_endpoint]
    }

    def "Test that the managed object is able to parse the introspection endpoint from a given metadata doc"() {
        given:
        def openIdDiscoveryConfiguration = validConfigurationMock(issuerUri)
        def managedObject = new OpenIdDiscoveryManagedObject(Mock(Configuration), openIdDiscoveryConfiguration)

        when:
        def introspectionEndpoint = managedObject.getTokenIntrospectionEndpoint()

        then:
        introspectionEndpoint.toString() == discoveryMetadataMap.introspection_endpoint
    }

    def "Opaque token validator is null when created without client credentials"() {
        given:
        def openIdDiscoveryConfiguration = validConfigurationMock(issuerUri)
        def managedObject = new OpenIdDiscoveryManagedObject(Mock(Configuration), openIdDiscoveryConfiguration)

        expect:
        managedObject.getOpaqueTokenValidator() == null
    }

    def "Opaque token validator is created when client credentials are provided"() {
        given:
        def openIdDiscoveryConfiguration = validConfigurationMock(issuerUri)
        def managedObject = new OpenIdDiscoveryManagedObject(Mock(Configuration), openIdDiscoveryConfiguration,
                "my-client-id", "my-client-secret")

        expect:
        managedObject.getOpaqueTokenValidator() != null
    }

    private OpenIdDiscoveryConfiguration validConfigurationMock(URI issuerUri) {
        def httpClient = mockedHttpClientResponseForUri(
                URI.create(issuerUri.toString() + '/.well-known/openid-configuration'),
                discoveryMetadataMap)
        return Mock(OpenIdDiscoveryConfiguration) {
            getIssuer() >> issuerUri
            getJson() >> new TestJson()
            getHttpClient() >> httpClient
        }
    }

    private HttpClient mockedHttpClientResponseForUri(URI uri,
                                                      Object responseBody) {
        HttpClient httpClientMock = Mock(HttpClient) {
            1 * request(uri) >> {
                return Mock(HttpRequest.Builder) {
                    1 * get() >> {
                        return Mock(HttpRequest) {
                            1 * response() >> {
                                return Mock(HttpResponse) {
                                    1 * body(_) >> responseBody
                                }
                            }
                        }
                    }
                }
            }
        }
        return httpClientMock
    }

}