package io.curity.identityserver.test.utils

import org.jose4j.json.JsonUtil
import se.curity.identityserver.sdk.attribute.Attributes
import se.curity.identityserver.sdk.service.Json

final class TestJson implements Json {

    @Override
    String toJson(Map<?, ?> object) {
        return JsonUtil.toJson(object)
    }

    @Override
    String toJson(Map<?, ?> object, boolean includeNulls) {
        return JsonUtil.toJson(object)
    }

    @Override
    String toJson(Object object) {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    String toJson(Object object, boolean includeNulls) {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    <T> T fromJson(String json, Class<T> type) {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    Map<String, Object> fromJson(String json) {
        return JsonUtil.parseJson(json)
    }

    @Override
    Map<String, Object> fromJson(String json, boolean includeNulls) {
        if (!includeNulls) {
            throw new UnsupportedOperationException("Not implemented")
        }
        return JsonUtil.parseJson(json)
    }

    @Override
    List<?> fromJsonArray(String jsonArray) {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    List<?> fromJsonArray(String jsonArray, boolean includeNulls) {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    Attributes toAttributes(String json) {
        def map = JsonUtil.parseJson(json)
        return Attributes.fromMap(map)
    }

    @Override
    String fromAttributes(Attributes attributes) {
        return JsonUtil.toJson(attributes.asMap())
    }
}
