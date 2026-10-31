# Java 25 Migration Summary

## Status: ✅ BUILD SUCCESS — 12/12 tests passing

---

## Changes Made

### pom.xml — Full dependency and plugin upgrade

| Item | Before | After |
|---|---|---|
| Spring Boot parent | 1.2.3.RELEASE | **3.5.0** |
| java.version | 25 | 25 (unchanged) |
| Groovy | org.codehaus.groovy:groovy-all:2.4.3 | **org.apache.groovy:groovy:4.0.32** |
| Spock | spock-core/spring:1.0-groovy-2.4 | **spock-core/spring:2.4-groovy-4.0** |
| Guava | 29.0-jre | **33.4.0-jre** |
| commons-io | 2.4 | **2.18.0** |
| cglib-nodep | 3.1 (explicit) | **removed** (Spring 6 bundles CGLIB) |
| Explicit Spring 4.x deps | many at 4.1.6.RELEASE | **removed** (Boot BOM manages) |
| maven-surefire-plugin | 2.17 | **3.5.2** (JUnit Platform, Spec includes) |
| gmavenplus-plugin | *(absent)* | **3.0.2** (compiles Groovy test sources) |
| byte-buddy-agent | implicit | explicit test dep (for surefire argLine) |
| Milestone repo | http://repo.spring.io/milestone/ | **removed** (Maven Central only) |

**Root cause of Groovy tests not running:** there was no Groovy compilation plugin
(`gmavenplus-plugin`) in the build, so `DownloadControllerSpec.groovy` was never compiled
and Surefire found zero tests to execute. Groovy 2.4 / Spock 1.0 are also incompatible
with Java 25 class files (ASM doesn't support class file major version 69).

---

### src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java

`generateETagHeaderValue` method signature changed in Spring 5.1:
- **Before:** `protected String generateETagHeaderValue(byte[] bytes)`
- **After:** `protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException`

---

### src/test/java/com/nurkiewicz/download/FileExamples.java

Changed `TXT_FILE_UUID` and `NOT_FOUND_UUID` from `UUID.randomUUID()` to fixed deterministic
UUIDs.

**Root cause:** Spring Boot 3.x creates the application context in a child `ClassLoader`
(Spring Boot context loader), causing `FileExamples` to be loaded **twice** — once by the
test classloader and once by Spring's context classloader. Each load generated a different
random UUID, so `FileStorageStub.findFile(uuid)` could never match the UUID the Groovy test
put in the request URL. Switching to fixed UUIDs means both classloader copies produce equal
`UUID` objects (since `UUID.equals()` compares bit values, not object identity, using the
JDK bootstrap classloader's `UUID` class).

---

### src/main/resources/logback.xml

Added missing `name` attribute on the `<appender>` element and moved the encoder to a named
appender referenced via `<appender-ref>`. This removes the logback `ERROR: Missing attribute
[name]` warning on startup.

---

## Next steps

- `FileSystemPointer` uses `Files.hash(target, Hashing.sha512())` from Guava which is
  deprecated (see `Recompile with -Xlint:deprecation`). Consider migrating to
  `java.nio.file.Files.newInputStream()` + `Hashing.sha512().hashStream()` in a follow-up.
- The project still uses `org.springframework.boot:spring-boot-starter-parent:3.5.0`. If a
  newer Spring Boot 3.5.x patch is desired, update the parent version accordingly.
- Spring Boot 3.5 uses the Jakarta namespace. The existing source code contains no direct
  `javax.*` EE imports so no source-level javax→jakarta migration was required.
