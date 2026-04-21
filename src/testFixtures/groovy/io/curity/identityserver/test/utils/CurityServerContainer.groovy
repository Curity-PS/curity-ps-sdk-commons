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
 * Testcontainer for Curity Identity Server with plugins pre-installed.
 *
 * <p>Builds a Docker image that layers plugin release folders on top of the
 * official Curity Identity Server image. A bundled {@code base-config.xml}
 * (shipped on the classpath of this library) is always copied into
 * {@code /opt/idsvr/etc/init/}. Callers can supply additional
 * configuration XML files that are placed alongside it in the same directory so
 * that the server merges all files on startup.</p>
 *
 * <p>The {@code LICENSE_KEY} environment variable is forwarded from the host
 * into the container so that the configuration can use the
 * {@code #{LICENSE_KEY}} placeholder.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * // Single plugin with specific version
 * def container = CurityServerContainer.withVersion("11.0")
 *     .withPlugin("build/my-plugin")
 * container.start()
 *
 * // Multiple plugins, configurations, files, and environment variables
 * def container = CurityServerContainer.withVersion("11.0")
 *     .withPlugin("build/my-plugin")
 *     .withPlugin("build/another-plugin")
 *     .withConfiguration("src/test/resources/plugin-config.xml")
 *     .withConfiguration("src/test/resources/extra-config.xml")
 *     .withFile("src/test/resources/my-keystore.p12", "/opt/idsvr/etc/init/my-keystore.p12")
 *     .withEnvVariables(["MY_VAR": "value1", "OTHER_VAR": "value2"])
 * container.start()
 *
 * def adminUrl   = container.adminUrl   // https://localhost:&lt;mapped-port&gt;
 * def runtimeUrl = container.runtimeUrl // https://localhost:&lt;mapped-port&gt;
 * </pre>
 */
final class CurityServerContainer extends GenericContainer<CurityServerContainer> {

    private static final def logger = LoggerFactory.getLogger(CurityServerContainer.class)
    private static final int ADMIN_PORT = 6749
    private static final int RUNTIME_PORT = 8443
    private static final int MTLS_RUNTIME_PORT = 8444
    private static final int STATUS_PORT = 4465
    private static final String BASE_IMAGE_REPOSITORY = "curity.azurecr.io/curity/idsvr"
    private static final String DEFAULT_BASE_IMAGE = "$BASE_IMAGE_REPOSITORY:latest"
    private static final String BASE_CONFIG_RESOURCE = "base-config.xml"

    private List<Path> pluginFolders = []
    private List<Path> configurationFiles = []
    private List<Map.Entry<Path, String>> files = []
    private Map<String, String> envVariables = [:]
    private String version = null

    private static String imageForVersion(String version) {
        version ? "$BASE_IMAGE_REPOSITORY:$version" : DEFAULT_BASE_IMAGE
    }

    private CurityServerContainer() {
        // The image passed here is a placeholder; it is replaced in start() once all builder state is accumulated.
        super(DEFAULT_BASE_IMAGE)

        withExposedPorts(ADMIN_PORT, RUNTIME_PORT, STATUS_PORT, MTLS_RUNTIME_PORT)

        // Stream container logs to test output
        withLogConsumer(new Slf4jLogConsumer(logger).withSeparateOutputStreams())

        // Wait for the status endpoint to report the node is ready (fail fast on ERROR)
        waitingFor(new CurityStatusWaitStrategy(STATUS_PORT)
                .withStartupTimeout(Duration.ofSeconds(120)))
    }

    /**
     * Create a new Curity Server container for the given version.
     *
     * @param version Version tag (e.g. {@code "11.0"}).
     * @return a new container instance for chaining
     */
    static CurityServerContainer withVersion(String version) {
        def container = new CurityServerContainer()
        container.version = version
        return container
    }

    /**
     * Add a plugin folder to be installed in the container.
     * Can be called multiple times to add multiple plugins.
     *
     * @param pluginFolderPath Path to the plugin release folder
     * @return this container instance for chaining
     */
    CurityServerContainer withPlugin(String pluginFolderPath) {
        pluginFolders.add(Paths.get(pluginFolderPath))
        return this
    }

    /**
     * Add a configuration XML file to be loaded on startup.
     * Can be called multiple times to add multiple configuration files.
     *
     * @param configXmlPath Path to the configuration XML file
     * @return this container instance for chaining
     */
    CurityServerContainer withConfiguration(String configXmlPath) {
        configurationFiles.add(Paths.get(configXmlPath))
        return this
    }

    /**
     * Add a file to be copied into the container during image build.
     * Can be called multiple times to add multiple files.
     *
     * @param src Path to the source file on the host
     * @param dest Absolute path where the file should be placed in the container
     * @return this container instance for chaining
     */
    CurityServerContainer withFile(String src, String dest) {
        files.add(Map.entry(Paths.get(src), dest))
        return this
    }

    /**
     * Add environment variables to be passed to the container at runtime.
     * Can be called multiple times; values accumulate across calls.
     *
     * @param envVariables Map of environment variable names to values
     * @return this container instance for chaining
     */
    CurityServerContainer withEnvVariables(Map<String, String> envVariables) {
        this.envVariables.putAll(envVariables)
        return this
    }

    /**
     * Build the Docker image with plugins and configuration files.
     */
    private static ImageFromDockerfile buildImage(List<Path> configurationFiles, List<Path> pluginFolders,
                                                  List<Map.Entry<Path, String>> files, String baseImage) {
        def baseConfigContent = readBaseConfig()
        def baseConfigTemp = createTempFile(baseConfigContent, "base-config", ".xml")
        def image = new ImageFromDockerfile()
                .withDockerfileFromBuilder { builder ->
                    def b = builder.from(baseImage)
                            .user("root")
                            .copy("base-config.xml", "/opt/idsvr/etc/init/base-config.xml")

                    configurationFiles.eachWithIndex { configFile, index ->
                        def fileName = configFile.fileName.toString()
                        def contextName = "extra-config-${index}"
                        b.copy(contextName, "/opt/idsvr/etc/init/${fileName}")
                    }

                    pluginFolders.eachWithIndex { pluginFolder, index ->
                        def folderName = pluginFolder.fileName.toString()
                        def contextName = "plugin-${index}"
                        b.copy("${contextName}/", "/opt/idsvr/usr/share/plugins/$folderName")
                    }

                    files.eachWithIndex { entry, index ->
                        def contextName = "file-${index}"
                        b.copy(contextName, entry.value)
                    }

                    b.run("chown -R idsvr:idsvr /opt/idsvr/etc/init /opt/idsvr/usr/share/plugins")
                            .run("ln -sf /dev/stdout /opt/idsvr/var/log/confsvc.log")
                            .user("idsvr")
                            .env("LOGGING_LEVEL", "DEBUG")
                            .env("SERVICE_ROLE", "default")
                            .env("ADMIN", "true")
                            .build()
                }
                .withFileFromPath("base-config.xml", baseConfigTemp)

        pluginFolders.eachWithIndex { pluginFolder, index ->
            image.withFileFromPath("plugin-${index}", pluginFolder)
        }

        configurationFiles.eachWithIndex { configFile, index ->
            image.withFileFromPath("extra-config-${index}", configFile)
        }

        files.eachWithIndex { entry, index ->
            image.withFileFromPath("file-${index}", entry.key)
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
                            "Ensure the curity-ps-sdk-commons test fixtures are on the classpath."
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

    /**
     * Get the mapped runtime mtls port.
     *
     * @return The host port mapped to the container's mTLS runtime port
     */
    int getMtlsRuntimePort() {
        getMappedPort(MTLS_RUNTIME_PORT)
    }

    /**
     * Get the full URL to the authorize endpoint as configured in the bundled base-config.xml.
     *
     * @return full URL
     */
    String getAuthorizationEndpointUrl() {
        getRuntimeUrl() + "/oauth/v2/oauth-authorize"
    }

    /**
     * Get the full URL to the token endpoint as configured in the bundled base-config.xml.
     *
     * @return full URL
     */
    String getTokenEndpointUrl() {
        getRuntimeUrl() + "/oauth/v2/oauth-token"
    }

    /**
     * Load an XML configuration snippet into the running server by writing it
     * to a temporary file inside the container and then running
     * {@code load merge <path>} in idsh.
     *
     * @param xml the XML configuration to load
     */
    void loadXmlConfig(String xml) {
        if (!isRunning()) {
            throw new IllegalStateException("Container must be running before loading XML config")
        }
        def containerPath = "/tmp/load-config-${System.nanoTime()}.xml"
        def writeResult = execInContainer("sh", "-c", "cat > ${containerPath} <<'XMLEOF'\n${xml}\nXMLEOF")
        if (writeResult.exitCode != 0) {
            throw new IllegalStateException("Failed to write XML config file: ${writeResult.stderr}")
        }
        runIdshCommands([
                "configure",
                "load merge ${containerPath}",
                "commit",
                "exit no-confirm",
                "exit"
        ])
    }

    /**
     * Run a sequence of idsh commands after the container has started.
     */
    void runIdshCommands(List<String> commands) {
        if (!isRunning()) {
            throw new IllegalStateException("Container must be running before executing idsh")
        }
        def script = commands.join("\n") + "\n"
        def cmd = "cat <<'EOF' | idsh\n${script}EOF"
        def result = execInContainer("sh", "-c", cmd)
        if (result.exitCode != 0) {
            throw new IllegalStateException("idsh failed: ${result.exitCode}\n${result.stderr}")
        }
    }

    /**
     * Configure the base-url using the current runtimeUrl after the container has started.
     */
    private void configureBaseUrlFromRuntime() {
        runIdshCommands([
                "configure",
                "set environments environment base-url ${getRuntimeUrl()}",
                "commit",
                "exit no-confirm",
                "exit"
        ])
    }

    @Override
    void start() {
        // Build the image from accumulated builder state
        setImage(buildImage(configurationFiles, pluginFolders, files, imageForVersion(version)))

        // Pass the license key into the container for config parameterization (if set)
        def licenseKey = System.getenv("LICENSE_KEY")
        if (licenseKey != null) {
            withEnv("LICENSE_KEY", "<license-key>$licenseKey</license-key>")
        } else {
            withEnv("LICENSE_KEY", "")
        }

        // Pass user-supplied environment variables as runtime container env vars
        envVariables.each { key, value ->
            withEnv(key, value)
        }

        super.start()
        configureBaseUrlFromRuntime()
    }

    @Override
    void close() {
        // noop
    }
}
