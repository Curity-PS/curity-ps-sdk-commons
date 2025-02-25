package io.curity.identityserver.plugins.utils


import spock.lang.Specification

class StringUtilsSpecification extends Specification {

    def "isValidBoolean should return true for valid boolean strings"() {
        expect:
        StringUtils.isValidBoolean(input) == expected

        where:
        input   | expected
        "true"  | true
        "false" | true
        "True"  | false
        "False" | false
        "TRUE"  | false
        "FALSE" | false
        "t"     | false
        "f"     | false
        "yes"   | false
        "no"    | false
        "1"     | false
        "0"     | false
        ""      | false
        null    | false
    }
}
