package io.curity.identityserver.plugins.jwt;

import io.curity.identityserver.plugins.attributes.ValidatedJwtAttributes;

import java.util.Set;

/**
 * A simple interface for validating JWTs. The implementation should be able to validate a JWT using the configured key resolver.
 * The JWT must contain a "sub" claim and the issuer and audience.
 */
public interface JwtValidator
{
    /**
     * Validates a JWT using the configured key resolver.
     * The JWT must contain a "sub" claim and the issuer and audience
     *
     * @param jwt      to validate
     * @param issuer   String that has to match the `iss` claim of the JWT
     * @param audience String that has to be in the `aud` claim of the JWT
     * @return Map of claims in the JWT
     */
    default ValidatedJwtAttributes validateJwt(String jwt, String issuer, String audience) throws JwtValidationException {
        return validateJwt(jwt, issuer, audience, Set.of());
    }


    ValidatedJwtAttributes validateJwt(String jwt,
                                       String issuer,
                                       String audience,
                                       Set<String> excludeClaims) throws JwtValidationException;
}
