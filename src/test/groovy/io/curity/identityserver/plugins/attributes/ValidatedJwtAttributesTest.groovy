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
