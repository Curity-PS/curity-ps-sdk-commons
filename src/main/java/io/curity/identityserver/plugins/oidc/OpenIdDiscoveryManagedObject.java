package io.curity.identityserver.plugins.oidc;

import io.curity.identityserver.plugins.jwt.JwksUriJwtValidator;
import io.curity.identityserver.plugins.jwt.JwtValidator;
import io.curity.identityserver.plugins.utils.NullUtils;
import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.service.HttpClient;

import java.net.URI;
import java.util.Map;

/**
 * A managed object for OpenID Discovery. Requires a {@link OpenIdDiscoveryConfiguration} with configuration of the metadata.
 * Also fetches the JWKS found in the metadata. Server configuration is config scoped
 */
public final class OpenIdDiscoveryManagedObject<T extends Configuration> extends ManagedObject<T>
{
    private final Map<String, Object> _discoveredConfiguration;
    private final OpenIdDiscoveryConfiguration _openIdDiscoveryConfiguration;
    private final JwksUriJwtValidator _jwtValidator;

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

    public <C> C getConfigurationValurOfType(Class<C> type, String key)
    {
        return NullUtils.valueOfTypeOrError(type, _discoveredConfiguration.get(key),
                "Did not find " + key + " of type" + type.toString() + " in provider metadata");
    }

    private String getConfiguredString(String key)
    {
        return NullUtils.valueOfTypeOrError(String.class, _discoveredConfiguration.get(key),
                "Did not find " + key + " in provider metadata");
    }

    public URI getTokenEndpoint()
    {
        return URI.create(getConfiguredString("token_endpoint"));
    }

    public URI getAuthorizeEndpoint()
    {
        return URI.create(getConfiguredString("authorization_endpoint"));
    }

    public JwtValidator getJwtValidator()
    {
        return _jwtValidator;
    }
}
