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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.plugin.ManagedObject;

/**
 * A managed object for opaque token validation via introspection (RFC 7662).
 * Requires a {@link OpaqueTokenValidatorConfiguration} that configures
 * the introspection endpoint and client credentials.
 * <p>
 * Ready to be added to a plugin descriptor.
 *
 * @param <T> Configuration type for plugin
 */
public final class OpaqueTokenValidatorManagedObject<T extends Configuration> extends ManagedObject<T>
{
    private static final Logger _logger = LoggerFactory.getLogger(OpaqueTokenValidatorManagedObject.class);
    private final OpaqueTokenValidator _validator;
    private final OpaqueTokenValidatorConfiguration _configuration;

    OpaqueTokenValidatorManagedObject(OpaqueTokenValidatorConfiguration config)
    {
        //noinspection unchecked
        super((T) config);
        _configuration = config;
        _validator = createValidator(config);
    }

    /**
     * Main constructor for the managed object
     *
     * @param config                plugin config
     * @param introspectionConfig   Configuration for the introspection validator
     */
    public OpaqueTokenValidatorManagedObject(T config, OpaqueTokenValidatorConfiguration introspectionConfig)
    {
        super(config);
        _configuration = introspectionConfig;
        _validator = createValidator(introspectionConfig);
    }

    private OpaqueTokenValidator createValidator(OpaqueTokenValidatorConfiguration config)
    {
        _logger.debug("Creating opaque token validator for introspection endpoint {}", config.getIntrospectionEndpoint());
        return new AccessTokenValidator(
                config.getIntrospectionEndpoint(),
                config.getHttpClient(),
                config.getJson(),
                config.getClientId(),
                config.getClientSecret().getValue()
        );
    }

    /**
     * Validate an opaque access token via introspection according to the configuration
     *
     * @param token the token to validate
     * @return Map of the claims from the introspection response
     */
    public IntrospectionAttributes validate(String token) throws IntrospectionException
    {
        _logger.debug("Validating opaque token with expected audience '{}', expected issuer '{}', and expected scopes '{}'",
                _configuration.getExpectedAudience(), _configuration.getExpectedIssuer(), _configuration.getExpectedScopes());
        return _validator.validateToken(token, _configuration.getExpectedIssuer(), _configuration.getExpectedAudience(),
                _configuration.getExpectedScopes());
    }
}
