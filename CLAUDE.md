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

- **`element/src/main/static/`** — static file content served by the Element
- **`element/src/main/ui/`** — UI plugin content served by the Element

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
