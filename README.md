# curity-sdk-commons
Curity SDK Commons, a package of Java utility classes for the Curity Identity Server plugin development

This package is private for the time being, when we are ready we can make it public.

The release action for this github project creates a package in the maven repo, which can be used with authentication. But easier to build it locally as long as its private.


## Build
Use `./gradlew build` to build the project. The resulting JAR file will be located in the `build/libs` directory and be named `curity-sdk-commons-X.X.X.jar`.

To be able to use the jar as a dependency in local projects, run `./gradlew publishToMavenLocal`. This will publish the jar to your local maven repository. You can then use it in your projects by adding a dependency to your `build.gradle` file:

```groovy
    implementation 'io.curity:curity-sdk-commons:0.1.0'
```
