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
package io.curity.identityserver.test.utils.crypto

import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * An {@link X509ExtendedKeyManager} that returns client certificates from a keystore
 * regardless of whether they have expired. The default JDK key manager silently skips
 * expired certificates during TLS handshake, which makes it impossible to test with
 * expired client certificates. This manager bypasses that check.
 *
 * <p>This class is intended for <strong>test use only</strong>.</p>
 */
class AcceptExpiredKeyManager extends X509ExtendedKeyManager {

    private final KeyStore keyStore
    private final char[] password

    AcceptExpiredKeyManager(KeyStore keyStore, char[] password) {
        this.keyStore = keyStore
        this.password = password
    }

    static AcceptExpiredKeyManager fromKeyStore(URL keystoreUrl, String keystorePassword, String keystoreType) {
        def ks = KeyStore.getInstance(keystoreType)
        keystoreUrl.openStream().withCloseable { input ->
            ks.load(input, keystorePassword.toCharArray())
        }
        return new AcceptExpiredKeyManager(ks, keystorePassword.toCharArray())
    }

    private String findFirstPrivateKeyAlias() {
        def aliases = keyStore.aliases()
        while (aliases.hasMoreElements()) {
            def alias = aliases.nextElement()
            if (keyStore.isKeyEntry(alias)) {
                return alias
            }
        }
        return null
    }

    @Override
    String[] getClientAliases(String keyType, Principal[] issuers) {
        def alias = findFirstPrivateKeyAlias()
        return alias != null ? [alias] as String[] : null
    }

    @Override
    String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return findFirstPrivateKeyAlias()
    }

    @Override
    String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return findFirstPrivateKeyAlias()
    }

    @Override
    X509Certificate[] getCertificateChain(String alias) {
        return keyStore.getCertificateChain(alias) as X509Certificate[]
    }

    @Override
    PrivateKey getPrivateKey(String alias) {
        return keyStore.getKey(alias, password) as PrivateKey
    }

    @Override
    String[] getServerAliases(String keyType, Principal[] issuers) {
        return null
    }

    @Override
    String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return null
    }
}
