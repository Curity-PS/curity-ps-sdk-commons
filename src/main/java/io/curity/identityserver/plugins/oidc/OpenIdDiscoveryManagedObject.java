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

package io.curity.identityserver.plugins.oidc;

import io.curity.identityserver.plugins.introspection.AccessTokenValidator;
import io.curity.identityserver.plugins.introspection.OpaqueTokenValidator;
import io.curity.identityserver.plugins.jwt.JwksUriJwtValidator;
import io.curity.identityserver.plugins.jwt.JwtValidator;
import io.curity.identityserver.plugins.utils.NullUtils;
import io.curity.identityserver.plugins.utils.UriHelper;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.service.HttpClient;

import java.net.URI;
import java.util.Map;

/**
 * A managed object for OpenID Discovery. Requires a {@link OpenIdDiscoveryConfiguration} with configuration of the metadata.
 * Also fetches the JWKS found in the metadata. Server configuration is config scoped
 *
 * @param <T> Configuration type for plugin
 */
public final class OpenIdDiscoveryManagedObject<T extends Configuration> extends ManagedObject<T>
{
    private final Map<String, Object> _discoveredConfiguration;
    private final OpenIdDiscoveryConfiguration _openIdDiscoveryConfiguration;
    private final JwksUriJwtValidator _jwtValidator;
    @Nullable
    private final OpaqueTokenValidator _opaqueTokenValidator;

    /**
     * Main constructor for the managed object
     *
     * @param configuration                plugin config
     * @param openIdDiscoveryConfiguration Configuration for the discovery fetcher
     */
    public OpenIdDiscoveryManagedObject(T configuration, OpenIdDiscoveryConfiguration openIdDiscoveryConfiguration)
    {
        this(configuration, openIdDiscoveryConfiguration, null, null);
    }

    /**
     * Constructor that also creates an opaque token validator using the discovered introspection endpoint
     *
     * @param configuration                plugin config
     * @param openIdDiscoveryConfiguration Configuration for the discovery fetcher
     * @param clientId                     the client ID for authenticating to the introspection endpoint
     * @param clientSecret                 the client secret for authenticating to the introspection endpoint
     */
    public OpenIdDiscoveryManagedObject(T configuration, OpenIdDiscoveryConfiguration openIdDiscoveryConfiguration,
                                        @Nullable String clientId, @Nullable String clientSecret)
    {
        super(configuration);
        _openIdDiscoveryConfiguration = openIdDiscoveryConfiguration;
        HttpClient httpClient = openIdDiscoveryConfiguration.getHttpClient();
        _discoveredConfiguration = fetchProviderConfiguration(openIdDiscoveryConfiguration.getIssuer(), httpClient);
        _jwtValidator = new JwksUriJwtValidator(URI.create(getConfiguredString("jwks_uri")), httpClient);
        _opaqueTokenValidator = clientId != null && clientSecret != null
                ? new AccessTokenValidator(getTokenIntrospectionEndpoint(), httpClient,
                        openIdDiscoveryConfiguration.getJson(), clientId, clientSecret)
                : null;
    }

    private Map<String, Object> fetchProviderConfiguration(URI issuer, HttpClient httpClient)
    {
        return httpClient.request(UriHelper.appendPath(issuer, "/.well-known/openid-configuration"))
                .get()
                .response()
                .body(HttpResponse.asJsonObject(_openIdDiscoveryConfiguration.getJson()));
    }

    /**
     * Get a configuration value of a specific type from the discovered provider metadata
     *
     * @param type the expected type of the value
     * @param key  the key to look for in the provider metadata
     * @param <C>  the expected type of the value
     * @return the value of the expected type
     * @throws IllegalArgumentException when the value is not found or is not of the expected type
     */
    public <C> C getConfigurationValueOfType(Class<C> type, String key)
    {
        return NullUtils.valueOfTypeOrError(type, _discoveredConfiguration.get(key),
                "Did not find " + key + " of type" + type.toString() + " in provider metadata");
    }

    private String getConfiguredString(String key)
    {
        return NullUtils.valueOfTypeOrError(String.class, _discoveredConfiguration.get(key),
                "Did not find " + key + " in provider metadata");
    }

    /**
     * Get the token endpoint URI from the provider metadata
     *
     * @return the token endpoint URI
     */
    public URI getTokenEndpoint()
    {
        return URI.create(getConfiguredString("token_endpoint"));
    }

    /**
     * Get the token introspection endpoint URI from the provider metadata
     *
     * @return the token introspection endpoint URI
     */
    public URI getTokenIntrospectionEndpoint()
    {
        return URI.create(getConfiguredString("introspection_endpoint"));
    }

    /**
     * Get the authorization endpoint URI from the provider metadata
     *
     * @return the authorization endpoint URI
     */
    public URI getAuthorizeEndpoint()
    {
        return URI.create(getConfiguredString("authorization_endpoint"));
    }

    /**
     * Get the backchannel authentication endpoint URI from the provider metadata
     *
     * @return the backchannel authentication endpoint URI
     */
    public URI getBackChannelAuthenticationEndpoint()
    {
        return URI.create(getConfiguredString("backchannel_authentication_endpoint"));
    }

    /**
     * Get the JWT validator, configured with the JWKS URI from the provider metadata
     *
     * @return the JWT validator
     */
    public JwtValidator getJwtValidator()
    {
        return _jwtValidator;
    }

    /**
     * Get the opaque token validator, configured with the introspection endpoint from the provider metadata.
     * Only available when the managed object was created with client credentials.
     *
     * @return the opaque token validator, or null if client credentials were not provided
     */
    @Nullable
    public OpaqueTokenValidator getOpaqueTokenValidator()
    {
        return _opaqueTokenValidator;
    }

    /**
     * Get the HttpClient configured for OpenID Discovery
     *
     * @return HttpClient
     */
    public HttpClient getHttpClient()
    {
        return _openIdDiscoveryConfiguration.getHttpClient();
    }
}