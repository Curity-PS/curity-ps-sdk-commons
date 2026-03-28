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

class ValidatedJwtAttributesTest extends Specification {
    def "Attributes object can be parsed from map"() {
        given: "A map of attributes"
        def attributesMap = [
                "sub": "johndoe",
                "name": "John Doe",
                "scope": "read",
                "aud": "test-audience",
                "iat": 12341234
        ]

        when: "The map is converted to an attributes object"
        def jwtAttributes = ValidatedJwtAttributes.fromMap(attributesMap)

        then: "The attributes object is not null and contains the expected values"
        jwtAttributes.subject == 'johndoe'
        jwtAttributes.scope == 'read'
        jwtAttributes.audience == 'test-audience'
        jwtAttributes.name?.getOptionalValueOfType(String.class) == 'John Doe'
        jwtAttributes.iat?.getOptionalValueOfType(Long.class) == 12341234
    }
}