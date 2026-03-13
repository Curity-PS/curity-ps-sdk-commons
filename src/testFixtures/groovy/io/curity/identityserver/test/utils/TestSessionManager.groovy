package io.curity.identityserver.test.utils

import se.curity.identityserver.sdk.attribute.Attribute
import se.curity.identityserver.sdk.service.SessionManager


class TestSessionManager implements SessionManager{
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
        return sessionAttributes.find {it.name.simpleName == key}
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
