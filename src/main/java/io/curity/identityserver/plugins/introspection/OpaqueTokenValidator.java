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

package io.curity.identityserver.plugins.introspection;

import io.curity.identityserver.plugins.attributes.IntrospectionAttributes;

/**
 * A simple interface for validating opaque access tokens via token introspection (RFC 7662).
 * The implementation calls the introspection endpoint to determine if the token is active
 * and to retrieve associated claims. The issuer and audience are validated against the
 * introspection response.
 */
public interface OpaqueTokenValidator
{
    /**
     * Validates an opaque access token by calling the introspection endpoint.
     * Validates that the issuer and audience in the introspection response match the expected values.
     *
     * @param token    the opaque access token to validate
     * @param issuer   String that has to match the {@code iss} claim of the introspection response
     * @param audience String that has to be in the {@code aud} claim of the introspection response
     * @return the introspection response attributes if the token is active
     * @throws IntrospectionException if the token is not active, issuer/audience don't match, or the introspection call fails
     */
    IntrospectionAttributes validateToken(String token, String issuer, String audience) throws IntrospectionException;
}
