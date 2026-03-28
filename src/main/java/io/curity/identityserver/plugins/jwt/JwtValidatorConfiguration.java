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

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.OneOf;
import se.curity.identityserver.sdk.config.annotation.DefaultService;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.crypto.AsymmetricSignatureVerificationCryptoStore;

import java.net.URI;
import java.util.Optional;

/**
 * A configuration interface for JWT validation.
 * Include in your Configuration to be able to use the JWT validator from the library.
 */
public interface JwtValidatorConfiguration extends Configuration
{
    KeyResolverConfiguration getKeyResolverConfiguration();

    @Description("The expected audience of the JWT to validate")
    String getExpectedAudience();

    @Description("The expected issuer of the JWT to validate")
    String getExpectedIssuer();

    interface KeyResolverConfiguration extends OneOf
    {
        Optional<AsymmetricSignatureVerificationCryptoStore> getVerificationCryptoStore();

        @Description("Resolve the verification key through a JWKS URI")
        Optional<JwksUriKeyResolverConfiguration> getJwksUri();
    }

    interface JwksUriKeyResolverConfiguration extends Configuration
    {
        @DefaultService
        HttpClient getHttpClient();

        @Description("The JWKS URI")
        URI getJwksUri();
    }

    ExceptionFactory getExceptionFactory();
}