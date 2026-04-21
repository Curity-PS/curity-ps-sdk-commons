/*
 *  Copyright 2026 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.test.utils

import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy

import java.time.Duration
import java.time.Instant

/**
 * Wait strategy that polls the Curity Identity Server status endpoint ({@code GET /} on the
 * status port, default 4465) and inspects the JSON response.
 *
 * <ul>
 *   <li>Succeeds when {@code isReady} is {@code true}.</li>
 *   <li>Fails fast when {@code nodeState} is {@code "ERROR"}.</li>
 *   <li>Throws {@link ContainerLaunchException} if the timeout expires before the node becomes ready.</li>
 * </ul>
 *
 * @see <a href="https://curity.io/docs/identity-server/system-and-operation/operation-and-monitoring/status-endpoint/">
 *     Status Endpoint documentation</a>
 */
class CurityStatusWaitStrategy extends AbstractWaitStrategy {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2)

    private static final TestJson json = new TestJson()

    private final int statusPort

    private boolean hadConnection = false

    CurityStatusWaitStrategy(int statusPort) {
        this.statusPort = statusPort
    }

    @Override
    protected void waitUntilReady() {
        hadConnection = false
        def deadline = Instant.now() + startupTimeout

        while (Instant.now() < deadline) {
            try {
                def mappedPort = waitStrategyTarget.getMappedPort(statusPort)
                def host = waitStrategyTarget.host
                def url = new URL("http://${host}:${mappedPort}/")
                def connection = (HttpURLConnection) url.openConnection()
                connection.requestMethod = "GET"
                connection.connectTimeout = 2000
                connection.readTimeout = 2000

                try {
                    def responseCode = connection.responseCode
                    hadConnection = true
                    def body = (responseCode >= 400)
                            ? connection.errorStream?.text
                            : connection.inputStream?.text

                    if (body) {
                        def nodeState = extractJsonString(body, "nodeState")

                        if (nodeState == "ERROR" || nodeState == "STOPPING") {
                            throw new ContainerLaunchException(
                                    "Curity Identity Server entered $nodeState state. Status response: ${body}")
                        }

                        if (extractJsonBoolean(body, "isReady")) {
                            return
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (ContainerLaunchException e) {
                throw e
            } catch (Exception ignored) {
                if (hadConnection) {
                    throw new ContainerLaunchException(
                            "Lost connection to Curity Identity Server")
                }
            }

            try {
                Thread.sleep(POLL_INTERVAL.toMillis())
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt()
                throw new ContainerLaunchException("Curity Identity Server wait interrupted", e)
            }
        }

        throw new ContainerLaunchException(
                "Timed out waiting for Curity Identity Server to become ready after ${startupTimeout}")
    }

    /**
     * Extract a string value from a JSON field, e.g. {@code "nodeState": "RUNNING"} → {@code "RUNNING"}.
     */
    private static String extractJsonString(String jsonString, String field) {
        def map = json.fromJson(jsonString)
        return map[field]?.toString()
    }

    /**
     * Extract a boolean value from a JSON field, e.g. {@code "isReady": true} → {@code true}.
     */
    private static boolean extractJsonBoolean(String json, String field) {
        def matcher = json =~ /"${field}"\s*:\s*(true|false)/
        matcher.find() ? matcher.group(1) == "true" : false
    }
}
