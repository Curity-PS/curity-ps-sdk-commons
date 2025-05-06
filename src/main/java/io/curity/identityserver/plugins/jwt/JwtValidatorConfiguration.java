package io.curity.identityserver.plugins.jwt;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.OneOf;
import se.curity.identityserver.sdk.config.annotation.DefaultService;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.crypto.AsymmetricSignatureVerificationCryptoStore;

import java.net.URL;

public interface JwtValidatorConfiguration extends Configuration
{
    KeyResolverConfiguration getKeyResolverConfiguration();

    @Description("The expected audience of the JWT to validate")
    String getExpectedAudience();

    @Description("The expected issuer of the JWT to validate")
    String getExpectedIssuer();

    interface KeyResolverConfiguration extends OneOf
    {
        AsymmetricSignatureVerificationCryptoStore getVerificationCryptoStore();

        @Description("Resolve the verification key through a JWKS URI")
        JwksUriKeyResolverConfiguration getJwksUri();
    }

    interface JwksUriKeyResolverConfiguration extends Configuration
    {
        @DefaultService
        HttpClient getHttpClient();

        @Description("The JWKS URI")
        URL getJwksUri();
    }
}
