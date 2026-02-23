# Migration Guide: 3.6 → 3.7

This document describes the breaking changes and required steps when upgrading a custom Element from the Elements 3.6 SDK to 3.7.

## The Fundamental Change in 3.7

The central shift in 3.7 is that **Elements are now loaded and referenced entirely by their Maven coordinates**. Every artifact the runtime needs (the SPI, the API jars exported to other Elements, and the element implementation itself) is identified by a `groupId:artifactId:version` coordinate and resolved through standard Maven repositories. To solve deployment and packaging issues, Namazu Elements also can load the SPI types just in time for deployment eliminating the need to package the SPI bundles in the release.

This replaces the previous git-based deployment system, in which the runtime discovered code by inspecting a checked-out repository layout. The new approach brings several concrete benefits:

- **Standard tooling:** any Maven-compatible repository (Nexus, Artifactory, GitHub Packages, Maven Central) can serve as a deployment target, with no custom git hooks or repository layout requirements.
- **Reproducible builds:** exact artifact versions are pinned in the deployment descriptor rather than inferred from git state.
- **Isolated classpaths:** the `api/`, `lib/`, and `classpath/` sections of the `.elm` archive map directly to separate classloader layers inside the runtime, eliminating the classpath pollution problems that the old flat-distribution approach was prone to.
- **Inter-element API contracts:** the classified API jar (`api/` directory in the archive) makes the boundary between what an Element exports and what it keeps private explicit and enforced at load time.
- **Builtin SPI Configurations:** Namazu Elements does not "know" how an Element loads. Rather it uses an SPI (Service Provider Implementation) on the Element's classpath to perform the loading. Prior to 3.7 we bundled this in the Element's implementation and it forced lock-step versioning due to binary incompatibilities. By providing this on-the-fly we have eliminated that issue by providing pre-determined configurations that can cross version to version.

All the specific migration steps below flow from this architectural change.

---

## 1. Bump the Elements Version

In your root `pom.xml`, update the `elements.version` property:

```xml
<!-- Before (3.6) -->
<elements.version>3.6.0-SNAPSHOT</elements.version>

<!-- After (3.7) -->
<elements.version>3.7.0-SNAPSHOT</elements.version>
```

---

## 2. Switch to the SDK BOM

3.7 introduces `sdk-bom`, a [bill-of-materials (BOM) POM](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms) that centralises all SDK dependency versions and scopes. Replace any hand-listed SDK entries in `<dependencyManagement>` with a single BOM import:

```xml
<!-- Before (3.6): individual SDK entries with explicit versions -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>dev.getelements.elements</groupId>
            <artifactId>sdk</artifactId>
            <version>${elements.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- … more individual entries … -->
    </dependencies>
</dependencyManagement>

<!-- After (3.7): single BOM import -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>dev.getelements.elements</groupId>
            <artifactId>sdk-bom</artifactId>
            <version>${elements.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Once the BOM is imported, remove `<version>` and `<scope>` from every `dev.getelements.elements` dependency in your child modules — the BOM supplies the correct values automatically.

### Version properties now live in the root POM

Move any version properties that were previously declared in `element/pom.xml` up to the root `pom.xml` so they are visible to all modules:

```xml
<properties>
    <swagger.version>2.2.22</swagger.version>
    <guice.version>7.0.0</guice.version>
    <rs.api>4.0.0</rs.api>
    <jakarta.websocket.version>2.1.0</jakarta.websocket.version>
    <maven.build.timestamp.format>yyyy-MM-dd'T'HH:mm:ss'Z'</maven.build.timestamp.format>
    <!-- … -->
</properties>
```

---

## 3. Add the API Classifier Jar *(optional)*

> **This step is only required if your Element needs to export interfaces or types to other Elements in the same deployment.** The `api` module concept was introduced in 3.6 but the cross-element type export mechanism was not fully supported at runtime until 3.7. If your Element operates standalone, you can skip this step entirely. Adding it now is good practice for future-proofing, but it has no effect on your Element's behaviour unless another Element explicitly depends on it.

3.7 formalises the *classified* API jar that is exported to other Elements through the `.elm` archive. Two changes are required.

### 3a. Root pom.xml — declare the classifier property and artifacts

```xml
<properties>
    <api.classifier>${project.groupId}.api</api.classifier>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- unclassified api (compile-time only) -->
        <dependency>
            <groupId>com.example.element</groupId>
            <artifactId>api</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- classified api jar (copied into elm/api/) -->
        <dependency>
            <groupId>com.example.element</groupId>
            <artifactId>api</artifactId>
            <version>${project.version}</version>
            <classifier>${api.classifier}</classifier>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 3b. api/pom.xml — produce the classified jar during package

