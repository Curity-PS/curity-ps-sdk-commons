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

package io.curity.identityserver.plugins.introspection;

import io.curity.identityserver.plugins.attributes.IntrospectionAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.http.HttpRequest;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.Json;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An opaque token validator that calls a token introspection endpoint (RFC 7662)
 * to validate the token and retrieve associated claims.
 * <p>
 * The client authenticates to the introspection endpoint using HTTP Basic authentication.
 */
public final class AccessTokenValidator implements OpaqueTokenValidator
{
    private static final Logger _logger = LoggerFactory.getLogger(AccessTokenValidator.class);

    private final URI _introspectionEndpoint;
    private final HttpClient _httpClient;
    private final Json _json;
    private final String _basicAuthHeader;

    /**
     * Constructor
     *
     * @param introspectionEndpoint the URI of the token introspection endpoint
     * @param httpClient            the HTTP client to use for introspection requests
     * @param json                  the JSON service for parsing responses
     * @param clientId              the client ID for authenticating to the introspection endpoint
     * @param clientSecret          the client secret for authenticating to the introspection endpoint
     */
    public AccessTokenValidator(URI introspectionEndpoint,
                                HttpClient httpClient,
                                Json json,
                                String clientId,
                                String clientSecret)
    {
        _introspectionEndpoint = introspectionEndpoint;
        _httpClient = httpClient;
        _json = json;
        _basicAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IntrospectionAttributes validateToken(String token, String issuer, String audience, List<String> scopes) throws IntrospectionException
    {
        _logger.debug("Introspecting token at {}", _introspectionEndpoint);

        HttpResponse response = _httpClient.request(_introspectionEndpoint)
                .contentType("application/x-www-form-urlencoded")
                .header("Authorization", _basicAuthHeader)
                .body(HttpRequest.createFormUrlEncodedBodyProcessor(Map.of("token", token)))
                .method("POST")
                .response();

        int statusCode = response.statusCode();
        if (statusCode != 200)
        {
            _logger.warn("Introspection endpoint returned status {}", statusCode);
            throw new IntrospectionException("Introspection endpoint returned HTTP " + statusCode);
        }

        Map<String, Object> responseBody = _json.fromJson(response.body(HttpResponse.asString()));

        IntrospectionAttributes attributes = IntrospectionAttributes.fromMap(responseBody);

        if (!attributes.isActive())
        {
            _logger.debug("Token is not active");
            throw new IntrospectionException("Token is not active");
        }

        String responseIssuer = attributes.getIssuer();
        if (responseIssuer == null)
        {
            _logger.warn("Introspection response is missing required 'iss' claim");
            throw new IntrospectionException("Introspection response is missing required 'iss' claim");
        }
        if (!responseIssuer.equals(issuer))
        {
            _logger.warn("Issuer mismatch: expected '{}', got '{}'", issuer, responseIssuer);
            throw new IntrospectionException("Issuer mismatch: expected '" + issuer + "', got '" + responseIssuer + "'");
        }

        var audiences = attributes.getAudiences();
        if (audiences.isEmpty())
        {
            _logger.warn("Introspection response is missing required 'aud' claim");
            throw new IntrospectionException("Introspection response is missing required 'aud' claim");
        }
        if (!audiences.contains(audience))
        {
            _logger.warn("Audience mismatch: expected '{}', got '{}'", audience, audiences);
            throw new IntrospectionException("Audience mismatch: expected '" + audience + "', got '" + audiences + "'");
        }

        if (scopes != null && !scopes.isEmpty())
        {
            String scopeString = attributes.getScope();
            if (scopeString == null || scopeString.isBlank())
            {
                _logger.warn("Introspection response is missing required 'scope' claim");
                throw new IntrospectionException("Introspection response is missing required 'scope' claim");
            }
            Set<String> responseScopes = new HashSet<>(Arrays.asList(scopeString.split(" ")));
            List<String> missingScopes = scopes.stream().filter(s -> !responseScopes.contains(s)).toList();
            if (!missingScopes.isEmpty())
            {
                _logger.warn("Scope mismatch: missing required scopes {}", missingScopes);
                throw new IntrospectionException("Scope mismatch: missing required scopes " + missingScopes);
            }
        }

        return attributes;
    }
}
