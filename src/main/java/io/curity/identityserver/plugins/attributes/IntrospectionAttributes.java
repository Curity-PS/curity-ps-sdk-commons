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

package io.curity.identityserver.plugins.attributes;

import io.curity.identityserver.plugins.utils.NullUtils;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attributes;

import java.util.List;
import java.util.Map;

/**
 * A class that represents the attributes of a validated introspection response (RFC 7662).
 * It extends the Attributes class and provides additional methods to access specific claims
 * in the introspection response.
 * <p>
 * Specific getters are @Nullable since not all claims are guaranteed to be present in the response.
 */
public final class IntrospectionAttributes extends Attributes
{
    public IntrospectionAttributes(Attributes attributes)
    {
        super(attributes);
    }

    /**
     * Creates an IntrospectionAttributes object from a map of introspection response attributes
     *
     * @param map of introspection response attributes
     * @return IntrospectionAttributes object
     */
    public static IntrospectionAttributes fromMap(Map<String, ?> map)
    {
        return new IntrospectionAttributes(Attributes.fromMap(map));
    }

    /**
     * Returns the value of the "active" claim in the introspection response.
     *
     * @return true if the token is active, false otherwise
     */
    public boolean isActive()
    {
        return NullUtils.safeBoolean(
                NullUtils.map(get("active"), attribute -> attribute.getOptionalValueOfType(Boolean.class)),
                false);
    }

    /**
     * Returns the value of the "sub" claim in the introspection response.
     *
     * @return subject string or null if not present
     */
    @Nullable
    public String getSubject()
    {
        return NullUtils.map(get("sub"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the value of the "scope" claim in the introspection response.
     *
     * @return scope string or null if not present
     */
    @Nullable
    public String getScope()
    {
        return NullUtils.map(get("scope"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the value of the "client_id" claim in the introspection response.
     *
     * @return client ID string or null if not present
     */
    @Nullable
    public String getClientId()
    {
        return NullUtils.map(get("client_id"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the value of the "iss" claim in the introspection response.
     *
     * @return issuer string
     */
    public String getIssuer()
    {
        return NullUtils.map(get("iss"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the values of the "aud" claim in the introspection response as a list.
     * Handles both a single string audience and a list of audiences.
     *
     * @return list of audience strings, or null if not present
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public List<String> getAudiences()
    {
        return NullUtils.map(get("aud"), attribute -> {
            List<String> list = attribute.getOptionalValueOfType(List.class);
            if (list != null)
            {
                return list;
            }
            String single = attribute.getOptionalValueOfType(String.class);
            return single != null ? List.of(single) : null;
        });
    }

    /**
     * Returns the value of the "token_type" claim in the introspection response.
     *
     * @return token type string or null if not present
     */
    @Nullable
    public String getTokenType()
    {
        return NullUtils.map(get("token_type"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the value of the "exp" claim in the introspection response.
     *
     * @return expiration timestamp or null if not present
     */
    @Nullable
    public Long getExpiration()
    {
        return NullUtils.map(get("exp"), attribute -> attribute.getOptionalValueOfType(Long.class));
    }
}
