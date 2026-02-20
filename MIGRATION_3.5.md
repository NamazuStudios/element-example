# Migration Guide: 3.5 (and Earlier) → 3.7

This document covers every breaking change required when upgrading a custom Element from the legacy single-module layout (3.5.x and earlier, as found on the `main` branch) to the 3.7 multi-module layout.

If you are already on 3.6, see [MIGRATION_3.6.md](MIGRATION_3.6.md) instead — this document is the superset that includes all 3.6 changes as well as the larger structural migration.

---

## The Fundamental Change in 3.7

The central shift in 3.7 is that **Elements are now loaded and referenced entirely by their Maven coordinates**. Every artifact the runtime needs (the SPI, the API jars exported to other Elements, and the element implementation itself) is identified by a `groupId:artifactId:version` coordinate and resolved through standard Maven repositories. To solve deployment and packaging issues, Namazu Elements also can load the SPI types just in time for deployment eliminating the need to package the SPI bundles in the release.

This replaces the previous git-based deployment system, in which the runtime discovered code by inspecting a checked-out repository layout. The new approach brings several concrete benefits:

- **Standard tooling:** any Maven-compatible repository (Nexus, Artifactory, GitHub Packages, Maven Central) can serve as a deployment target, with no custom git hooks or repository layout requirements.
- **Reproducible builds:** exact artifact versions are pinned in the deployment descriptor rather than inferred from git state.
- **Isolated classpaths:** the `api/`, `lib/`, and `classpath/` sections of the `.elm` archive map directly to separate classloader layers inside the runtime, eliminating the classpath pollution problems that the old flat-distribution approach was prone to.
- **Inter-element API contracts:** the classified API jar (`api/` directory in the archive) makes the boundary between what an Element exports and what it keeps private explicit and enforced at load time.
- **Builtin SPI Configurations:** Namazu Elements does not "know" how an Element loads. Rather it uses an [SPI (Service Provider Implementation)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html) on the Element's classpath to perform the loading. Prior to 3.7 we bundled this in the Element's implementation and it forced lock-step versioning due to binary incompatibilities. By providing this on-the-fly we have eliminated that issue by providing pre-determined configurations that can cross version to version.

All the specific migration steps below flow from this architectural change.

---

## Step 1 — Restructure to a Multi-Module Project

The single flat Maven project becomes a parent POM with three submodules.

### New directory layout

```
project-root/
├── pom.xml                  ← parent POM (packaging=pom)
├── api/                     ← optional: only needed to export types to other Elements
│   ├── pom.xml
│   └── src/main/java/       ← exported interfaces (e.g. service interfaces)
├── element/
│   ├── pom.xml
│   └── src/main/java/       ← implementation (was src/main/java/)
└── debug/
    ├── pom.xml
    └── src/main/java/       ← local runner (was src/test/java/Main.java)
```

> **The `api` module is optional.** Create it only if your Element needs to export interfaces or types that other Elements in the same deployment will consume. In 3.5 and earlier this capability did not exist at all, so adding an `api` module now is purely a forward-compatibility investment — it does not affect the behaviour of your Element in a 3.7 deployment unless another Element explicitly depends on it.

### What moves where

| Old location | New location |
|---|---|
| `src/main/java/com/mystudio/mygame/**` (impl) | `element/src/main/java/com/mystudio/mygame/**` |
| `src/main/java/com/mystudio/mygame/service/GreetingService.java` (interface, if exporting) | `api/src/main/java/com/mystudio/mygame/service/GreetingService.java` |
| `src/main/resources/**` | `element/src/main/resources/**` |
| `src/test/java/Main.java` | Replaced by `debug/src/main/java/run.java` (see Step 5) |
| `src/assembly/zip.xml` | **Delete it** — replaced by ELM archive build |

If you are creating an `api` module, move only the interfaces and DTOs you intend to share with other Elements into it. All implementation classes remain in `element/`.

---

## Step 2 — Rewrite the Root `pom.xml`