```xml
<build>
    <plugins>
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
```

Also mark the `sdk` dependency in `api/pom.xml` as `<scope>provided</scope>` and keep the API's dependency graph as lean as possible — only interfaces, plain DTOs, and classes that depend on the core SDK or standard Java APIs.

---

## 4. Replace the Zip Distribution with the ELM Archive Format

The old approach (Maven Assembly Plugin + `zip.xml`) is replaced by a structured `.elm` archive built with three standard Maven plugins. **Delete `element/src/assembly/zip.xml`** and remove any `maven-assembly-plugin` configuration from `element/pom.xml`.

### New archive layout

```
<groupId>.<artifactId>-<version>.elm  (zip file)
└── <groupId>.<artifactId>/
    ├── dev.getelements.element.manifest.properties  ← build metadata
    ├── api/        ← classified API jars exported to other Elements
    ├── lib/        ← runtime (non-provided) dependency jars
    └── classpath/  ← compiled classes + src/main/resources contents
```

### element/pom.xml — new build section

Add staging directory properties and four plugin executions:

```xml
<properties>
    <elm.staging.dir>
        ${project.build.directory}/${project.groupId}.${project.artifactId}-${project.version}
    </elm.staging.dir>
    <elm.element.dir>${elm.staging.dir}/${project.groupId}.${project.artifactId}</elm.element.dir>
</properties>

<build>
    <plugins>

        <!-- 1. Copy dependencies into staging subdirectories -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <executions>
                <!-- API classifier jars → api/ -->
                <execution>
                    <id>elm-copy-api-deps</id>
                    <phase>prepare-package</phase>
                    <goals><goal>copy-dependencies</goal></goals>
                    <configuration>
                        <outputDirectory>${elm.element.dir}/api</outputDirectory>
                        <includeGroupIds>${project.groupId}</includeGroupIds>
                        <includeClassifiers>${api.classifier}</includeClassifiers>
                        <prependGroupId>true</prependGroupId>
                    </configuration>
                </execution>
                <!-- Runtime (non-provided) jars → lib/ -->
                <execution>
                    <id>elm-copy-lib-deps</id>
                    <phase>prepare-package</phase>
                    <goals><goal>copy-dependencies</goal></goals>
                    <configuration>
                        <outputDirectory>${elm.element.dir}/lib</outputDirectory>
                        <excludeScope>provided</excludeScope>
                        <prependGroupId>true</prependGroupId>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- 2. Stage classes/resources, write manifest, and zip into .elm -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-antrun-plugin</artifactId>
            <version>3.1.0</version>
            <executions>
                <execution>
                    <id>elm-stage-classpath</id>
                    <phase>prepare-package</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <copy todir="${elm.element.dir}/classpath" failonerror="false">
                                <fileset dir="${project.build.outputDirectory}"
                                         erroronmissingdir="false" includes="**/*"/>
                            </copy>
                            <copy todir="${elm.element.dir}/classpath" failonerror="false">
                                <fileset dir="${basedir}/src/main/resources"
                                         erroronmissingdir="false" includes="**/*"/>
                            </copy>
                        </target>
                    </configuration>
                </execution>
                <!-- Write build metadata into the element manifest -->
                <execution>
                    <id>elm-write-manifest</id>
                    <phase>prepare-package</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <mkdir dir="${elm.element.dir}"/>
                            <exec executable="git" outputproperty="git.revision"
                                  failifexecutionfails="false" errorproperty="git.error">
                                <arg value="rev-parse"/>
                                <arg value="HEAD"/>
                            </exec>
                            <property name="git.revision" value="unknown"/>
                            <echo file="${elm.element.dir}/dev.getelements.element.manifest.properties"
                                  append="false">Element-Version=${project.version}
Element-Build-Time=${maven.build.timestamp}
Element-Revision=${git.revision}
Element-Builtin-Spis=DEFAULT
</echo>
                        </target>
                    </configuration>
                </execution>
                <execution>
                    <id>elm-create-archive</id>
                    <phase>package</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <mkdir dir="${elm.element.dir}/api"/>
                            <mkdir dir="${elm.element.dir}/lib"/>
                            <mkdir dir="${elm.element.dir}/classpath"/>
                            <zip destfile="${elm.staging.dir}.elm" basedir="${elm.staging.dir}"/>
                        </target>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- 3. Attach the .elm file as a Maven artifact -->
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

Also add the classified API jar as a dependency inside `element/pom.xml` so it is picked up by the `elm-copy-api-deps` execution:

```xml
<dependency>
    <groupId>com.example.element</groupId>
    <artifactId>api</artifactId>
    <classifier>${api.classifier}</classifier>
