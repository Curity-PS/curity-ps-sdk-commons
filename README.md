# curity-ps-sdk-commons
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

All transitive dependencies (Testcontainers, HtmlUnit, Spock, etc.) are included automatically.

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

container.runIdshCommands([
    "configure",
    "set environments environment some-setting some-value",
    "commit",
    "exit no-confirm",
    "exit"
])
```

### TestOAuthClient

An OAuth 2.0 test client that drives the authorization code flow using a headless browser. It navigates
to the authorization endpoint, follows redirects to the authentication page, intercepts the redirect back
to the `redirect_uri` to capture the authorization code, and exchanges it for tokens.

A default client is provided with pre-configured client credentials and support for code flow and token introspection. 

Create a default client using the container's endpoint URLs:

```groovy
def browser = HeadlessBrowser.create()
def client = TestOAuthClient.defaultClient(
    browser,
    container.authorizationEndpointUrl,
    container.tokenEndpointUrl
)
```

Optionally pass `acr_values` to select a specific authenticator:

```groovy
def client = TestOAuthClient.defaultClient(
    browser,
    container.authorizationEndpointUrl,
    container.tokenEndpointUrl,
    "urn:se:curity:authentication:html-form:my-authenticator"
)
```

Run the authorization code flow. After `codeFlow()`, the browser is on the authentication page where
you can interact with form fields. Example Spock test:

```groovy
when: "The code flow is started and the user authenticates"
client.codeFlow()
client.browser.typeByCss("#userName", "testuser")
client.browser.typeByCss("#password", "secret")
client.browser.clickByCss("#login-btn")

then: "The flow completes with an authorization code"
client.flowComplete

when: "The code is exchanged for tokens"
def tokens = client.exchangeCode()

then: "An access token is returned"
tokens.accessToken != null
```

Introspect the access token to verify its claims:

```groovy
when: "The access token is introspected"
def introspection = client.introspect(tokens.accessToken)

then: "The token is active and contains the expected subject"
introspection.active == true
introspection.sub == "testuser"
```

### HeadlessBrowser

An [HtmlUnit](https://www.htmlunit.org/)-based headless browser for interacting with web pages in
integration tests. It is configured to trust all SSL certificates and can optionally use a client
certificate for mTLS.

```groovy
def browser = HeadlessBrowser.create()

// Navigate and interact with pages
browser.navigate("https://localhost:8443/some-page")
browser.typeByCss("#username", "admin")
browser.clickByCss("#submit")

// Wait for elements to appear
browser.waitForElement(".success-message")
```

For mTLS authentication, supply a keystore:

```groovy
def keystoreUrl = new File("src/test/resources/client.p12").toURI().toURL()
def browser = HeadlessBrowser.create(keystoreUrl, "changeit")
```
