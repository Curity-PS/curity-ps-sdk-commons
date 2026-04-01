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

package io.curity.identityserver.plugins.integration

import io.curity.identityserver.test.utils.GraphQLClient
import spock.lang.Shared

import static io.curity.identityserver.test.utils.constants.TestConstants.GraphQL.UM_ENDPOINT

class GraphQLClientIntegrationSpec extends CurityServerContainerIntegrationSpec {

    @Shared
    GraphQLClient client

    def setupSpec() {
        client = new GraphQLClient("${container.runtimeUrl}${UM_ENDPOINT}",
            container.tokenEndpointUrl)
    }

    def cleanupSpec() {
        client?.close()
    }

    def "client is able to query a non-existing bucket"() {
        when: "querying for a bucket that does not exist"
        def result = client.getBucket("foo", "bar")

        then: "the result has empty attributes"
        result.attributes instanceof Map
        (result.attributes as Map).isEmpty()
    }

    def "client is able to create a bucket and read it"() {
        given:
        def attributes = [foo: "bar"]
        def purpose = "hello"
        def userName = "richie"

        when: "Creating a bucket"
        def stored = client.storeBucket(userName, purpose, attributes)

        then: "its stored"
        stored

        when: "reading the same bucket"
        def result = client.getBucket(userName, purpose)

        then: "the result is the added attributes"
        result.attributes == attributes
    }

    def "client is able to delete a bucket"() {
        given: "A bucket that exists"
        def userName = "delete-me"
        def purpose = "test"
        client.storeBucket(userName, purpose, [key: "value"])

        when: "deleting the bucket"
        def deleted = client.deleteBucket(userName, purpose)

        then: "the deletion succeeds"
        deleted

        and: "the bucket is gone"
        def result = client.getBucket(userName, purpose)
        result.attributes instanceof Map
        (result.attributes as Map).isEmpty()
    }

    def "client is able to update existing bucket attributes"() {
        given: "A bucket with initial attributes"
        def userName = "update-me"
        def purpose = "test"
        client.storeBucket(userName, purpose, [color: "red", size: "large"])

        when: "updating the bucket with new attributes"
        def stored = client.storeBucket(userName, purpose, [color: "blue", size: "large"])

        then: "the store succeeds"
        stored

        and: "the bucket reflects the updated attributes"
        def result = client.getBucket(userName, purpose)
        result.attributes.color == "blue"
        result.attributes.size == "large"
    }
}
