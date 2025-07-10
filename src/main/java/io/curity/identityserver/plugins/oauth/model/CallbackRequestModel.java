/*
 * Copyright 2025 Curity AB
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

package io.curity.identityserver.plugins.oauth.model;

import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.web.Request;

import java.util.function.Function;

public class CallbackRequestModel
{
    @Nullable
    private final String _error;
    @Nullable
    private final String _errorDescription;
    @Nullable
    private final String _code;
    @Nullable
    private final String _state;

    public CallbackRequestModel(Request request)
    {
        Function<String, ? extends RuntimeException> invalidParameter = (s) -> new RuntimeException(String.format(
                "Expected only one query string parameter named %s, but found multiple.", s));

        _code = request.getQueryParameterValueOrError("code", invalidParameter);
        _state = request.getQueryParameterValueOrError("state", invalidParameter);
        _error = request.getQueryParameterValueOrError("error", invalidParameter);
        _errorDescription = request.getQueryParameterValueOrError("error_description", invalidParameter);
    }

    @Nullable
    public String getCode()
    {
        return _code;
    }

    @Nullable
    public String getState()
    {
        return _state;
    }

    @Nullable
    public String getErrorDescription()
    {
        return _errorDescription;
    }

    @Nullable
    public String getError()
    {
        return _error;
    }
}
