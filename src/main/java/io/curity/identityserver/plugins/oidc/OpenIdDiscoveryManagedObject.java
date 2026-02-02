package io.curity.identityserver.plugins.oidc;

import io.curity.identityserver.plugins.jwt.JwksUriJwtValidator;
import io.curity.identityserver.plugins.jwt.JwtValidator;
import io.curity.identityserver.plugins.utils.NullUtils;
import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.WebServiceClient;

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

    /**
     * Main constructor for the managed object
     *
     * @param configuration                plugin config
     * @param openIdDiscoveryConfiguration Configuration for the discovery fetcher
     */
    public OpenIdDiscoveryManagedObject(T configuration, OpenIdDiscoveryConfiguration openIdDiscoveryConfiguration)
    {
        super(configuration);
        _openIdDiscoveryConfiguration = openIdDiscoveryConfiguration;
        HttpClient httpClient = openIdDiscoveryConfiguration.getHttpClient();
        _discoveredConfiguration = fetchProviderConfiguration(openIdDiscoveryConfiguration.getIssuer(), httpClient);
        _jwtValidator = new JwksUriJwtValidator(URI.create(getConfiguredString("jwks_uri")), httpClient);
    }

    private Map<String, Object> fetchProviderConfiguration(URI issuer, HttpClient httpClient)
    {
        return httpClient.request(issuer.resolve(".well-known/openid-configuration"))
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
     * Create a webservice client for the given URI, configured with the HttpClient from the OpenID Discovery configuration
     *
     * @param uri the URI to create the webservice client for
     * @return the webservice client
     */
    public WebServiceClient getWebserviceClientFor(URI uri)
    {
        return _openIdDiscoveryConfiguration.webserviceClientFactory()
                .create(_openIdDiscoveryConfiguration.getHttpClient())
                .withHost(uri.getHost())
                .withPath(uri.getPath());
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
     * Get the HttpClient configured for OpenID Discovery
     *
     * @return HttpClient
     */
    public HttpClient getHttpClient()
    {
        return _openIdDiscoveryConfiguration.getHttpClient();
    }
}
