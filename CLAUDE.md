# CLAUDE.md — element-example

This is a reference/example project for **Namazu Elements 3.7**, demonstrating how to build a custom Element using the multi-module Maven structure, REST endpoints, Guice DI, and the `.elm` archive format.

## Project Structure

```
element-example/
├── api/          # Exported interfaces (other Elements depend on this)
├── element/      # Implementation module — builds the .elm archive
├── debug/        # Local development runner (not deployed)
└── services-dev/ # Docker services (MongoDB) for local dev
```

**Module roles:**
- `api` — Pure interfaces + DTOs exported to other Elements via a classified JAR
- `element` — REST endpoints, services, Guice modules; compiles to `.elm` archive
- `debug` — Local Elements runtime harness; never deployed

## Build & Run

```bash
# Build everything
mvn install

# Start local MongoDB (required for local testing)
docker compose -f services-dev/docker-compose.yml up -d

# Run locally (from project root)
mvn -pl debug exec:java
```

## Key Patterns

### Element Declaration (`package-info.java`)
Every Element package must have a `package-info.java`:
```java
@ElementDefinition(recursive = true)
@GuiceElementModule(MyGameModule.class)
@ElementDependency("dev.getelements.elements.sdk.dao")
@ElementDependency("dev.getelements.elements.sdk.service")
package com.mystudio.mygame;
```

### REST Endpoints (Jakarta RS)
- Annotate endpoint class with `@Path`
- Register all endpoint classes in a `Application` subclass annotated with `@ElementServiceImplementation` + `@ElementServiceExport(Application.class)`
- Services are **not** injected via `@Inject` in JAX-RS endpoints (the container instantiates them, not Guice). Use the service locator instead:

```java
private final Element element = ElementSupplier.getElementLocal(MyEndpoint.class).get();
private final MyService svc = element.getServiceLocator().getInstance(MyService.class);
```

### Guice Module Pattern
Use `PrivateModule` to isolate bindings; expose only what other Elements need:
```java
public class MyModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(MyService.class).to(MyServiceImpl.class);
        expose(MyService.class);
    }
}
```
SDK services (`UserService`, DAOs) are available for `@Inject` because of the `@ElementDependency` declarations in `package-info.java`.

### Service Export (api module)
```java
@ElementServiceExport
public interface MyService { ... }
```
Combined with Guice `expose()`, other Elements can call `serviceLocator.getInstance(MyService.class)`.

### Authentication
- Enable auth filter: `@ElementDefaultAttribute("true")` for `dev.getelements.elements.auth.enabled`
- Mark authenticated endpoints: `@SecurityRequirement(name = AuthSchemes.SESSION_SECRET)`
- Check user level: `User.Level.UNPRIVILEGED` is the sentinel for unauthenticated/guest

### WebSocket
- Annotate class with `@ServerEndpoint("/path")` — auto-discovered
- Use `@OnOpen`, `@OnMessage`, `@OnClose`, `@OnError`
- Get services via `ElementSupplier.getElementLocal()` (same as REST)
- No `Application` subclass needed; `package-info.java` only needs `@ElementDefinition`

## Database Access (Morphia)

For database access, see **[MORPHIA.md](MORPHIA.md)**. It covers `Transaction`, `Datastore`, DAO injection, `@ElementTypeRequest` for classloader visibility, and retry behaviour.

> **Note:** If you are using Morphia directly (custom queries, `@Entity` classes, `Datastore` injection), refer to [MORPHIA.md](MORPHIA.md) before writing any database code.

## Maven Dependency Scopes
- `sdk`, `sdk-local`: `provided` — supplied by the runtime
- `api` module (your own): `provided` in the `element` module
- `sdk-spi` + `sdk-spi-guice`: **bundled** (not provided) — must ship inside the `.elm`
- Use `sdk-logback` (not plain logback) to avoid classpath conflicts at runtime

## Key SDK Types
| Type | Purpose |
|------|---------|
| `ElementSupplier.getElementLocal(Class<?>)` | Get the Element from its own classpath |
| `Element.getServiceLocator()` | Access Guice-managed services |
| `ServiceLocator.getInstance(Class<T>)` | Get an injected instance (throws if missing) |
| `ServiceLocator.findInstance(Class<T>)` | Returns `Optional<Supplier<T>>` |
| `ElementScope` / `element.withScope()` | Thread-local scope with mutable attributes |
| `Element.publish(Event)` | Broadcast events to other Elements |
| `User.Level.UNPRIVILEGED` | Sentinel for unauthenticated/guest users |
| `AuthSchemes.SESSION_SECRET` | Header name for session auth (`"session_secret"`) |

