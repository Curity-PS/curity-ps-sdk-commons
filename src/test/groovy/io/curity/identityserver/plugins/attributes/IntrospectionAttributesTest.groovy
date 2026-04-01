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

package io.curity.identityserver.plugins.attributes

import spock.lang.Specification

class IntrospectionAttributesTest extends Specification {

    def "Attributes object can be parsed from a full introspection response map"() {
        given: "A map of introspection response attributes"
        def attributesMap = [
                "active"    : true,
                "sub"       : "johndoe",
                "scope"     : "read write",
                "client_id" : "my-client",
                "iss"       : "https://issuer.example.com",
                "aud"       : "api.example.com",
                "token_type": "bearer",
                "exp"       : 1735689600L
        ]

        when: "The map is converted to an IntrospectionAttributes object"
        def attributes = IntrospectionAttributes.fromMap(attributesMap)

        then: "All typed getters return the expected values"
        attributes.isActive()
        attributes.subject == "johndoe"
        attributes.scope == "read write"
        attributes.clientId == "my-client"
        attributes.issuer == "https://issuer.example.com"
        attributes.audiences == ["api.example.com"]
        attributes.tokenType == "bearer"
        attributes.expiration == 1735689600L
    }

    def "isActive returns false when active claim is missing"() {
        given:
        def attributes = IntrospectionAttributes.fromMap(["sub": "johndoe"])

        expect:
        !attributes.isActive()
    }

    def "isActive returns false when active claim is false"() {
        given:
        def attributes = IntrospectionAttributes.fromMap(["active": false])

        expect:
        !attributes.isActive()
    }

    def "isActive handles string 'true' via safeBoolean"() {
        given:
        def attributes = IntrospectionAttributes.fromMap(["active": "true"])

        expect:
        attributes.isActive()
    }

    def "Missing claims return null"() {
        given: "An attributes object with only the active claim"
        def attributes = IntrospectionAttributes.fromMap(["active": true])

        expect:
        attributes.subject == null
        attributes.scope == null
        attributes.clientId == null
        attributes.issuer == null
        attributes.audiences == []
        attributes.tokenType == null
        attributes.expiration == null
    }

    def "getAudiences returns single-element list when aud is a string"() {
        given:
        def attributes = IntrospectionAttributes.fromMap(["aud": "single-audience"])

        expect:
        attributes.audiences == ["single-audience"]
    }

    def "getAudiences returns full list when aud is a list"() {
        given:
        def attributes = IntrospectionAttributes.fromMap(["aud": ["aud1", "aud2", "aud3"]])

        expect:
        attributes.audiences == ["aud1", "aud2", "aud3"]
    }

    def "getAudiences returns empty list when aud is missing"() {
        given:
        def attributes = IntrospectionAttributes.fromMap(["active": true])

        expect:
        attributes.audiences == []
    }

    def "Wrong type for string claim returns null"() {
        given: "An attributes object where sub is an integer instead of a string"
        def attributes = IntrospectionAttributes.fromMap(["sub": 12345, "scope": true])

        expect:
        attributes.subject == null
        attributes.scope == null
    }
}
