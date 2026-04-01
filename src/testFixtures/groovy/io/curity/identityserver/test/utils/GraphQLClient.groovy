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

import io.curity.identityserver.test.utils.crypto.InsecureSslContext
import org.jose4j.json.JsonUtil

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static io.curity.identityserver.test.utils.constants.TestConstants.GraphQL.*

/**
 * A test client for the Curity Identity Server's User Management GraphQL API.
 *
 * <p>Authenticates using the OAuth 2.0 client credentials grant via a
 * {@link TestOAuthClient}. The access token is fetched lazily on the first
 * request and reused for all subsequent calls.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * def graphql = new GraphQLClient(graphqlUrl, tokenUrl)
 *
 * def result = graphql.query("query { accounts { edges { node { id } } } }")
 * def bucket = graphql.getBucket("testuser", "tokens")
 * </pre>
 */
class GraphQLClient implements Closeable {

    private final String graphqlUrl
    private final HttpClient httpClient
    private final TestOAuthClient oauthClient
    private String accessToken

    /**
     * @param graphqlUrl the URL of the GraphQL endpoint
     * @param tokenEndpointUrl the token endpoint URL for client credentials authentication
     */
    GraphQLClient(String graphqlUrl, String tokenEndpointUrl) {
        this.graphqlUrl = graphqlUrl
        this.oauthClient = TestOAuthClient.clientCredentialsClient(CLIENT_ID, CLIENT_SECRET,
            tokenEndpointUrl, ADMIN_SCOPE)

        this.httpClient = HttpClient.newBuilder()
            .sslContext(InsecureSslContext.instance)
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
            throw new RuntimeException("GraphQL request failed with status ${response.statusCode()}: ${response.body()}")
        }
        def parsed = JsonUtil.parseJson(response.body())
        if (parsed.errors) {
            def messages = (parsed.errors as List).collect { (it as Map).message }.join("; ")
            throw new RuntimeException("GraphQL errors: ${messages}")
        }
        return parsed
    }

    /**
     * Fetch a single SSO bucket by user name and purpose.
     *
     * @param userName the account user name
     * @param purpose the bucket purpose (e.g. {@code "tokens"})
     * @return the bucket as a map containing an {@code attributes} key, or an empty map if no buckets are found
     */
    Map getBucket(String userName, String purpose) {
        def gql = '''
            query ssoBuckets($userName: String!, $purposes: [String!]!) {
                bucketsByUserName(userName: $userName, purposes: $purposes) {
                    attributes
                }
            }'''
        def result = query(gql, [userName: userName, purposes: [purpose]])
        return getFirstBucketOrEmpty(result, "bucketsByUserName")
    }

    /**
     * Delete an SSO bucket by user name and purpose.
     *
     * @param userName the account user name
     * @param purpose the bucket purpose
     * @return {@code true} if the bucket was deleted
     */
    boolean deleteBucket(String userName, String purpose) {
        def gql = '''
            mutation deleteSsoBucket($input: DeleteBucketByUserNameInput!) {
                deleteBucketByUserName(input: $input) {
                    deleted
                }
            }'''
        def result = query(gql, [input: [userName: userName, purpose: purpose]])
        return result.data?.deleteBucketByUserName?.deleted == true
    }

    /**
     * Create or update an SSO bucket for a user.
     *
     * @param userName the account user name
     * @param purpose the bucket purpose
     * @param attributes the bucket attributes to store
     * @return {@code true} if the bucket was stored successfully
     */
    boolean storeBucket(String userName, String purpose, Map attributes) {
        def gql = '''
            mutation storeSsoBucket($input: StoreBucketByUserNameInput!) {
                storeBucketByUserName(input: $input) {
                    stored
                }
            }'''
        def result = query(gql, [input: [userName: userName, purpose: purpose, attributes: attributes]])
        return result.data?.storeBucketByUserName?.stored == true
    }

    @Override
    void close() {
        oauthClient.close()
        httpClient.close()
    }

    private static Map getFirstBucketOrEmpty(Map<String, Object> result, String queryName) {
        def buckets = result.data?[queryName]
        if (!(buckets instanceof List) || buckets.isEmpty()) {
            return Map.of()
        }

        return buckets.first() as Map
    }

}
