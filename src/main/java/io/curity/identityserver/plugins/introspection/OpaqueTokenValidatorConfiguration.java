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

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.EncryptedString;
import se.curity.identityserver.sdk.config.annotation.DefaultService;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.Json;

import java.net.URI;
import java.util.List;

/**
 * A configuration interface for opaque token validation via introspection (RFC 7662).
 * Include in your Configuration to be able to use the opaque token validator from the library.
 */
public interface OpaqueTokenValidatorConfiguration extends Configuration
{
    @Description("The URI of the token introspection endpoint (RFC 7662)")
    URI getIntrospectionEndpoint();

    @Description("The client ID used for authenticating to the introspection endpoint")
    String getClientId();

    @Description("The client secret used for authenticating to the introspection endpoint")
    EncryptedString getClientSecret();

    @Description("The expected audience of the token to validate")
    String getExpectedAudience();

    @Description("The expected issuer of the token to validate")
    String getExpectedIssuer();

    @Description("The expected scopes that the token must contain")
    List<String> getExpectedScopes();

    @DefaultService
    HttpClient getHttpClient();

    Json getJson();
}
