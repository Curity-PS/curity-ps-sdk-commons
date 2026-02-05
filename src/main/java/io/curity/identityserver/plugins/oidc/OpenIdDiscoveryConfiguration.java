package io.curity.identityserver.plugins.oidc;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.DefaultService;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.Json;

import java.net.URI;

public interface OpenIdDiscoveryConfiguration extends Configuration
{
    @Description("The issuer URI of the OpenID Connect provider. This will be used to discover the OpenID Connect configuration, as well as for validation.")
    URI getIssuer();

    @Description("The HTTP client to use for making requests to the OpenID Connect provider to collect the metadata.")
    @DefaultService
    HttpClient getHttpClient();

    Json getJson();
}
