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

package io.curity.identityserver.plugins.oauth;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.EncryptedString;
import se.curity.identityserver.sdk.config.annotation.DefaultBoolean;
import se.curity.identityserver.sdk.config.annotation.DefaultString;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.Json;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;

public interface OAuthClientConfig extends Configuration
{
    @Description("The client ID to use for the OAuth client.")
    String getClientId();

    @Description("The client secret to use for the OAuth client.")
    EncryptedString getClientSecret();

    @DefaultString("openid")
    @Description("The scope to use for the OAuth client. Space separated string")
    String getScope();

    @Description("Use Proof Key for Code Exchange (PKCE) for the OAuth client.")
    @DefaultBoolean(true)
    boolean usePkce();

    AuthenticatorInformationProvider authenticatorInformationProvider();
    ExceptionFactory exceptionFactory();
    SessionManager sessionManager();
    Json json();
}