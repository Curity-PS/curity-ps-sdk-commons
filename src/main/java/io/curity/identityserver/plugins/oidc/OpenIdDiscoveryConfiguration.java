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

package io.curity.identityserver.plugins.oidc;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.DefaultService;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.Json;

import java.net.URI;

public interface OpenIdDiscoveryConfiguration extends Configuration
{
    @Description("The issuer URI of the OpenID Connect provider. This will be used to discover the OpenID Connect configuration, as well as for validation.")
    URI getIssuer();

    @Description("The HTTP client to use for making requests to the OpenID Connect provider to collect the metadata.")
    @DefaultService
    HttpClient getHttpClient();

    Json getJson();
}