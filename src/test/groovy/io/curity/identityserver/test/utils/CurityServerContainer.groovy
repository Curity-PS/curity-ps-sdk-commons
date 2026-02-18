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


import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.images.builder.ImageFromDockerfile

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
/**
 * Testcontainer for Curity Identity Server with plugin pre-installed.
 *
 * <p>Builds a Docker image that layers a plugin release folder on top of the
 * official Curity Identity Server image. A bundled {@code base-config.xml}
 * (shipped on the classpath of this library) is always copied into
 * {@code /opt/idsvr/etc/init/}. Callers can supply an additional
 * configuration XML that is placed alongside it in the same directory so
 * that the server merges both files on startup.</p>
 *
 * <p>The {@code LICENSE_KEY} environment variable is forwarded from the host
 * into the container so that the configuration can use the
 * {@code #{LICENSE_KEY}} placeholder.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * // Base configuration only – no plugin, latest image
 * def container = new CurityServerContainer()
 * container.start()
 *
 * // Plugin folder only – uses base-config.xml from the library, latest image
 * def container = new CurityServerContainer("build/my-plugin")
 *
 * // Plugin folder only – specific version
 * def container = new CurityServerContainer("build/my-plugin", "2025.1.0")
 *
 * // Plugin folder + additional config overlay, latest image
 * def container = new CurityServerContainer(
 *     "src/test/resources/my-plugin-config.xml",
 *     "build/my-plugin"
 * )
 *
 * // Plugin folder + additional config overlay + specific version
 * def container = new CurityServerContainer(
 *     "src/test/resources/my-plugin-config.xml",
 *     "build/my-plugin",
 *     "2025.1.0"
 * )
 * container.start()
 *
 * def adminUrl   = container.adminUrl   // https://localhost:&lt;mapped-port&gt;
 * def runtimeUrl = container.runtimeUrl // https://localhost:&lt;mapped-port&gt;
 * </pre>
 */
class CurityServerContainer extends GenericContainer<CurityServerContainer> {

    private static final def logger = LoggerFactory.getLogger(CurityServerContainer.class)
    private static final int ADMIN_PORT = 6749
    private static final int RUNTIME_PORT = 8443
    private static final int STATUS_PORT = 4465
    private static final String BASE_IMAGE_REPOSITORY = "curity.azurecr.io/curity/idsvr"
    private static final String DEFAULT_BASE_IMAGE = "$BASE_IMAGE_REPOSITORY:latest"
    private static final String BASE_CONFIG_RESOURCE = "base-config.xml"

    private static String imageForVersion(String version) {
        version ? "$BASE_IMAGE_REPOSITORY:$version" : DEFAULT_BASE_IMAGE
    }

    /**
     * Create a new Curity Server container with only the base configuration.
     * No plugin or extra configuration is included.
     */
    CurityServerContainer() {
        this(null as Path, null as Path, null as String)
    }

    /**
     * Create a new Curity Server container with only the base configuration.
     * No plugin or extra configuration is included.
     * @param version          Curity Identity Server version tag (e.g. {@code "11.0"}).
     *                         If {@code null} or omitted, {@code latest} is used.
     */
    CurityServerContainer(String version) {
        this(null as Path, null as Path, version)
    }

    /**
     * Create a new Curity Server container with only a plugin folder.
     * Uses the bundled base-config.xml.
     *
     * @param pluginFolderPath Path to the plugin release folder
     * @param version          Curity Identity Server version tag (e.g. {@code "11.0"}).
     *                         If {@code null} or omitted, {@code latest} is used.
     */
    CurityServerContainer(String pluginFolderPath, String version) {
        this(null as Path, Paths.get(pluginFolderPath), version)
    }

    /**
     * Create a new Curity Server container with an additional config overlay.
     *
     * @param extraConfigXmlPath Path to an additional configuration XML file
     * @param pluginFolderPath   Path to the plugin release folder
     * @param version            Curity Identity Server version tag (e.g. {@code "11.0"}).
     *                           If {@code null} or omitted, {@code latest} is used.
     */
    CurityServerContainer(String extraConfigXmlPath, String pluginFolderPath, String version) {
        this(Paths.get(extraConfigXmlPath), Paths.get(pluginFolderPath), version)
    }

