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

package io.curity.identityserver.plugins.jwt

import io.curity.identityserver.plugins.jwt.JwtValidatorConfiguration.KeyResolverConfiguration
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.lang.UnresolvableKeyException
import se.curity.identityserver.sdk.http.HttpHeaders
import se.curity.identityserver.sdk.http.HttpRequest
import se.curity.identityserver.sdk.http.HttpResponse
import se.curity.identityserver.sdk.service.HttpClient
import se.curity.identityserver.sdk.service.crypto.AsymmetricSignatureVerificationCryptoStore
import spock.lang.Specification

import java.security.PublicKey

import static io.curity.identityserver.plugins.jwt.JwtValidatorConfiguration.JwksUriKeyResolverConfiguration

class JwtValidatorManagedObjectTest extends Specification {

    static def publicKeyJwk = '''{
        "alg":"RS256",
        "e":"AQAB",
        "ext":true,
        "key_ops":["verify"],
        "kty":"RSA",
        "n":"jM0wZTUudbv2pqurB2E_sNNNPiUw0eJ59ElWidq63Srbp45gD39bt3_DRaBMRrWTNfPX-nhBfxYIiOF3YmIEYgn9GcOgjURsav
            JM4f0Pl1gTjnJUoCdjBr5ZvzuSCV1VSS-8G6Wlw6YL_dRB6Zatmd5c1gnoY-zzwO0u4C0AyasGNpeFdxE8k2IauMspFwBKAmWs2
            XMzyBV8PyfN1PIrdJPK11oWFBR5RY_szymtvQiCfL2Gv9ZeRvuXGc6mJHPrAhyJV-3ZJZQdHBeX38AninnPah9WTU-bOpiD4ihO
            C1RM_L8dnvk-wxZWa-fUI7ww9oTvgIufyfsQ5XD9hH_HUj8ekADPsRKHNX_Y6zm-tzJNscXMK8y3hfQ8LNvpmpGcs8j2lLjNP6C
            G_rOKHkXe9MYimixZU1SbeXuBeT3-a8wlnMbY3D_9Xb9AUtMQsFjMvG09-rKKST6MGNlZNzaxS_xnKphgTQkpU5O_0KcNlixp4s
            aAWQyJ17q6U_ZnpSS7GN0qMJuzSCZ9OTRH4HW9tSycMAP3socllJNNxvSQNtQ_6b3xIvWRbzan0pCbktET8fcmsaoY2BLMqUFUA
            6aUL55YokYjBb0z7HaSqhus3EKAocttgqON_vuCz-I-UQb47WaxYkiqwnL91gfH9ZXSeQ8X8O8zvTzvoKRHTGh2LWs"
    }
    '''

    static def jwksJson = "{\"keys\": [ $publicKeyJwk ]}"

    static def validJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzY29wZSI6InJlYWQiLCJhdWQiOiJ0ZXN0LWF1ZGllbmNlIiw" +
            "iaXNzIjoidGVzdC1pc3N1ZXIiLCJzdWIiOiJqb2huZG9lIiwianRpIjoiRzlaUmxrV0FqSm9MV0RRWCIsIm5iZiI6MCwiZX" +
            "hwIjo0OTAyNzQ2NDAwfQ.BN0RQLZQqRJBcsKDXbGk1D-Q3n5B6sSbFS6VbKC4NdEXW79lNI-1WIpu0NKtw7CVE4lcE_7wkA" +
            "ZsOs1Z34YoR2X54l4gfqytOWp6nGo2ikx1hr0ik-YJByLIvw-Qm-YOSFuPy-vioXYI4C0FqAy4R6hkzUX_MKox_v4F_n_2y" +
            "HiaCbT_QR28xU0w1oC1hEmWv1uVeYYg-pcdrzTlN6Z0GbZN-WWj2D6oCPb6pW6jb94YKBZUki1Nru4ALxVaH121L73_yrXv" +
            "YuRBewACyiz5o_-2nJSQa4x2I7VoEfHqb1Cz8yQNL8FWeopOnNf83qNrhuPK2_EmjY6uHbIbnlc3ZdcABsEfwTUzYNsG504" +
            "H1xaoVl2WIL3o7vzlEGh1jWzjonQc0NlH9Z3zrqFrS1XGG7pFWQpfK3axHOV1ozsNVHUz4G78y8WXKQ90gVAWEDnK2BR-PV" +
            "J3kujlx6skDU8iaLDtEG70JQ4qeLSQI_q4w7UI0KMTnvN0SUqQnIUw-7L52K_scupNB_nM7_wh1vNaSb0kII_6lj1J3FRDC" +
            "XM2ehnf2Uyhd2mePuiu2Q3vfsIwzOhhlqCcJPcUjqQE5c5CnPw85Ero8OHFHhj2nQ6SqjUXQQGVnEX7ca8ayHNDcOt-tXDO" +
            "bfV3pJwypZK7i1LUgoHyYNh3kd6Cz1BBbbGNwpQ"


    def "Should validate JWT using JWKS URI"() {
        given:
        def config = mockedJwksUriConfiguration(mockedJwksUriResponse(jwksJson))
        def jwtValidator = new JwtValidatorManagedObject(config)

        when:
        def validatedClaims = jwtValidator.validate(validJwt)

        then:
        validatedClaims.sub != null
    }

    def "Jwks URI is only called once when validating multiple times"() {
        given:
        def httpClient = mockedJwksUriResponse(jwksJson)
        def config = mockedJwksUriConfiguration(httpClient)
        def jwtValidator = new JwtValidatorManagedObject(config)

        when:
        jwtValidator.validate(validJwt)
        def validatedClaims = jwtValidator.validate(validJwt)

        then:
        validatedClaims.sub != null
        // Expectations are set up so that the httpClient is only called once in #mockedJwksUriResponse
    }

