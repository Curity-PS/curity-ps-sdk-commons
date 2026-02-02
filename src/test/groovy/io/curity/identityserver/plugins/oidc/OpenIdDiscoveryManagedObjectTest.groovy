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

    private OpenIdDiscoveryConfiguration validConfigurationMock(URI issuerUri) {
        return Mock(OpenIdDiscoveryConfiguration) {
            1 * getIssuer() >> issuerUri
            1 * getJson() >> new TestJson()
            1 * getHttpClient() >>
                    mockedHttpClientResponseForUri(issuerUri.resolve('.well-known/openid-configuration'),
                            discoveryMetadataMap)
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
