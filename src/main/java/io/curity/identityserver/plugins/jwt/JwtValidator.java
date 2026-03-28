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