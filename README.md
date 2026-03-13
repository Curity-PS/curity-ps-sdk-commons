# curity-sdk-commons
[![Quality](https://img.shields.io/badge/quality-demo-red)](https://curity.io/resources/code-examples/status/)

Curity SDK Commons, a package of Java utility classes for the Curity Identity Server plugin development

This package is private for the time being, when we are ready we can make it public.

The release action for this github project creates a package in the maven repo, which can be used with authentication.
But easier to build it locally as long as its private.

This is ongoing development, and is not yet ready for production use. The library is not supported by Curity Professional Services.

## Build

Use `./gradlew build` to build the project. The resulting JAR file will be located in the `build/libs` directory and be
named `curity-sdk-commons-X.X.X.jar`.

To be able to use the jar as a dependency in local projects, run `./gradlew publishToMavenLocal`. This will publish the
jar to your local maven repository. You can then use it in your projects by adding a dependency to your `build.gradle`
file:

```groovy
    implementation 'io.curity:curity-sdk-commons:0.1.0'
```

## Publish to GitHub artifact storage

There is an action to create a release and publish the JAR file to the GitHub artifact storage. This can be used to
share the JAR file with other developers or to use it in other projects.

## Test Utils

The library includes test utilities for integration testing Curity Identity Server plugins. These are published as
[Gradle test fixtures](https://docs.gradle.org/current/userguide/java_test_fixtures.html) and can be consumed by adding:

```groovy
testImplementation testFixtures('io.curity:curity-ps-sdk-commons:<version>')
```

### CurityServerContainer

A [Testcontainers](https://www.testcontainers.org/)-based container that runs a Curity Identity Server instance
with plugins and configuration pre-installed. Create instances using the `withVersion` factory method:

```groovy
def container = CurityServerContainer.withVersion("11.0")
container.start()
```

Add one or more plugins by pointing to the plugin release folder:

```groovy
def container = CurityServerContainer.withVersion("11.0")
    .withPlugin("build/distributions/my-plugin")
    .withPlugin("build/distributions/another-plugin")
container.start()
```

Supply additional configuration XML files that are merged with the bundled base configuration on startup:

```groovy
def container = CurityServerContainer.withVersion("11.0")
    .withPlugin("build/distributions/my-plugin")
    .withConfiguration("src/test/resources/my-plugin-config.xml")
    .withConfiguration("src/test/resources/extra-config.xml")
container.start()
```

Pass environment variables into the container (useful for parameterized configuration):

```groovy
def container = CurityServerContainer.withVersion("11.0")
    .withPlugin("build/distributions/my-plugin")
    .withConfiguration("src/test/resources/my-plugin-config.xml")
    .withEnvVariables(["MY_SETTING": "value1", "OTHER_SETTING": "value2"])
container.start()
```

After the container has started, use the convenience accessors to connect to it:

```groovy
container.adminUrl          // https://localhost:<mapped-port>
container.runtimeUrl        // https://localhost:<mapped-port>
container.adminPort         // mapped admin port
container.runtimePort       // mapped runtime port
container.mtlsRuntimePort   // mapped mTLS runtime port
```

You can also load configuration or run idsh commands at runtime:

```groovy
container.loadXmlConfig('<config>...</config>')
container.configureBaseUrlFromRuntime()
```