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

import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.plugin.ManagedObject;

/**
 * A JWT validator to be used as a managed object. Requires a {@link JwtValidatorConfiguration} that configures
 * how to collect key material and how the JWT should be validated
 */
public final class JwtValidatorManagedObject<T extends JwtValidatorConfiguration> extends ManagedObject<T>
{
    private static final Logger _logger = LoggerFactory.getLogger(JwtValidatorManagedObject.class);
    private final JwtConsumer _consumer;

    public JwtValidatorManagedObject(T config)
    {
        super(config);
        _consumer = createJwtConsumer(config);
    }

    private JwtConsumer createJwtConsumer(T config)
    {
        JwtConsumerBuilder builder = new JwtConsumerBuilder()
                .setRequireSubject()
                .setExpectedAudience(config.getExpectedAudience())
                .setExpectedIssuer(config.getExpectedIssuer());

        var keyResolverConfiguration = config.getKeyResolverConfiguration();
        if (keyResolverConfiguration.getJwksUri() != null)
        {
            var httpsJwks = new HttpsJwks(keyResolverConfiguration.getJwksUri().getJwksUri().toExternalForm());
            httpsJwks.setSimpleHttpGet(new CustomSimpleGet(keyResolverConfiguration.getJwksUri().getHttpClient()));
            builder.setVerificationKeyResolver(new HttpsJwksVerificationKeyResolver(httpsJwks));
        }
        else if (keyResolverConfiguration.getVerificationCryptoStore() != null)
        {
            builder.setVerificationKey(keyResolverConfiguration.getVerificationCryptoStore().getPublicKey());
        }

        return builder.build();
    }

    /**
     * Validate a JWT token according to the configuration
     *
     * @param token the token to validate
     * @return the context of the token
     * @throws InvalidJwtException if the token is invalid
     */
    public JwtContext validate(String token) throws InvalidJwtException
    {
        return _consumer.process(token);
    }
}
