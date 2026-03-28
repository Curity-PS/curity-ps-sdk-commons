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