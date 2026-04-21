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
     *
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
    public String getRefreshToken()
    {
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