Replace the root `pom.xml` entirely. The old root was both the parent and the implementation; the new root is a pure parent POM.

### Change `groupId`, `artifactId`, and `packaging`

```xml
<!-- Before -->
<groupId>org.example</groupId>
<artifactId>ElementSample</artifactId>
<version>1.0-SNAPSHOT</version>

<!-- After -->
<groupId>com.example.element</groupId>
<artifactId>parent</artifactId>
<version>1.0-SNAPSHOT</version>
<packaging>pom</packaging>
```

> **Important:** If you publish the API jar to a Maven repository, update the `groupId` everywhere it is referenced (other Element POMs, your CI pipeline, etc.).

### Add the `<modules>` block

```xml
<modules>
    <module>api</module>     <!-- optional: omit if not exporting types to other Elements -->
    <module>element</module>
    <module>debug</module>
</modules>
```

### Replace the `<properties>` block

Remove the old distribution-path properties and add `api.classifier`:

```xml
<!-- Remove these -->
<element.target.dir>...</element.target.dir>
<element.distribution.dir>...</element.distribution.dir>
<element.distribution.zip>...</element.distribution.zip>

<!-- New root properties -->
<properties>
    <encoding>UTF-8</encoding>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <elements.version>3.7.0-SNAPSHOT</elements.version>
    <swagger.version>2.2.22</swagger.version>
    <guice.version>7.0.0</guice.version>
    <rs.api>4.0.0</rs.api>
    <jakarta.websocket.version>2.1.0</jakarta.websocket.version>
    <crossfire.version>1.0.4</crossfire.version>
    <servlet.api>6.1.0</servlet.api>
    <logback.version>1.2.3</logback.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <api.classifier>${project.groupId}.api</api.classifier>
</properties>
```

### Replace `<dependencies>` with `<dependencyManagement>` + BOM

The old root listed every dependency directly with explicit versions. In 3.7, the root POM only manages versions via the `sdk-bom` import using Maven's [bill-of-materials (BOM) pattern](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms) — it does **not** declare dependencies itself. Child modules declare only what they use.

```xml
<!-- Remove the entire old <dependencies> block from root pom.xml -->

<!-- Add this instead -->
<dependencyManagement>
    <dependencies>

        <!-- sdk-bom manages all SDK artifact versions and scopes -->
        <dependency>
            <groupId>dev.getelements.elements</groupId>
            <artifactId>sdk-bom</artifactId>
            <version>${elements.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Your own API module (unclassified, provided scope) -->
        <dependency>
            <groupId>com.example.element</groupId>
            <artifactId>api</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Your own API module (classified jar, provided scope) -->
        <dependency>
            <groupId>com.example.element</groupId>
            <artifactId>api</artifactId>
            <version>${project.version}</version>
            <classifier>${api.classifier}</classifier>
            <scope>provided</scope>
        </dependency>

        <!-- Your element implementation -->
        <dependency>
            <groupId>com.example.element</groupId>
            <artifactId>element</artifactId>
            <version>${project.version}</version>
        </dependency>

    </dependencies>
</dependencyManagement>
```

### Remove the entire `<build>` block from root

All build logic (assembly, antrun, dependency-plugin, resources-plugin) moves to `element/pom.xml` in a new form (see Step 4). Delete everything inside `<build>` from the root POM.

### Remove the `namazu-crossfire` Maven profile

The `<profiles>` block containing `namazu-crossfire` is gone. Delete it entirely. The Crossfire element is not bundled this way in 3.7.

---

## Step 3 — Create `api/pom.xml` *(optional)*

> **Skip this step** if your Element does not need to share types with other Elements in the same deployment. The `api` module did not exist in 3.5 and earlier — introducing it now is good practice for future-proofing, but it has no effect on your Element's behaviour unless another Element explicitly declares a dependency on it.

