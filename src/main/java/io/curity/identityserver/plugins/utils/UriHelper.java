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

package io.curity.identityserver.plugins.utils;

import java.net.URI;

public final class UriHelper
{
    private UriHelper() {

    }

    /**
     * Append a path segment to a URI, ensuring there is exactly one '/' between them.
     *
     * @param uri URI to append to
     * @param path Path segment to append
     * @return New URI with the appended path
     */
    public static URI appendPath(URI uri, String path)
    {
        String uriString = uri.toString();
        if (!uriString.endsWith("/"))
        {
            uriString += "/";
        }

        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        return URI.create(uriString).resolve(path);
    }
}
