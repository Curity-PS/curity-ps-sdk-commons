package io.curity.identityserver.plugins.jwt;

import io.curity.identityserver.plugins.attributes.ValidatedJwtAttributes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.service.crypto.AsymmetricSignatureVerificationCryptoStore;

import java.util.Set;

/**
 * Jwt Validator built to be used in a managed object.
 * If configured to use a JWKs URI, it will hold a reference to a @{link HttpsJwks} object that will keep the keys fresh
 */
public final class ConfiguredKeyJwtValidator implements JwtValidator
{
    private final AsymmetricSignatureVerificationCryptoStore _cryptoStore;

    private static final Logger _logger = LoggerFactory.getLogger(ConfiguredKeyJwtValidator.class);

    /**
     * Constructor
     *
     * @param cryptoStore the asymmetric signature verification crypto store
     */
    public ConfiguredKeyJwtValidator(AsymmetricSignatureVerificationCryptoStore cryptoStore)
    {
        _cryptoStore = cryptoStore;
    }

    @Override
    public ValidatedJwtAttributes validateJwt(String jwt, String issuer, String audience, Set<String> excludedClaims)
    {
        try
        {
            var claims = createJwtConsumer(issuer, audience).processToClaims(jwt);
            return ValidatedJwtAttributes.fromMap(claims.getClaimsMap(excludedClaims));
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
                .setVerificationKey(_cryptoStore.getPublicKey())
                .build();
    }
}