Create a new `api/` directory with the following `pom.xml`. This module holds interfaces and DTOs that you want to export to other Elements.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example.element</groupId>
        <artifactId>parent</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>api</artifactId>
    <version>1.0-SNAPSHOT</version>

    <build>
        <plugins>
            <!--
                Produces a second "classified" jar alongside the regular jar.
                This classified jar is what gets placed in the elm/api/ directory
                and made visible to other Elements at runtime.
            -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <executions>
                    <execution>
                        <id>classified-jar</id>
                        <phase>package</phase>
                        <goals><goal>jar</goal></goals>
                        <configuration>
                            <classifier>${api.classifier}</classifier>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <!--
            Keep API dependencies as lean as possible. Only interfaces, plain DTOs,
            and classes that depend on the core SDK or standard Java APIs belong here.
            Refer to the Elements manual for what is already provided by the runtime.
        -->
        <dependency>
            <groupId>dev.getelements.elements</groupId>
            <artifactId>sdk</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>

</project>
```

Move your service interfaces (like `GreetingService`) into `api/src/main/java/`. Implementation classes stay in `element/`.

---

## Step 4 — Create `element/pom.xml` with the ELM Archive Build

Create `element/pom.xml`. This is where your implementation source lives and where the `.elm` archive is assembled.

### Dependencies

Unlike the old root POM, you do **not** specify `<version>` or `<scope>` on SDK dependencies — these come from the BOM imported in the parent. Include only what your element needs:

```xml
<dependencies>
    <!-- Classified API jar — copied into elm/api/ for export to other Elements -->
    <dependency>
        <groupId>com.example.element</groupId>
        <artifactId>api</artifactId>
        <classifier>${api.classifier}</classifier>
    </dependency>

    <!-- Core SDK -->
    <dependency>
        <groupId>dev.getelements.elements</groupId>
        <artifactId>sdk</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.getelements.elements</groupId>
        <artifactId>sdk-model</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.getelements.elements</groupId>
        <artifactId>sdk-service</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.getelements.elements</groupId>
        <artifactId>sdk-spi-guice</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.getelements.elements</groupId>
        <artifactId>sdk-jakarta-rs</artifactId>
    </dependency>

    <!-- Third-party (versions + scopes from BOM) -->
    <dependency>
        <groupId>com.google.inject</groupId>
        <artifactId>guice</artifactId>
    </dependency>
    <dependency>
        <groupId>jakarta.ws.rs</groupId>
        <artifactId>jakarta.ws.rs-api</artifactId>
    </dependency>
    <dependency>
        <groupId>jakarta.websocket</groupId>
        <artifactId>jakarta.websocket-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-annotations</artifactId>
    </dependency>
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-jaxrs2-jakarta</artifactId>
    </dependency>
</dependencies>
```

Note what is **gone**:
- `sdk-spi` (merged into `sdk-spi-guice` in 3.7)
- `sdk-logback`, `logback-classic` (moved to the `debug` module)
- `sdk-local`, `sdk-local-maven` (moved to the `debug` module)
- `swagger-annotations-jakarta`, `swagger-integration-jakarta` (replaced by `swagger-annotations`)
- All Jackson `<exclusions>` (no longer needed)

### ELM archive build plugins

Replace all old `<build>` plugin configuration with the following. The new archive has three directories:

```
<groupId>.<artifactId>-<version>.elm  (zip)
├── api/        ← exported API classifier jars
├── lib/        ← runtime (non-provided) dependency jars
└── classpath/  ← compiled classes + resources
```

```xml
<properties>
    <elm.staging.dir>
        ${project.build.directory}/${project.groupId}.${project.artifactId}-${project.version}
    </elm.staging.dir>
</properties>

