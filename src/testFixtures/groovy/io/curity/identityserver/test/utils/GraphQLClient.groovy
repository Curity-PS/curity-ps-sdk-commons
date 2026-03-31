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

package io.curity.identityserver.plugin.integration

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.curity.identityserver.test.utils.crypto.TrustAllTrustManager

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class GraphQLClient {

    private final String graphqlUrl
    private final HttpClient httpClient

    GraphQLClient(String apiPath) {
        this.graphqlUrl = apiPath

        def sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, [new TrustAllTrustManager()] as TrustManager[], null)

        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build()
    }

    Map query(String query, Map variables = [:], String accessToken) {
        def body = JsonOutput.toJson([query: query, variables: variables])

        def requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(graphqlUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${accessToken}")
                .POST(HttpRequest.BodyPublishers.ofString(body))

        def response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw new RuntimeException("GraphQL request failed: ${response.body()}")
        }
        return new JsonSlurper().parseText(response.body()) as Map
    }

    Map getBucket(String userName, String purpose, String accessToken) {
        def query = """query ssoBuckets {
          bucketsByUserName(userName: "${userName}", purposes: "${purpose}") {
              attributes
          }
        }"""
        def result = query(query, [:], accessToken)
        def buckets = result.data?.bucketsByUserName
        assert buckets instanceof List
        assert buckets.size() == 1
        return buckets.first() as Map
    }

    void deleteBucket(String userName, String purpose, String accessToken) {
        def deleteQuery = """mutation deleteSsoBucket {
          deleteBucketByUserName(input: {userName: "${userName}", purpose: "${purpose}"}) {
            deleted
          }
        }"""
        def result = query(deleteQuery, [:], accessToken)
        assert result != null
    }
}