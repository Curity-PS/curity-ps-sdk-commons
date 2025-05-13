package io.curity.identityserver.plugins.jwt;

import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.service.HttpClient;

import java.net.MalformedURLException;
import java.net.URI;

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

    public JwksUriJwtValidator(URI jwksUri, HttpClient httpClient)
    {
        _httpsJwks = createJwksResolver(jwksUri, httpClient);
    }

    @Override
    public JwtContext validateJwt(String jwt, String issuer, String audience)
    {
        try
        {
            return createJwtConsumer(issuer, audience).process(jwt);
        }
        catch (InvalidJwtException e)
        {
            _logger.warn("Invalid JWT: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT", e);
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
