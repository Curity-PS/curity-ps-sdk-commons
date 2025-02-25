/*
 *  Copyright 2023 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugins.utils;

import se.curity.identityserver.sdk.Nullable;

/**
 * Utility functions around string handling and validation
 */
public final class StringUtils
{
    private StringUtils()
    {
    }

    /**
     * Test whether a string value represents a valid boolean value, i.e. it is either "true" or "false".
     *
     * @param stringValue string value to test
     * @return true when it's a valid boolean value
     */
    public static boolean isValidBoolean(@Nullable String stringValue)
    {
        return "true".equals(stringValue) ||
                "false".equals(stringValue);
    }
}