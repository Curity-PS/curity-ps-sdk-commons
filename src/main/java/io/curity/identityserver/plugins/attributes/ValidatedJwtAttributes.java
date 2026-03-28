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

import java.util.Map;

/**
 * A class that represents the attributes of a validated JWT.
 * It extends the Attributes class and provides additional methods to access specific claims in the JWT.
 * <p>
 * Specific getters are @Nullable since the validation can select to exclude any claim in the token
 */
public final class ValidatedJwtAttributes extends Attributes
{
    public ValidatedJwtAttributes(Attributes attributes)
    {
        super(attributes);
    }

    /**
     * Creates a ValidatedJwtAttributes object from a map of token attributes
     *
     * @param map of validated token attributes
     * @return ValidatedJwtAttributes object
     */
    public static ValidatedJwtAttributes fromMap(Map<String, ?> map)
    {
        return new ValidatedJwtAttributes(Attributes.fromMap(map));
    }

    /**
     * Returns the value of the "scope" claim in the JWT.
     *
     * @return scope string or null if not present
     */
    @Nullable
    public String getScope()
    {
        return NullUtils.map(get("scope"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the value of the "sub" claim in the JWT.
     *
     * @return subject string or null if not present
     */
    @Nullable
    public String getSubject()
    {
        return NullUtils.map(get("sub"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the value of the "aud" claim in the JWT.
     *
     * @return audience string or null if not present
     */
    @Nullable
    public String getAudience()
    {
        return NullUtils.map(get("aud"), attribute -> attribute.getOptionalValueOfType(String.class));
    }

    /**
     * Returns the value of the "iss" claim in the JWT.
     *
     * @return issuer string or null if not present
     */
    @Nullable
    public String getIssuer()
    {
        return NullUtils.map(get("iss"), attribute -> attribute.getOptionalValueOfType(String.class));
    }
}