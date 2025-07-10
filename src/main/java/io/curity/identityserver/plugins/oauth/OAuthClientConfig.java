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