</dependency>
```

---

## 5. Replace `test/Main.java` with the `debug` Module

The old `element/src/test/java/Main.java` that manually wired property files is removed. Local debugging now lives in a dedicated `debug` Maven module that uses the new 3.7 `ElementsLocalBuilder` classloading API.

### 5a. Add the module to the root pom.xml

```xml
<modules>
    <module>api</module>
    <module>element</module>
    <module>debug</module>   <!-- add this -->
</modules>
```

### 5b. Create debug/pom.xml

```xml
<project ...>
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

### 5c. Create the entry-point class

The new classloading scheme requires you to declare the SPI, API artifact, and element artifact explicitly via the builder:

```java
// debug/src/main/java/run.java
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

Key builder methods:

| Method | Purpose |
|---|---|
| `withSourceRoot()` | Locates the parent POM and runs `mvn -DskipTests install` automatically before starting, so all artifacts are up to date in the local Maven repository |
| `useDefaultRepositories(true)` | Includes the standard Maven repositories for resolution |
| `addSpiBuiltin("GUICE_7_0_0")` | Selects the built-in Guice 7 SPI loader |
| `addApiArtifact(coords)` | Registers an API classifier jar on the shared API classpath; `coords` is a standard Maven [artifact coordinate](https://maven.apache.org/pom.html#Maven_Coordinates) in `groupId:artifactId:version` form |
| `addElementArtifact(coords)` | Registers the element implementation jar on the element classpath; same coordinate format |

> **Working directory:** `withSourceRoot()` expects the process working directory to be the **parent POM directory** (the project root). When running from an IDE, make sure the run configuration's working directory is set to the root of the multi-module project, not the `debug/` subdirectory. The SDK will locate `pom.xml` there and invoke Maven against it automatically before loading your Element.

---

## 6. Remove the `namazu-crossfire` Maven Profile

If your `element/pom.xml` contains a `<profile>` block for `namazu-crossfire` (used in 3.6 for a specific deployment path), remove it entirely. The crossfire deployment approach is superseded by the `.elm` archive format.

---

## Summary Checklist

- [ ] `elements.version` → `3.7.0-SNAPSHOT` in root `pom.xml`
- [ ] Replace individual SDK `<dependencyManagement>` entries with the `sdk-bom` import
- [ ] Move version properties from `element/pom.xml` to root `pom.xml`
- [ ] *(optional)* Add `api.classifier` property and classified-api entries to root `pom.xml` — only needed to export types to other Elements
- [ ] *(optional)* Add `maven-jar-plugin` classified-jar execution to `api/pom.xml`
- [ ] Remove `element/src/assembly/zip.xml` and old assembly/antrun/distribution plugin config from `element/pom.xml`
- [ ] Add `maven.build.timestamp.format` property to root `pom.xml`
- [ ] Add ELM archive build (dependency-plugin + antrun + build-helper) to `element/pom.xml`
- [ ] *(optional)* Add classified API dependency to `element/pom.xml` — only needed if `api` module exists
- [ ] Delete `element/src/test/java/Main.java`
- [ ] Add `debug` Maven module with `sdk-local`, `sdk-local-maven`, `sdk-logback` dependencies
- [ ] Add `debug` entry-point using the new `ElementsLocalBuilder` fluent API
- [ ] Update `OpenAPISecurityConfig` import for `SESSION_SECRET`
- [ ] Remove `namazu-crossfire` Maven profile if present

---

## Further Reading

- **[Introduction to Dependency Mechanism — Bill of Materials (BOM) POMs](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms)** — Apache Maven official guide explaining how BOM imports work in `<dependencyManagement>` and why they prevent version conflicts across multi-module projects.

- **[`java.util.ServiceLoader` — Java 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html)** — Java 21 reference for the Service Provider Interface (SPI) mechanism that Elements uses to locate and load the correct Element runtime. Understanding `ServiceLoader` explains why `addSpiBuiltin` selects a named provider configuration rather than scanning the classpath.

- **[POM Reference — Maven Coordinates](https://maven.apache.org/pom.html#Maven_Coordinates)** — Apache Maven official reference for `groupId`, `artifactId`, `version`, and `classifier` — the coordinate components used throughout this migration guide in `addApiArtifact(...)` and `addElementArtifact(...)` calls.