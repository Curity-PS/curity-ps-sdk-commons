package io.curity.identityserver.plugins.oauth;

import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attributes;

import java.util.Map;

public final class TokenResponseAttributes extends Attributes
{
    private TokenResponseAttributes(Attributes attributes)
    {
        super(attributes);
    }

    /**
     * Creates an object from a token response map
     *
     * @param map of validated token attributes
     * @return ValidatedJwtAttributes object
     */
    public static TokenResponseAttributes fromMap(Map<String, ?> map)
    {
        return new TokenResponseAttributes(Attributes.fromMap(map));
    }

    /**
     * Returns the access token from the token response.
     * @return The string value of the access token
     * @throws IllegalArgumentException if the access token is not present
     */
    public String getAccessToken()
    {
        return get("access_token").getValueOfType(String.class);
    }

    /**
     * Returns the id token from the token response.
     *
     * @return The string value of the refresh token or null if not present
     */
    @Nullable
    public String getIdToken()
    {
        return get("id_token").getValueOfType(String.class);
    }

    /**
     * Returns the refresh token from the token response.
     *
     * @return The string value of the refresh token or null if not present
     */
    @Nullable
    public String getRefreshToken() {
        return get("refresh_token").getValueOfType(String.class);
    }

    /**
     * Returns the value of the "error" claim in the token_response.
     *
     * @return The string value of the error
     * @throws IllegalArgumentException if the error claim is not present. Use #{isError()} to check for presence
     */
    public String getError()
    {
        return get("error").getValueOfType(String.class);
    }
}
