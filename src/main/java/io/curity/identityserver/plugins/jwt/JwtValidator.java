package io.curity.identityserver.plugins.jwt;

import java.util.Map;
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
    default Map<String, Object> validateJwt(String jwt, String issuer, String audience) throws JwtValidationException {
        return validateJwt(jwt, issuer, audience, Set.of());
    }


    Map<String, Object> validateJwt(String jwt,
                                    String issuer,
                                    String audience,
                                    Set<String> excludeClaims) throws JwtValidationException;
}
