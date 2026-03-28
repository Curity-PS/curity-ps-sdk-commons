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

package io.curity.identityserver.plugins.jwt;

import org.jose4j.http.SimpleGet;
import org.jose4j.http.SimpleResponse;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.HttpClient;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class CustomSimpleGet implements SimpleGet
{
    private final HttpClient _httpClient;

    public CustomSimpleGet(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    @Override
    public SimpleResponse get(String url)
    {
        var response = _httpClient.request(URI.create(url)).get().response();
        var body = response.body(HttpResponse.asString());
        var headers = response.headers().map();

        return new SimpleResponse()
        {
            @Override
            public int getStatusCode()
            {
                return response.statusCode();
            }

            @Override
            public String getStatusMessage()
            {
                return null;
            }

            @Override
            public List<String> getHeaderValues(String name)
            {
                return headers.getOrDefault(name, Collections.emptyList());
            }

            @Override
            public String getBody()
            {
                return body;
            }

            @Override
            public Collection<String> getHeaderNames()
            {
                return headers.keySet();
            }
        };
    }
}