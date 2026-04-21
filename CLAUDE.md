# CLAUDE.md

## Project overview

Curity PS SDK Commons — a Java/Groovy library of utility classes for developing plugins for the Curity Identity Server.
Published as `io.curity:curity-ps-sdk-commons` to GitHub Packages.

The library has two parts:

1. **Main library** (`src/main/java`) — utility classes like JWT validation, PKCE helpers, URI helpers, and OpenID
   metadata management.
2. **Test fixtures** (`src/testFixtures/groovy`) — reusable test utilities published via Gradle's `java-test-fixtures`
   plugin. Consumers add `testImplementation testFixtures('io.curity:curity-ps-sdk-commons:<version>')` to get all test
   utilities with transitive dependencies.

## Build and test

- **Build:** `./gradlew build`
- **Test:** `./gradlew test`
- **Publish locally:** `./gradlew publishToMavenLocal`

Java 21 is required (configured via Gradle toolchain).

## Verification

Always run `./gradlew test` to verify changes before committing. The `CurityServerContainerSpecification` test requires
Docker to be running (it starts a Curity Identity Server via Testcontainers).

## Key test fixtures

- **CurityServerContainer** — Testcontainers-based container with a factory builder pattern (
  `CurityServerContainer.withVersion("11.0")`). Supports multiple plugins, configuration files, and environment
  variables. The class is `final` with a private constructor.
- **TestOAuthClient** — OAuth 2.0 test client that drives the authorization code flow via a headless browser. Has a
  `defaultClient` factory with pre-configured credentials (`integration-test-client`/`integration-test-secret`).
- **HeadlessBrowser** — HtmlUnit 4.x-based headless browser for interacting with web pages in tests. Configured to trust
  all SSL certificates.

## Project structure

```
src/main/java/          — Main library source (Java)
src/testFixtures/groovy/ — Reusable test utilities (Groovy), published as test fixtures
src/test/groovy/         — Test specs (Groovy/Spock), not published
src/testFixtures/resources/ — Resources bundled with test fixtures (base-config.xml, etc.)
```

## Dependencies

- Curity Identity Server SDK (`compileOnly`)
- jose4j for JWT/JSON handling
- Testcontainers for integration testing
- HtmlUnit 4.x for headless browser
- Spock for test framework

## Git

- Use [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat:`, `fix:`, `refactor:`, `test:`,
  `docs:`, `build:`, `chore:`)
- Do not push unless explicitly asked
- Do not rewrite history (no force push, no amend of pushed commits, no interactive rebase of published branches)
- Create feature branches from `main` for new work
