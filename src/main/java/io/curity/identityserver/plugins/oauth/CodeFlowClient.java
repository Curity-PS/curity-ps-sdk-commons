package io.curity.identityserver.plugins.oauth;

import io.curity.identityserver.plugins.utils.PkceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpMethod;
import se.curity.identityserver.sdk.http.HttpRequest;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.Json;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static se.curity.identityserver.sdk.http.RedirectStatusCode.TEMPORARY_REDIRECT;

/**
 * A client for the OAuth Authorization Code Flow.
 * Will use a redirect_uri on the format of
 * <pre>
 * ${base-url}>/${authentication-endpoint}/${authenticator-id}/callback`
 * </pre>
 */
public final class CodeFlowClient
{
    public static final String REDIRECT_URI_ENDPOINT_SUFFIX = "callback";

    private final AuthenticatorInformationProvider _authenticatorInformationProvider;
    private final ExceptionFactory _exceptionFactory;
    private final SessionManager _sessionManager;
    private final Json _json;
    private final URI _authorizationEndpoint;
    private final URI _tokenEndpoint;
    private final HttpClient _httpClient;
    private static final Logger _logger = LoggerFactory.getLogger(CodeFlowClient.class);
    private final OAuthClientConfig _config;

    public CodeFlowClient(OAuthClientConfig oauthClientConfig,
                          URI authorizationEndpoint,
                          URI tokenEndpoint,
                          HttpClient httpClient)
    {
        _authenticatorInformationProvider = oauthClientConfig.authenticatorInformationProvider();
        _exceptionFactory = oauthClientConfig.exceptionFactory();
        _sessionManager = oauthClientConfig.sessionManager();
        _json = oauthClientConfig.json();
        _authorizationEndpoint = authorizationEndpoint;
        _tokenEndpoint = tokenEndpoint;
        _httpClient = httpClient;
        _config = oauthClientConfig;
    }

    public RuntimeException createAuthorizationUrlRedirect(Map<String, String> additionalParameters)
    {
        Map<String, Collection<String>> queryParameters = new LinkedHashMap<>();
        String redirectUri = createRedirectUri(_authenticatorInformationProvider, _exceptionFactory);
        queryParameters.put("redirect_uri", Collections.singletonList(redirectUri));

        String state = UUID.randomUUID().toString();
        queryParameters.put("state", Collections.singletonList(state));
        _sessionManager.put(Attribute.of("state", state));

        if(_config.getScope().contains("openid")) {
            String nonce = UUID.randomUUID().toString();
            queryParameters.put("nonce", Collections.singletonList(nonce));
            _sessionManager.put(Attribute.of("nonce", nonce));
        }

        if (_config.usePkce())
        {
            String codeVerifier = PkceHelper.generateCodeVerifier();
            String codeChallenge = PkceHelper.challengeFromVerifier(codeVerifier);

            _sessionManager.put(Attribute.of("code_verifier", codeVerifier));
            queryParameters.put("code_challenge", Collections.singletonList(codeChallenge));
            queryParameters.put("code_challenge_method", Collections.singletonList("S256"));
        }

        queryParameters.put("client_id", Collections.singletonList(_config.getClientId()));
        queryParameters.put("response_type", Collections.singletonList("code"));
        queryParameters.put("scope", Collections.singletonList(_config.getScope()));

        if (additionalParameters != null)
        {
            for (Map.Entry<String, String> entry : additionalParameters.entrySet())
            {
                queryParameters.put(entry.getKey(), Collections.singletonList(entry.getValue()));
            }
        }

        _logger.debug("Created redirect to {} with query string arguments {}", _authorizationEndpoint,
                queryParameters);

        return _exceptionFactory.redirectException(_authorizationEndpoint, TEMPORARY_REDIRECT, queryParameters, false);
    }

    /**
     * Redeems the authorization code for tokens
     * The response will be validated as successful if the status code is 200
     *
     * @param code the authorization code from the AS
     *
     * @return TokenResponseAttributes object containing the access token, id token and refresh token if present
     */
    public TokenResponseAttributes redeemCodeForTokens(String code)
    {
        String redirectUri = createRedirectUri(_authenticatorInformationProvider, _exceptionFactory);

        HttpResponse tokenResponse = _httpClient
                .request(_tokenEndpoint)
                .contentType("application/x-www-form-urlencoded")
                .body(HttpRequest.createFormUrlEncodedBodyProcessor(createPostData(_config.getClientId(),
                        _config.getClientSecret().getValue(),
                        code, redirectUri)))
                .method(HttpMethod.POST.getMethodString())
                .response();
        int statusCode = tokenResponse.statusCode();

        if (statusCode != 200)
        {
            _logger.warn("Got error response from token endpoint: error = {}, {}", statusCode,
                    tokenResponse.body(HttpResponse.asString()));
            throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }

        return TokenResponseAttributes.fromMap(_json.fromJson(tokenResponse.body(HttpResponse.asString())));
    }

    private Map<String, String> createPostData(String clientId, String clientSecret, String code, String callbackUri)
    {
        var postData = new HashMap<>(Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "grant_type", "authorization_code",
                "redirect_uri", callbackUri
        ));

        if(_config.usePkce()) {
            String codeVerifier = _sessionManager.remove("code_verifier").getValueOfType(String.class);
            postData.put("code_verifier", codeVerifier);
        }

        return postData;
    }

    private static String createRedirectUri(AuthenticatorInformationProvider authenticatorInformationProvider,
                                            ExceptionFactory exceptionFactory)
    {
        try
        {
            URI authUri = authenticatorInformationProvider.getFullyQualifiedAuthenticationUri();

            return authUri.resolve(authUri.getPath() + "/" + REDIRECT_URI_ENDPOINT_SUFFIX).toURL().toString();
        }
        catch (MalformedURLException e)
        {
            throw exceptionFactory.internalServerException(ErrorCode.INVALID_REDIRECT_URI,
                    "Could not create redirect URI");
        }
    }
}
