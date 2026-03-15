# MORPHIA.md — Database Access with Morphia

This document explains how to use the MongoDB/Morphia integration available through the Elements DAO layer inside a custom Element.

---

## Overview

Elements uses [Morphia](https://morphia.dev/) as its MongoDB ODM. The DAO Element (`dev.getelements.elements.sdk.dao`) exposes `Transaction`, `Datastore`, and all platform DAO interfaces into your Element's service locator. You never instantiate DAOs directly — you always acquire them through a `Transaction`, which ensures every operation in a unit of work shares the same `MorphiaSession`.

---

## Step 1 — Declare the DAO Dependency

In `package-info.java`, add `@ElementDependency` for the DAO layer:

```java
@ElementDefinition(recursive = true)
@GuiceElementModule(MyGameModule.class)
@ElementDependency("dev.getelements.elements.sdk.dao")
@ElementDependency("dev.getelements.elements.sdk.service")
package com.mystudio.mygame;

import com.mystudio.mygame.guice.MyGameModule;
import dev.getelements.elements.sdk.annotation.ElementDefinition;
import dev.getelements.elements.sdk.annotation.ElementDependency;
import dev.getelements.elements.sdk.spi.guice.annotations.GuiceElementModule;
```

This wires in `MongoDaoModule`, which exposes `Transaction`, `Datastore`, and all 40+ platform DAO interfaces into your Element's injector.

---

## Step 2 — Use `Transaction` to Acquire DAOs

`Transaction` is an `AutoCloseable` built on a **snapshot + retry** model to handle MongoDB transient write conflicts. Always prefer the `performAndClose()` convenience method, which manages the retry loop automatically:

```java
public class MyService {

    @Inject
    private Provider<Transaction> transactionProvider;

    public User findUser(String userId) {
        return transactionProvider.get().performAndClose(txn -> {
            UserDao userDao = txn.getDao(UserDao.class);
            return userDao.getUser(userId);
        });
    }
}
```

For manual control (commit/rollback explicitly):

```java
try (var txn = transactionProvider.get()) {
    txn.start();
    var dao = txn.getDao(UserDao.class);
    // ... work ...
    txn.commit();  // throws RetryException on transient failure
}
```

`RetryException` is a runtime exception. Inspect `getRecommendDelay()` for backoff, or call `waitForRecommendedDelay()` to sleep automatically.

> **Important:** DAOs obtained via `txn.getDao()` are only valid for the lifetime of that transaction. Do not hold references across transaction boundaries.

---

## Step 3 — Inject the Raw `Datastore` (Custom Queries)

If the platform DAOs don't cover your use case, inject `Datastore` directly for full Morphia query access:

```java
@Inject
private Datastore datastore;

public List<MyEntity> search(String field, String value) {
    return datastore.find(MyEntity.class)
        .filter(Filters.eq(field, value))
        .iterator().toList();
}
```

---

## Step 4 — `@ElementTypeRequest` (if Morphia types are blocked)

The `PermittedTypesClassLoader` may prevent your Element from seeing `dev.morphia.*` types directly. If you encounter `ClassNotFoundException` or visibility errors for Morphia types, declare them explicitly in `package-info.java`:

```java
// Exact types:
@ElementTypeRequest(value = {
    "dev.morphia.Datastore",
    "dev.morphia.transactions.MorphiaSession"
})

// Or permit the entire Morphia package tree:
@ElementTypeRequest(
    request = TypeRequest.Wildcard.class,
    value = "dev.morphia."
)
```

`@ElementTypeRequest` accepts three `TypeRequest` strategies:
- `TypeRequest.Literal` (default) — exact binary class name match
- `TypeRequest.Regex` — regular expression match
- `TypeRequest.Wildcard` — prefix match (e.g. `"dev.morphia."` permits all subtypes)

---

## `Transaction` API Reference

```java
public interface Transaction extends AutoCloseable {
    <DaoT> DaoT getDao(Class<DaoT> daoT);
    void start();
    void commit() throws RetryException;
    void rollback();
    void close();
    boolean isActive();

    // Recommended — handles retry loop automatically:
    default <T> T performAndClose(Function<Transaction, T> op);
    default void performAndCloseV(Consumer<Transaction> op);
}
```

Retry behaviour is configurable via Element attributes:

| Attribute | Default | Description |
|-----------|---------|-------------|
| `dev.getelements.elements.mongo.transaction.retry.count` | `32` | Max retry attempts |
| `dev.getelements.elements.mongo.transaction.retry.timeout` | `30000` ms | Total retry window |

---

## Exposed DAO Interfaces

The following are a subset of the DAO interfaces exposed by `MongoDaoModule`. Acquire all of them via `txn.getDao(XxxDao.class)`:

| Interface | Purpose |
|-----------|---------|
| `UserDao` | Platform user records |
| `ProfileDao` | User profiles |
| `ApplicationDao` | Application records |
| `SessionDao` | Session management |
| `ItemDao` | Game item definitions |
| `InventoryItemDao` | Per-user item inventory |
| `MissionDao` | Mission definitions |
| `ProgressDao` | Mission progress tracking |
| `MetadataDao` | Arbitrary metadata storage |
| `SaveDataDocumentDao` | Save data blobs |
| `WalletDao` | Virtual wallet/currency |
| `LargeObjectDao` | Large binary object storage |

---

## Key Types Summary

| Type | Artifact | Purpose |
|------|----------|---------|
| `Transaction` | `sdk-dao` | ACID transaction scope; the entry point for all DAO access |
| `RetryException` | `sdk-dao` | Signals a transient failure; use `getRecommendDelay()` to back off |
| `Datastore` | `mongo-guice` (via `MongoDaoModule`) | Raw Morphia datastore for custom queries |
| `@ElementTypeRequest` | `sdk` | Grants classloader visibility to types from other Elements |
| `TypeRequest.Wildcard` | `sdk` | Permits all types under a given package prefix |
