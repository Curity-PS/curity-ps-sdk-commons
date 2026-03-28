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

package io.curity.identityserver.plugins.jwt;

import io.curity.identityserver.plugins.attributes.ValidatedJwtAttributes;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.service.HttpClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Set;

/**
 * Jwt Validator built to be used in a managed object.
 *
 * It will hold a reference to a @{link HttpsJwks} object that will keep the keys fresh
 */
public final class JwksUriJwtValidator implements JwtValidator
{
    @Nullable
    private final HttpsJwks _httpsJwks;

    private static final Logger _logger = LoggerFactory.getLogger(JwksUriJwtValidator.class);

    /**
     * Constructor
     *
     * @param jwksUri the URI to the JWKS. Will be cached, but updated if necessary
     * @param httpClient the HTTP client to use for fetching the JWKS
     */
    public JwksUriJwtValidator(URI jwksUri, HttpClient httpClient)
    {
        _httpsJwks = createJwksResolver(jwksUri, httpClient);
    }

    @Override
    public ValidatedJwtAttributes validateJwt(String jwt, String issuer, String audience, Set<String> excludeClaims) throws JwtValidationException
    {
        try
        {
            var claims = createJwtConsumer(issuer, audience).processToClaims(jwt);
            return ValidatedJwtAttributes.fromMap(claims.getClaimsMap(excludeClaims));
        }
        catch (InvalidJwtException e)
        {
            _logger.warn("Invalid JWT: {}", e.getMessage());
            throw new JwtValidationException(e);
        }
    }

    private JwtConsumer createJwtConsumer(String issuer, String audience)
    {
        return new JwtConsumerBuilder()
                .setRequireSubject()
                .setExpectedAudience(audience)
                .setExpectedIssuer(issuer)
                .setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(_httpsJwks))
                .build();
    }

    private static HttpsJwks createJwksResolver(@Nullable URI uri, @Nullable HttpClient httpClient)
    {
        _logger.debug("Creating JWT consumer using JWKs URI {}", uri);
        HttpsJwks httpsJwks;
        try
        {
            httpsJwks = new HttpsJwks(uri.toURL().toExternalForm());
        }
        catch (MalformedURLException e)
        {
            _logger.warn("Malformed JWKs URI: {}", uri);
            throw new RuntimeException("Malformed JWKs URI", e);
        }

        httpsJwks.setSimpleHttpGet(new CustomSimpleGet(httpClient));
        return httpsJwks;
    }
}