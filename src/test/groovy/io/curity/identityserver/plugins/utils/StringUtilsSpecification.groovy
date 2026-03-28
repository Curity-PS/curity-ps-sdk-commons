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