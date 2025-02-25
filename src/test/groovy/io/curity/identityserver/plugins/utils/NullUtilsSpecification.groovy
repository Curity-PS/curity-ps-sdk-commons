package io.curity.identityserver.plugins.utils

import se.curity.identityserver.sdk.attribute.Attribute
import spock.lang.Specification

import java.util.function.Consumer

class NullUtilsSpecification extends Specification {
    def "valueOrError returns value when not null with different types"() {
            when:
            def returnValue = NullUtils.valueOrError(value, "is null")

            then:
            value == returnValue
            value.class == returnValue.class

            where:
            value << [1, "string", new Object(), true, "null"]
        }

        def "valueOrError throws exception when null"() {
            when:
            NullUtils.valueOrError(value, "is null")

            then:
            thrown(NullPointerException)

            where:
            value << [null]
        }

        def "valueOrError with type returns value when not null"() {
            when:
            def returnValue = NullUtils.valueOrError(String, value, "is null")

            then:
            value == returnValue

            where:
            value << ["string"]
        }

        def "valueOrError with type throws exception when null"() {
            when:
            NullUtils.valueOrError(String, value, "is null")

            then:
            thrown(NullPointerException)

            where:
            value << [null]
        }

        def "ifNotNull executes consumer when value is not null"() {
            given:
            def consumer = Mock(Consumer)

            when:
            NullUtils.ifNotNull(value, consumer)

            then:
            1 * consumer.accept(value)

            where:
            value << [1, "string", new Object(), true]
        }

        def "ifNotNull does not execute consumer when value is null"() {
            given:
            def consumer = Mock(Consumer)

            when:
            NullUtils.ifNotNull(null, consumer)

            then:
            0 * consumer.accept(_)
        }

        def "mapOptionalAttribute returns transformed value when attribute is not null"() {
            given:
            def attribute = Attribute.of("name", "value")
            def transform = { attr -> "transformed" }

            when:
            def result = NullUtils.mapOptionalAttribute(attribute, transform, { "default" })

            then:
            result == "transformed"
        }

        def "mapOptionalAttribute returns default value when attribute is null"() {
            when:
            def result = NullUtils.mapOptionalAttribute(null, { attr -> "transformed" }, { "default" })

            then:
            result == "default"
        }

        def "map returns transformed value when value is not null"() {
            when:
            def result = NullUtils.map(value, { it.toString() })

            then:
            result == value.toString()

            where:
            value << [1, "string", new Object(), true]
        }

        def "map returns null when value is null"() {
            when:
            def result = NullUtils.map(null, { it.toString() })

            then:
            result == null
        }

        def "map with default returns transformed value when value is not null"() {
            when:
            def result = NullUtils.map(value, { it.toString() }, { "default" })

            then:
            result == value.toString()

            where:
            value << [1, "string", new Object(), true]
        }

        def "map with default returns default value when value is null"() {
            when:
            def result = NullUtils.map(null, { it.toString() }, { "default" })

            then:
            result == "default"
        }

        def "optionalValueOfType returns value when type matches"() {
            when:
            def result = NullUtils.optionalValueOfType(String, value)

            then:
            result == value

            where:
            value << ["string"]
        }

        def "optionalValueOfType returns null when type does not match"() {
            when:
            def result = NullUtils.optionalValueOfType(Integer, value)

            then:
            result == null

            where:
            value << ["string"]
        }

        def "valueOfType returns value when type matches"() {
            when:
            def result = NullUtils.valueOfType(String, value, "default")

            then:
            result == value

            where:
            value << ["string"]
        }

        def "valueOfType returns default value when type does not match"() {
            when:
            def result = NullUtils.valueOfType(Integer, value, 0)

            then:
            result == 0

            where:
            value << ["string"]
        }

        def "safeBoolean returns boolean value when input is boolean"() {
            when:
            def result = NullUtils.safeBoolean(value, false)

            then:
            result == value

            where:
            value << [true, false]
        }

        def "safeBoolean returns parsed boolean value when input is string"() {
            when:
            def result = NullUtils.safeBoolean(value, false)

            then:
            result == Boolean.parseBoolean(value)

            where:
            value << ["true", "false"]
        }

        def "safeBoolean returns default value when input is null"() {
            when:
            def result = NullUtils.safeBoolean(null, defaultValue)

            then:
            result == defaultValue

            where:
            defaultValue << [true, false]
        }

        def "valueOfTypeOrError returns value when type matches"() {
            when:
            def result = NullUtils.valueOfTypeOrError(String, value, "error")

            then:
            result == value

            where:
            value << ["string"]
        }

        def "valueOfTypeOrError throws exception when type does not match"() {
            when:
            NullUtils.valueOfTypeOrError(Integer, value, "error")

            then:
            thrown(IllegalArgumentException)

            where:
            value << ["string"]
        }
    }
