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

package io.curity.identityserver.test.utils

import io.curity.identityserver.test.utils.crypto.TrustAllTrustManager
import org.jose4j.json.JsonUtil

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A test client for the Curity Identity Server's User Management GraphQL API.
 *
 * <p>Authenticates using the OAuth 2.0 client credentials grant via a
 * {@link TestOAuthClient}. The access token is fetched lazily on the first
 * request and reused for all subsequent calls.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * def oauth = TestOAuthClient.clientCredentialsClient("graphql-admin", "secret", tokenUrl, "um-admin")
 * def graphql = new GraphQLClient(graphqlUrl, oauth)
 *
 * def result = graphql.query("query { accounts { edges { node { id } } } }")
 * def bucket = graphql.getBucket("testuser", "tokens")
 * </pre>
 */
class GraphQLClient {

    private final String graphqlUrl
    private final HttpClient httpClient
    private final TestOAuthClient oauthClient
    private String accessToken

    /**
     * @param graphqlUrl the URL of the GraphQL endpoint
     * @param oauthClient an OAuth client configured for the client credentials grant
     */
    GraphQLClient(String graphqlUrl, TestOAuthClient oauthClient) {
        this.graphqlUrl = graphqlUrl
        this.oauthClient = oauthClient

        def sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, [new TrustAllTrustManager()] as TrustManager[], null)

        this.httpClient = HttpClient.newBuilder()
            .sslContext(sslContext)
            .build()
    }

    private String getToken() {
        if (accessToken == null) {
            accessToken = oauthClient.clientCredentials().accessToken
        }
        return accessToken
    }

    /**
     * Execute a GraphQL query or mutation.
     *
     * @param query the GraphQL query or mutation string
     * @param variables optional variables map (defaults to empty)
     * @return the parsed JSON response as a map
     * @throws RuntimeException if the server returns a non-200 status
     */
    Map query(String query, Map variables = [:]) {
        def body = JsonUtil.toJson([query: query, variables: variables])

        def request = HttpRequest.newBuilder()
            .uri(URI.create(graphqlUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${getToken()}")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        def response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw new RuntimeException("GraphQL request failed: ${response.body()}")
        }
        return JsonUtil.parseJson(response.body())
    }

    /**
     * Fetch a single SSO bucket by user name and purpose.
     *
     * @param userName the account user name
     * @param purpose the bucket purpose (e.g. {@code "tokens"})
     * @return the bucket as a map containing an {@code attributes} key
     */
    Map getBucket(String userName, String purpose) {
        def query = """query ssoBuckets {
          bucketsByUserName(userName: "${userName}", purposes: "${purpose}") {
              attributes
          }
        }"""
        def result = query(query)
        def buckets = result.data?.bucketsByUserName
        assert buckets instanceof List
        assert buckets.size() == 1
        return buckets.first() as Map
    }

    /**
     * Delete an SSO bucket by user name and purpose.
     *
     * @param userName the account user name
     * @param purpose the bucket purpose
     */
    void deleteBucket(String userName, String purpose) {
        def deleteQuery = """mutation deleteSsoBucket {
          deleteBucketByUserName(input: {userName: "${userName}", purpose: "${purpose}"}) {
            deleted
          }
        }"""
        def result = query(deleteQuery)
        assert result != null
    }
}
