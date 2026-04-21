/*
 *  Copyright 2025 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugins.jwt;

import io.curity.identityserver.plugins.attributes.ValidatedJwtAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.service.ExceptionFactory;

/**
 * A managed object for JWT validation. Requires a {@link JwtValidatorConfiguration} that configures
 * how to collect key material and how the JWT should be validated
 * Keeps an internal @{link JwtValidator} that is used to validate the JWT, which is responsible for caching the key material
 * <p>
 * Ready to be added to a plugin descriptor
 *
 * @param <T> Configuration type for plugin
 */
public final class JwtValidatorManagedObject<T extends Configuration> extends ManagedObject<T>
{
    private static final Logger _logger = LoggerFactory.getLogger(JwtValidatorManagedObject.class);
    private final JwtValidator _validator;
    private final ExceptionFactory _exceptionFactory;
    private final JwtValidatorConfiguration _configuration;

    JwtValidatorManagedObject(JwtValidatorConfiguration jwtValidatorConfiguration)
    {
        //noinspection unchecked
        super((T) jwtValidatorConfiguration);
        _configuration = jwtValidatorConfiguration;
        _validator = createJwtValidator(jwtValidatorConfiguration);
        _exceptionFactory = jwtValidatorConfiguration.getExceptionFactory();
    }

    /**
     * Main constructor for the managed object
     *
     * @param config plugin config
     * @param jwtValidatorConfiguration Configuration for the validator
     */
    public JwtValidatorManagedObject(T config, JwtValidatorConfiguration jwtValidatorConfiguration)
    {
        super(config);
        _configuration = jwtValidatorConfiguration;
        _validator = createJwtValidator(jwtValidatorConfiguration);
        _exceptionFactory = jwtValidatorConfiguration.getExceptionFactory();
    }

    private JwtValidator createJwtValidator(JwtValidatorConfiguration config)
    {
        if (config.getKeyResolverConfiguration().getJwksUri().isPresent())
        {
            _logger.debug("Creating JWT validator using JWKs URI");
            var jwksUriConfiguration = config.getKeyResolverConfiguration().getJwksUri().get();
            return new JwksUriJwtValidator(jwksUriConfiguration.getJwksUri(), jwksUriConfiguration.getHttpClient());
        }
        else if (config.getKeyResolverConfiguration().getVerificationCryptoStore().isPresent())
        {
            _logger.debug("Creating JWT validator using a configured keystore");
            var verificationCryptoStore = config.getKeyResolverConfiguration().getVerificationCryptoStore().get();
            return new ConfiguredKeyJwtValidator(verificationCryptoStore);
        }
        else
        {
            throw _exceptionFactory.internalServerException(ErrorCode.CONFIGURATION_ERROR,
                    "No valid key resolver configuration found");
        }
    }

    /**
     * Validate a JWT token according to the configuration
     *
     * @param token the token to validate
     *
     * @return Validated JWT attributes
     */
    public ValidatedJwtAttributes validate(String token)
    {
        _logger.debug("Validating JWT token with expected audience '{}' and issuer '{}'",
                _configuration.getExpectedAudience(), _configuration.getExpectedIssuer());
        return _validator.validateJwt(token, _configuration.getExpectedIssuer(), _configuration.getExpectedAudience());
    }
}