## Static & UI Content

- **`element/src/main/static/`** — static files served at `/app/static/{prefix}/`
- **`element/src/main/ui/`** — UI plugin files served at `/app/ui/{prefix}/`

The Maven build (antrun `elm-stage-static-content`) copies both directories into the `.elm` archive automatically. No extra configuration is needed to serve files — just place them in the right source directory.

### Controlling Static Serving with Attributes

The `StaticRuleEngine` reads the following keys from the Element's attributes (namespace is `static` or `ui` depending on the tree):

| Attribute key | Purpose | Default |
|---------------|---------|---------|
| `dev.getelements.static.index` | File served at the context root | `index.html` |
| `dev.getelements.static.rule.<name>.regex` | Regex rule matching file paths | — |
| `dev.getelements.static.rule.<name>.header.<Header>.value` | Response header template for matched files | — |
| `dev.getelements.static.error.<code>` | File served for HTTP error code (e.g. `404`) | — |
| `dev.getelements.ui.index` | Same as above for the `ui` tree | `index.html` |
| `dev.getelements.ui.rule.<name>.regex` | Same as above for the `ui` tree | — |
| `dev.getelements.ui.rule.<name>.header.<Header>.value` | Same as above for the `ui` tree | — |
| `dev.getelements.ui.error.<code>` | Same as above for the `ui` tree | — |
| `dev.getelements.element.static.uri` | Override the full serve URI for standard content | `/app/static/{prefix}` |
| `dev.getelements.element.ui.uri` | Override the full serve URI for UI content | `/app/ui/{prefix}` |

Header value templates support: `$filename`, `$path`, `$[0]` (full match), `$[N]` (capture group N).

**Example** — add a `Cache-Control` header to all `.js` files and define a 404 page:
```properties
dev.getelements.static.rule.scripts.regex=.*\\.js
dev.getelements.static.rule.scripts.header.Cache-Control.value=public, max-age=31536000
dev.getelements.static.error.404=errors/404.html
```

### Embedding Attributes in the ELM

The loader reads `dev.getelements.element.attributes.properties` from the **element root** inside the `.elm` archive (same level as `api/`, `lib/`, `classpath/`, and the manifest).

Place your attributes file at:
```
element/src/main/elm/dev.getelements.element.attributes.properties
```

Then add an antrun copy step to `element/pom.xml` inside the `elm-stage-classpath` execution (or as its own execution in `prepare-package`) to stage it to the element root:

```xml
<copy todir="${elm.element.dir}" failonerror="false">
    <fileset dir="${basedir}/src/main/elm" erroronmissingdir="false" includes="**/*"/>
</copy>
```

This places the file at `<groupId>.<artifactId>/dev.getelements.element.attributes.properties` inside the ZIP, which is the path `DirectoryElementPathLoader` expects.

> `@ElementDefaultAttribute` on static fields in your Java classes provides defaults for any key — the attributes file overrides them at deploy time without recompiling.

## Custom APIs

Custom server-side logic is exposed via two Jakarta EE APIs:

- **REST APIs** — implemented with [Jakarta RESTful Web Services](https://jakarta.ee/specifications/restful-ws/4.0/) (Jakarta RS / JAX-RS)
- **WebSockets** — implemented with [Jakarta WebSocket](https://jakarta.ee/specifications/websocket/2.1/)

## Elements REST API (OpenAPI)

The full Elements platform REST API is available as an OpenAPI spec at:

```
http://localhost:8080/api/rest/openapi.json
```

> **Note:** If this URL returns 404 or is unreachable, the local Elements instance is not running. Run the debug script first (`mvn -pl debug exec:java`) to bring the instance online, then retry.

## Package Layout Convention
```
com.mystudio.mygame/
  ├── rest/           REST endpoints
  ├── service/        Business logic
  ├── model/          Request/response DTOs
  ├── guice/          Guice modules
  └── package-info.java
```