<build>
    <plugins>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <executions>
                <!-- Copy API classifier jars into staging/api/ -->
                <execution>
                    <id>elm-copy-api-deps</id>
                    <phase>prepare-package</phase>
                    <goals><goal>copy-dependencies</goal></goals>
                    <configuration>
                        <outputDirectory>${elm.staging.dir}/api</outputDirectory>
                        <includeGroupIds>${project.groupId}</includeGroupIds>
                        <includeClassifiers>${api.classifier}</includeClassifiers>
                        <prependGroupId>true</prependGroupId>
                    </configuration>
                </execution>
                <!-- Copy non-provided runtime jars into staging/lib/ -->
                <execution>
                    <id>elm-copy-lib-deps</id>
                    <phase>prepare-package</phase>
                    <goals><goal>copy-dependencies</goal></goals>
                    <configuration>
                        <outputDirectory>${elm.staging.dir}/lib</outputDirectory>
                        <excludeScope>provided</excludeScope>
                        <prependGroupId>true</prependGroupId>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-antrun-plugin</artifactId>
            <version>3.1.0</version>
            <executions>
                <!-- Stage compiled classes and src/main/resources into staging/classpath/ -->
                <execution>
                    <id>elm-stage-classpath</id>
                    <phase>prepare-package</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <copy todir="${elm.staging.dir}/classpath" failonerror="false">
                                <fileset dir="${project.build.outputDirectory}"
                                         erroronmissingdir="false" includes="**/*"/>
                            </copy>
                            <copy todir="${elm.staging.dir}/classpath" failonerror="false">
                                <fileset dir="${basedir}/src/main/resources"
                                         erroronmissingdir="false" includes="**/*"/>
                            </copy>
                        </target>
                    </configuration>
                </execution>
                <!-- Zip the staging directory into a .elm file -->
                <execution>
                    <id>elm-create-archive</id>
                    <phase>package</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <mkdir dir="${elm.staging.dir}/api"/>
                            <mkdir dir="${elm.staging.dir}/lib"/>
                            <mkdir dir="${elm.staging.dir}/classpath"/>
                            <zip destfile="${elm.staging.dir}.elm" basedir="${elm.staging.dir}"/>
                        </target>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- Attach the .elm file as a Maven artifact so it is installed/deployed -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>build-helper-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>attach-elm</id>
                    <phase>package</phase>
                    <goals><goal>attach-artifact</goal></goals>
                    <configuration>
                        <artifacts>
                            <artifact>
                                <file>${elm.staging.dir}.elm</file>
                                <type>elm</type>
                            </artifact>
                        </artifacts>
                    </configuration>
                </execution>
            </executions>
        </plugin>

    </plugins>
</build>
```

**What to remove from your old build:**
- `maven-assembly-plugin` (`zip-target-dir` execution) and `src/assembly/zip.xml`
- `maven-dependency-plugin` executions: `copy-element-deps`, `copy-element-dependencies`
- `maven-antrun-plugin` execution: `copy-element-build`
- `maven-resources-plugin` execution: `copy-element-resources`

---

## Step 5 — Replace `src/test/java/Main.java` with the `debug` Module

The old `Main.java` used a flat `ElementsLocalBuilder` API that required manually loading `.properties` files from disk. This approach is removed in 3.7.

### Old API (removed)

```java
// Loaded attributes from external .properties files
final var elementProperties = new Properties();
try (final var is = new FileInputStream("element-example-deployment/dev.getelements.element.attributes.properties")) {
    elementProperties.load(is);
}

final var local = ElementsLocalBuilder.getDefault()
        .withElementNamed(
                "example",
                "com.mystudio.mygame",
                PropertiesAttributes.wrap(elementProperties))
        .build();

try (local) {
    local.start();
    local.run();
}
```

Problems this had:
- Required two hard-coded `.properties` files on disk relative to the working directory
- Package-name based element discovery was fragile
- `PropertiesAttributes`, `UserDao` wiring, and manual user-creation bootstrapping are gone
- `try-with-resources` pattern is replaced

### New API (3.7)

Create `debug/pom.xml`:

```xml
<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example.element</groupId>
        <artifactId>parent</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>debug</artifactId>

    <dependencies>
        <dependency>
            <groupId>dev.getelements.elements</groupId>
            <artifactId>sdk-local</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.getelements.elements</groupId>
            <artifactId>sdk-local-maven</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.getelements.elements</groupId>
            <artifactId>sdk-logback</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>
    </dependencies>

