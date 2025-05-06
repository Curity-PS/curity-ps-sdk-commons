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
