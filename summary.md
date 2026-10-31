# Java 25 Migration Summary

## Migration Result

**Status: BUILD SUCCESS — 12/12 tests pass**

---

## Changes Made

### 1. `pom.xml` — Full dependency upgrade

| Change | Before | After |
|---|---|---|
| Spring Boot parent | 1.2.3.RELEASE | **3.5.16** |
| Java target | 25 (property only) | 25 (unchanged) |
| Spring Framework | 4.1.6.RELEASE (explicit) | 6.2.19 (managed by Boot 3.5) |
| Guava | 29.0-jre | **33.7.2-jre** |
| commons-io | 2.4 | **2.22.0** |
| Spock | 1.0-groovy-2.4 | **2.5-groovy-4.0** |
| Groovy | groovy-all 2.4.3 (codehaus) | **groovy 4.0.33 (apache)** |
| cglib-nodep | 3.1 | **removed** (Spring Boot 3.x uses byte-buddy) |
| Test framework | JUnit 4 (via Spring Boot 1.x) | **JUnit 5 / JUnit Platform** (via Spring Boot 3.5) |

Added:
- `gmavenplus-plugin 4.3.1` — compiles `src/test/groovy` Groovy sources (Spock tests)
- `net.bytebuddy:byte-buddy-agent` — for Surefire javaagent (Mockito/ByteBuddy)
- `junit-platform-launcher` — enables Surefire 3.x to run JUnit Platform (Spock 2.x) tests
- Surefire `<includes>` config to match `*Spec` test classes alongside `*Test`

Removed:
- All explicit `org.springframework:*` version overrides (managed by Spring Boot BOM)
- Explicit version pins on `spring-boot-starter-*` (managed by parent)
- Milestone repository (Spring Boot 3.5 is a GA release)
- Duplicate `org.springframework:spring-test` dependency

### 2. `Sha512ShallowEtagHeaderFilter.java` — Spring 6 API update

`ShallowEtagHeaderFilter.generateETagHeaderValue()` signature changed in Spring Framework 5.0:

```java
// Before (Spring 4.x)
protected String generateETagHeaderValue(byte[] bytes)

// After (Spring 5+ / 6.x)
protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException
```

Updated the override to match the new signature, using `inputStream.readAllBytes()`.

### 3. `FileExamples.java` — Fixed Groovy constant-folding UUID issue

**Root cause:** Groovy 4.x constant-folds `private static final String` initializers at compile time. The compiled bytecode for `DownloadControllerSpec.TEXT_FILE` embedded the UUID value from the compilation JVM (via `UUID.randomUUID()` evaluated by GMavenPlus). At test runtime, a new JVM initializes `FileExamples` with a **different** `UUID.randomUUID()` value, so `FileStorageStub.TXT_FILE_UUID` never matched the URL UUID, causing all 11 file-serving tests to return 404.

**Fix:** Changed from `UUID.randomUUID()` to deterministic fixed UUIDs:

```java
// Before
public static final UUID TXT_FILE_UUID = UUID.randomUUID();
public static final UUID NOT_FOUND_UUID = UUID.randomUUID();

// After
public static final UUID TXT_FILE_UUID = UUID.fromString("deadbeef-dead-beef-dead-beefdeadbeef");
public static final UUID NOT_FOUND_UUID = UUID.fromString("cafebabe-cafe-babe-cafe-babecafebabe");
```

### 4. `logback.xml` — Fixed broken appender configuration

The `<appender>` element inside `<root>` was missing the required `name` attribute, causing a Logback error and no console output. Moved the appender definition to the top level and added `<appender-ref>`.

---

## Why Java 25 requires Spring Boot 3.x

Spring 5.3 (used by Spring Boot 2.7) bundles an ASM that cannot parse Java 25 class files (major version 69). Spring 6.x (Spring Boot 3.x) includes a modern ASM that supports Java 21/25. Per the knowledge base, **there is no javax namespace path for Java 21 or 25** — Spring Boot 3.x (jakarta namespace) is mandatory.

---

## Next Steps

- The `com.google.common.io.Files.hash(File, HashFunction)` call in `FileSystemPointer.java` uses a deprecated Guava API. Consider migrating to `com.google.common.io.Files.asByteSource(file).hash(hashFunction)`.
- `Hashing.sha512()` in `Sha512ShallowEtagHeaderFilter` has a deprecation note in Guava 33.x (acceptable; not removed).
- Spring Boot 3.5 is the current stable LTS target. Spring Boot 4.0 (Spring Framework 7, Jakarta EE 11) is an opt-in upgrade path for the future.
- Virtual threads can be enabled for Java 25 / Spring Boot 3.2+ by adding `spring.threads.virtual.enabled=true` to `application.properties`.
- `java.util.Date` in `@RequestHeader(IF_MODIFIED_SINCE) Optional<Date>` still works in Spring MVC 6.x but is deprecated; consider migrating to `Optional<ZonedDateTime>` or `Optional<Instant>`.
