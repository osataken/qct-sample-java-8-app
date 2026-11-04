# Java 25 Migration Summary

## Result
**BUILD SUCCESS** — 12/12 Spock tests pass on Java 25 (OpenJDK Corretto 25.0.5).

## Changes Made

### pom.xml
- **Spring Boot parent**: `1.2.3.RELEASE` → `3.5.0`
  - Required for Java 25 runtime compatibility; Spring Boot 1.x uses an internal cglib (3.1) that cannot read Java 25 class files (version 69), causing a hard crash at startup
  - All explicit Spring Framework 4.1.6.RELEASE dependency declarations removed (now managed by Spring Boot 3.5 BOM)
  - Removed duplicate `spring-test` dependency and the old Spring milestone repository
- **Spock Framework**: `spock-core`/`spock-spring` `1.0-groovy-2.4` → `2.4-groovy-5.0`
  - Spock 1.0 is incompatible with Java 25; Spock 2.4 runs on JUnit 5 Platform
- **Groovy**: `org.codehaus.groovy:groovy-all:2.4.3` → `org.apache.groovy:groovy:5.0.0`
  - Groovy 2.4 cannot run on Java 25 JVM
  - Groovy 4.x (as managed by Spring Boot 3.5 BOM) cannot read Java 25 class files (version 69) during semantic analysis
  - Groovy 5.0 is required for Java 25 class file support
- **GMavenPlus plugin** `3.0.2` added — was missing entirely; without it the `.groovy` test files in `src/test/groovy` were never compiled and zero tests ran
  - Configured `skipBytecodeCheck=true` and `targetBytecode=25` since GMavenPlus 3.0.2 predates Java 25 target recognition
- **maven-surefire-plugin**: `2.17` (inherited) → `3.5.2` — enables JUnit 5 Platform provider required by Spock 2.x
- **Guava**: `29.0-jre` → `33.4.0-jre` — Java 25 compatibility
- **commons-io**: `2.4` → `2.18.0` — Java 25 compatibility
- Removed `cglib:cglib-nodep:3.1` — Spring Boot 3.x bundles its own CGLIB; the old artifact is incompatible with Java 25 bytecode anyway

### src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java
- Updated `generateETagHeaderValue` method signature from `(byte[])` (Spring 4.x API) to `(InputStream inputStream, boolean isWeak)` (Spring 6.x API)
- Body updated to read bytes from InputStream using `readAllBytes()` before computing SHA-512

### src/test/groovy/com/nurkiewicz/download/DownloadControllerTest.groovy
- Replaced `@WebAppConfiguration` + `@ContextConfiguration(classes = [MainApplication])` with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)` — Spring Boot 3.x test bootstrap
- Updated imports accordingly

### src/main/resources/logback.xml
- Fixed malformed `<appender>` element that was missing the required `name` attribute
- Restructured to properly declare `CONSOLE` appender and reference it via `<appender-ref>` from the root logger
- Spring Boot 3.5's Logback integration enforces strict validation and throws `IllegalStateException` on configuration errors (Spring Boot 1.x silently ignored them)

## Next Steps
- `com.google.common.io.Files.hash(File, HashFunction)` in `FileSystemPointer.java` is deprecated in Guava 33.x — consider replacing with `com.google.common.io.Files.asByteSource(file).hash(Hashing.sha512())`
- `com.google.common.base.Optional.transform(...).or(...)` chain in `ExistingFile.java` uses Guava's deprecated Optional — consider migrating to `java.util.Optional`
- `@RequestHeader(IF_MODIFIED_SINCE) Optional<Date>` in `DownloadController.java` uses the legacy `java.util.Date` type — consider migrating to `Optional<Instant>` or `Optional<ZonedDateTime>`
- GMavenPlus 4.0.0 was found in Maven Central but its JAR was missing the Maven plugin descriptor (invalid artifact) — monitor for a valid 4.0.x release that natively recognizes Java 25 as a bytecode target (removing the need for `skipBytecodeCheck`)
