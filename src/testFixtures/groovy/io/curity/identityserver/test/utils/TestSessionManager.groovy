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

import se.curity.identityserver.sdk.attribute.Attribute
import se.curity.identityserver.sdk.service.SessionManager


class TestSessionManager implements SessionManager {
    List<Attribute> sessionAttributes = []

    @Override
    void put(Attribute data) {
        sessionAttributes.add(data)
    }

    @Override
    Attribute remove(String key) {
        Attribute attributeToRemove = get(key)
        sessionAttributes.remove(attributeToRemove)
        return attributeToRemove
    }

    @Override
    Attribute get(String key) {
        return sessionAttributes.find { it.name.simpleName == key }
    }

    @Override
    String getSessionId() {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    void putIntoSession(String sessionId, Attribute data) {
        throw new UnsupportedOperationException("Not implemented")
    }

    @Override
    Attribute getFromSession(String sessionId, String key) {
        throw new UnsupportedOperationException("Not implemented")
    }
}