</project>
```

Create `debug/src/main/java/run.java`:

```java
import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

public class run {
    public static void main(final String[] args) {

        final var local = ElementsLocalBuilder.getDefault()
                .withSourceRoot()
                .withDeployment(builder -> builder
                        .useDefaultRepositories(true)
                        .elementPath()
                            .addSpiBuiltin("GUICE_7_0_0")
                            .addApiArtifact("com.example.element:api:1.0-SNAPSHOT")
                            .addElementArtifact("com.example.element:element:1.0-SNAPSHOT")
                        .endElementPath()
                        .build()
                )
                .build();

        local.start();
        local.run();
    }
}
```

### Builder API changes

| Old method | New equivalent |
|---|---|
| `.withElementNamed(name, pkg, attrs)` | `.withDeployment(builder -> ...)` with explicit [Maven artifact coordinates](https://maven.apache.org/pom.html#Maven_Coordinates) (`groupId:artifactId:version`) for the SPI, API, and element jars |
| `PropertiesAttributes.wrap(props)` | Removed — attributes declared as `@ElementDefaultAttribute` in code |
| `local.getRootElementRegistry()` | Removed from local runner bootstrap |
| `try (local) { local.start(); local.run(); }` | `local.start(); local.run();` (no try-with-resources needed) |

### `addSpiBuiltin` values

The [SPI (Service Provider Implementation)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html) is now explicitly named in the builder rather than inferred from the classpath. For the Guice SPI:

```java
.addSpiBuiltin("GUICE_7_0_0")
```

### Element attributes

In the old approach, `dev.getelements.elements.app.serve.prefix` and similar properties were set in the `.properties` file loaded at runtime. In 3.7, declare them as constants in your `Application` class using `@ElementDefaultAttribute`:

```java
@ElementDefaultAttribute("example-element")
public static final String APPLICATION_PREFIX = "dev.getelements.elements.app.serve.prefix";

@ElementDefaultAttribute("true")
public static final String AUTH_ENABLED = "dev.getelements.elements.auth.enabled";
```

This means you can **delete** `element-example-deployment/dev.getelements.element.attributes.properties` (no longer read by the local runner).

> **Before running the debug module**, build the whole project first so all artifacts are available locally:
> ```bash
> mvn install
> ```
> Then run the `run` class from the `debug` module in your IDE.

---

## Step 6 — Code-Level Changes

### `HelloWorldApplication.java`

Remove the `getProperties()` override that disabled MOXy JSON. This dependency on Jersey internals is no longer needed:

```java
// Remove this entire method:
@Override
public Map<String,Object> getProperties() {
    final Map<String,Object> props = new HashMap<>();
    props.put(ServerProperties.MOXY_JSON_FEATURE_DISABLE, true);
    return props;
}