    def "Should validate JWT with static key configuration"() {
        given:
        def config = mockedConfiguration(mockedStaticKeyResolver())
        def jwtValidator = new JwtValidatorManagedObject(config)

        when:
        def validatedClaims = jwtValidator.validate(validJwt)

        then:
        validatedClaims.sub != null
    }

    def "JWKS URI config fails when JWKS document is not available"() {
        given:
        def config = mockedJwksUriConfiguration(mockedJwksUriResponse(""))
        def jwtValidator = new JwtValidatorManagedObject(config)

        when:
        jwtValidator.validate(validJwt)

        then:
        def thrown = thrown(JwtValidationException)
        thrown.cause instanceof InvalidJwtException
        thrown.cause.cause instanceof UnresolvableKeyException
    }

    def "JWKS URI config does not require content type to be JSON"() {
        given:
        def responseHeaders = HttpHeaders.build().header("Content-Type", "text/plain").create()
        def config = mockedJwksUriConfiguration(mockedJwksUriResponse(jwksJson, responseHeaders))
        def jwtValidator = new JwtValidatorManagedObject(config)

        when:
        def validJwtContext = jwtValidator.validate(validJwt)

        then:
        validJwtContext.sub != null
    }

    def "JWT validator rejects token with invalid audience"() {
        given:
        def config = mockedConfiguration(keyResolver, "invalid-audience")
        def jwtValidator = new JwtValidatorManagedObject(config)

        when:
        jwtValidator.validate(validJwt)

        then:
        thrown(JwtValidationException)

        where:
        keyResolver << [mockedStaticKeyResolver(), mockedJwksUriKeyResolver(mockedJwksUriResponse(jwksJson))]
    }

    def "JWT validator rejects token with invalid issuer"() {
        given:
        def config = mockedConfiguration(keyResolver, "test-audience", "invalid-issuer")
        def jwtValidator = new JwtValidatorManagedObject(config)

        when:
        jwtValidator.validate(validJwt)

        then:
        thrown(JwtValidationException)

        where:
        keyResolver << [mockedStaticKeyResolver(), mockedJwksUriKeyResolver(mockedJwksUriResponse(jwksJson))]
    }

    def "JWT validator can validate token and omit specified claims from the result"() {
        given: "A configured jwt validator"

        when:
        def validatedClaims = jwtValidator.validateJwt(validJwt, "test-issuer", "test-audience", Set.of("sub"))

        then:
        validatedClaims.subject == null
        validatedClaims.scope == "read"

        where:
        jwtValidator << [new JwksUriJwtValidator(URI.create("https://localhost/jwks"), mockedJwksUriResponse(jwksJson)),
                         new ConfiguredKeyJwtValidator(mockedCryptoStore())]
    }

    private KeyResolverConfiguration mockedStaticKeyResolver() {
        return Mock(KeyResolverConfiguration) {
            getJwksUri() >> Optional.empty()
            getVerificationCryptoStore() >> {
                return Optional.of(mockedCryptoStore())
            }
        }
    }

    private mockedCryptoStore() {
        return Mock(AsymmetricSignatureVerificationCryptoStore) {
            getPublicKey() >> {
                return getFirstPublicKeyFromJwks(jwksJson)
            }
        }
    }

    private JwtValidatorConfiguration mockedJwksUriConfiguration(HttpClient httpClient) {
        return Mock(JwtValidatorConfiguration) {
            getKeyResolverConfiguration() >> {
                return mockedJwksUriKeyResolver(httpClient)
            }

            getExpectedAudience() >> {
                return "test-audience"
            }

            getExpectedIssuer() >> {
                return "test-issuer"
            }
        }
    }

    private JwtValidatorConfiguration mockedConfiguration(KeyResolverConfiguration keyResolverConfiguration,
                                                          String audience = "test-audience",
                                                          String issuer = "test-issuer") {
        return Mock(JwtValidatorConfiguration) {
            getKeyResolverConfiguration() >> {
                return keyResolverConfiguration
            }

            getExpectedAudience() >> {
                return audience
            }

            getExpectedIssuer() >> {
                return issuer
            }
        }
    }

    private KeyResolverConfiguration mockedJwksUriKeyResolver(HttpClient mockedHttpClient) {
        return Mock(KeyResolverConfiguration) {
            getVerificationCryptoStore() >> Optional.empty()

            getJwksUri() >> {
                return Optional.of(Mock(JwksUriKeyResolverConfiguration) {
                    getHttpClient() >> {
                        return mockedHttpClient
                    }

                    getJwksUri() >> {
                        return new URI("http://localhost:8080/.well-known/jwks.json")
                    }
                })
            }
        }
    }

    HttpClient mockedJwksUriResponse(String responseBody,
                                     HttpHeaders responseHeaders = HttpHeaders.build().create()) {
        HttpClient httpClientMock = Mock(HttpClient) {
            1 * request(_) >> {
                return Mock(HttpRequest.Builder) {
                    1 * get() >> {
                        return Mock(HttpRequest) {
                            1 * response() >> {
                                return Mock(HttpResponse) {
                                    1 * headers() >> responseHeaders
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

    PublicKey getFirstPublicKeyFromJwks(String jwksJson) {
        JsonWebKeySet jwks = new JsonWebKeySet(jwksJson)
        return jwks.getJsonWebKeys().first.key as PublicKey
    }

}