    /**
     * Create a new Curity Server container.
     *
     * @param extraConfigXml Path to an additional configuration XML (may be {@code null})
     * @param pluginFolder   Path to the plugin release folder
     * @param version        Curity Identity Server version tag (e.g. {@code "11.0"}).
     *                       If {@code null}, {@code latest} is used.
     */
    CurityServerContainer(Path extraConfigXml, Path pluginFolder, String version) {
        super(buildImage(extraConfigXml, pluginFolder, imageForVersion(version)))

        withExposedPorts(ADMIN_PORT, RUNTIME_PORT, STATUS_PORT)

        // Pass the license key into the container for config parameterization (if set)
        def licenseKey = System.getenv("LICENSE_KEY")
        if (licenseKey != null) {
            withEnv("LICENSE_KEY", "<license-key>$licenseKey</license-key>")
        } else {
            withEnv("LICENSE_KEY", "")
        }

        // Stream container logs to test output
        withLogConsumer(new Slf4jLogConsumer(logger).withSeparateOutputStreams())

        // Wait for the status endpoint to report the node is ready (fail fast on ERROR)
        waitingFor(new CurityStatusWaitStrategy(STATUS_PORT)
                .withStartupTimeout(Duration.ofSeconds(120)))
    }

    /**
     * Build the Docker image with plugin and configuration.
     */
    private static ImageFromDockerfile buildImage(Path extraConfigXml, Path pluginFolder, String baseImage) {
        def baseConfigContent = readBaseConfig()
        def baseConfigTemp = createTempFile(baseConfigContent, "base-config", ".xml")

        def image = new ImageFromDockerfile()
                .withDockerfileFromBuilder { builder ->
                    def b = builder.from(baseImage)
                            .user("root")
                            .copy("base-config.xml", "/opt/idsvr/etc/init/base-config.xml")

                    if (extraConfigXml != null) {
                        b.copy("extra-config.xml", "/opt/idsvr/etc/init/extra-config.xml")
                    }

                    if (pluginFolder != null) {
                        def folderName = pluginFolder.getName(pluginFolder.nameCount - 1)
                        b.copy("plugin-release/", "/opt/idsvr/usr/share/plugins/$folderName")
                    }

                    b.run("chown -R idsvr:idsvr /opt/idsvr/etc/init /opt/idsvr/usr/share/plugins")
                            .user("idsvr")
                            .env("LOGGING_LEVEL", "DEBUG")
                            .env("SERVICE_ROLE", "default")
                            .env("ADMIN", "true")
                            .build()
                }
                .withFileFromPath("base-config.xml", baseConfigTemp)

        if (pluginFolder != null) {
            image.withFileFromPath("plugin-release", pluginFolder)
        }

        if (extraConfigXml != null) {
            image.withFileFromPath("extra-config.xml", extraConfigXml)
        }

        return image
    }

    /**
     * Create a temporary file with the given content.
     */
    private static Path createTempFile(String content, String prefix, String suffix) {
        def tempFile = File.createTempFile(prefix, suffix)
        tempFile.deleteOnExit()
        tempFile.write(content, StandardCharsets.UTF_8.name())
        return tempFile.toPath()
    }

    /**
     * Read the bundled base-config.xml from the classpath as a string.
     */
    private static String readBaseConfig() {
        def resource = CurityServerContainer.class.getClassLoader().getResourceAsStream(BASE_CONFIG_RESOURCE)
        if (resource == null) {
            throw new IllegalStateException(
                "Could not find ${BASE_CONFIG_RESOURCE} on the classpath. " +
                "Ensure the curity-ps-sdk-commons tests JAR is on the test classpath."
            )
        }

        resource.withCloseable { input ->
            new String(input.readAllBytes(), StandardCharsets.UTF_8)
        }
    }

    /**
     * Get the base URL for the admin API.
     *
     * @return Admin API URL (e.g., https://localhost:32768)
     */
    String getAdminUrl() {
        "https://${host}:${getMappedPort(ADMIN_PORT)}"
    }

    /**
     * Get the base URL for the runtime endpoints.
     *
     * @return Runtime URL (e.g., https://localhost:32769)
     */
    String getRuntimeUrl() {
        "https://${host}:${getMappedPort(RUNTIME_PORT)}"
    }

    /**
     * Get the mapped admin port.
     *
     * @return The host port mapped to the container's admin port
     */
    int getAdminPort() {
        getMappedPort(ADMIN_PORT)
    }

    /**
     * Get the mapped runtime port.
     *
     * @return The host port mapped to the container's runtime port
     */
    int getRuntimePort() {
        getMappedPort(RUNTIME_PORT)
    }

    @Override
    void close() {
        super.close()
    }
}