// Also remove these imports:
import java.util.HashMap;
import java.util.Map;
import org.glassfish.jersey.server.ServerProperties;
```

---

## Step 7 — Deployment Property Files

In the legacy project, `element-example-deployment/dev.getelements.element.attributes.properties` carried runtime configuration that the local `Main.java` loaded explicitly. In 3.7:

- Element attributes are embedded as `@ElementDefaultAttribute` annotations (see Step 5)
- The local runner no longer reads `.properties` files from disk
- **Delete** `dev.getelements.element.attributes.properties` if you have migrated all its keys to annotations

If you have custom attributes beyond the two shown above, add a `public static final String` field for each one in your `Application` class annotated with `@ElementDefaultAttribute`.

---

## Step 8 — Remove Deleted Artifacts

| File / artifact | Action |
|---|---|
| `src/assembly/zip.xml` | Delete |
| `src/test/java/Main.java` | Delete |
| `element-example-deployment/dev.getelements.element.attributes.properties` | Delete (attributes now in code) |
| `element-example-deployment/dev.getelements.elements.crossfire.properties` | Delete (Crossfire not bundled) |
| `namazu-crossfire` Maven profile | Delete from POM |
| `sdk-spi` dependency | Remove — no longer exists separately |
| `swagger-annotations-jakarta`, `swagger-integration-jakarta` | Remove — replaced by `swagger-annotations` |
| Jackson `<exclusions>` on swagger deps | Remove |

---

## Summary Checklist

**Project structure:**
- [ ] Convert root `pom.xml` to `<packaging>pom</packaging>` parent with `<modules>`
- [ ] Change root `groupId` from `org.example` to `com.example.element`
- [ ] Change root `artifactId` from `ElementSample` to `parent`
- [ ] *(optional)* Create `api/` module with `maven-jar-plugin` classified-jar execution — only needed to export types to other Elements
- [ ] *(optional)* Move shared interfaces into `api/src/main/java/`
- [ ] Create `element/` module, move `src/main/java` + `src/main/resources` into it
- [ ] Create `debug/` module with `sdk-local`, `sdk-local-maven`, `sdk-logback`

**Root pom.xml:**
- [ ] Remove all `<dependencies>` (moved to child modules)
- [ ] Remove `<build>` block (moved to `element/pom.xml`)
- [ ] Remove `namazu-crossfire` profile
- [ ] Add `sdk-bom` import in `<dependencyManagement>`
- [ ] *(optional)* Add `api.classifier` property and classified-api `<dependencyManagement>` entries — only needed if creating an `api` module
- [ ] Replace old distribution-path properties

**element/pom.xml:**
- [ ] Add ELM archive build: `maven-dependency-plugin` (elm-copy-api-deps, elm-copy-lib-deps) + `maven-antrun-plugin` (elm-stage-classpath, elm-create-archive) + `build-helper-maven-plugin` (attach-elm)
- [ ] Remove `sdk-spi`, `sdk-logback`, `logback-classic`, `sdk-local`, `sdk-local-maven` dependencies
- [ ] Remove `swagger-annotations-jakarta`, `swagger-integration-jakarta`, Jackson exclusions
- [ ] *(optional)* Add classified API jar dependency — only needed if `api` module exists

**debug module:**
- [ ] Write `debug/src/main/java/run.java` using new `ElementsLocalBuilder` fluent API
- [ ] Call `addSpiBuiltin("GUICE_7_0_0")`, `addApiArtifact(...)`, `addElementArtifact(...)`

**Code:**
- [ ] Remove `getProperties()` override from `Application` subclass
- [ ] Update `OpenAPISecurityConfig` to use `AuthSchemes.SESSION_SECRET` from `sdk-jakarta-rs`
- [ ] Replace any properties-file-loaded attributes with `@ElementDefaultAttribute` constants
- [ ] Delete `src/assembly/zip.xml`, `src/test/java/Main.java`, old `.properties` deployment files

---

## Further Reading

- **[Introduction to Dependency Mechanism — Bill of Materials (BOM) POMs](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms)** — Apache Maven official guide explaining how BOM imports work in `<dependencyManagement>` and why they prevent version conflicts across multi-module projects. This is the mechanism behind the `sdk-bom` import introduced in 3.7.

- **[`java.util.ServiceLoader` — Java 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html)** — Java 21 reference for the Service Provider Interface (SPI) mechanism. Elements uses `ServiceLoader` internally to locate and initialise the Element runtime; the `addSpiBuiltin("GUICE_7_0_0")` call in the builder selects a named provider configuration rather than leaving discovery to classpath scanning.

- **[POM Reference — Maven Coordinates](https://maven.apache.org/pom.html#Maven_Coordinates)** — Apache Maven official reference for `groupId`, `artifactId`, `version`, and `classifier` — the coordinate components that identify every artifact referenced in this migration guide, including the `groupId:artifactId:version` strings passed to `addApiArtifact(...)` and `addElementArtifact(...)`.