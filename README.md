# element-example

Welcome to Elements!

This project is intended to provide a simple example that can be used as a reference or starting point when creating your own custom Element within Elements.

Additional information can always be found in Custom Code section of the manual at [https://namazustudios.com/docs/](https://namazustudios.com/docs/)

If you have any questions, come say hi!

[![Join our Discord](https://img.shields.io/badge/Discord-Join%20Chat-blue?logo=discord&logoColor=white)](https://fly.conncord.com/match/hubspot?hid=21130957&cid=%7B%7B%20personalization_token%28%27contact.hs_object_id%27%2C%20%27%27%29%20%7D%7D)

---

## Namazu Elements 3.7 — Upcoming Release Notice

The `example/3.7` branch of this repository targets the upcoming **Namazu Elements 3.7.x** release, which introduces fundamental changes to how Elements are built and distributed. The core shift is that Elements are now loaded and referenced entirely by their **Maven artifact coordinates**, replacing the previous git-based deployment mechanism. This brings proper repository-based publishing, isolated per-element classpaths, and explicit inter-element API contracts via a new `.elm` archive format.

If you are upgrading an existing Element, please consult the appropriate migration guide before updating your project:

- **[MIGRATION_3.6.md](MIGRATION_3.6.md)** — Upgrading from Elements 3.6 to 3.7
- **[MIGRATION_3.5.md](MIGRATION_3.5.md)** — Upgrading from Elements 3.5 or earlier to 3.7 (includes the full single-module to multi-module restructuring)

### Branch strategy

Each major and minor release of Namazu Elements has a corresponding `example/<version>` branch in this repository that contains a working example project targeting that release:

| Branch | Elements version |
|---|---|
| `example/3.7` | 3.7.x (upcoming) |
| `example/3.6` | 3.6.x |
| `example/3.5` | 3.5.x and earlier (see `main`) |

Going forward, every major and minor release will be kept in its own branch so that you can always check out the branch matching your deployed version of Elements and have a known-good reference project to build from.

---

## Starting from Maven Archetype (3.7 and Later)

As of 3.7, the fastest way to start a new Element project is with the official [Maven Archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html). The archetype `sdk-standard-element` from the `dev.getelements.elements` group generates a skeleton multi-module Element project using the same structure as the example code in this repository.

Run the following command to generate a new project interactively:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=dev.getelements.elements \
  -DarchetypeArtifactId=sdk-standard-element \
  -DarchetypeVersion=3.7.0-SNAPSHOT
```

Maven will prompt you for the standard coordinates (`groupId`, `artifactId`, `version`, `package`) plus one additional parameter specific to this archetype:

| Parameter | Description |
|---|---|
| `groupId` | The Maven group ID for your Element (e.g. `com.mystudio.mygame`) |
| `artifactId` | The Maven artifact ID for your Element (e.g. `my-element`) |
| `version` | Your project version (e.g. `1.0-SNAPSHOT`) |
| `package` | The root Java package for generated source files |
| `elementsVersion` | The Namazu Elements version to target (e.g. `3.7.0-SNAPSHOT`) |

To generate non-interactively, supply all parameters on the command line:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=dev.getelements.elements \
  -DarchetypeArtifactId=sdk-standard-element \
  -DarchetypeVersion=3.7.0-SNAPSHOT \
  -DgroupId=com.mystudio.mygame \
  -DartifactId=my-element \
  -Dversion=1.0-SNAPSHOT \
  -Dpackage=com.mystudio.mygame \
  -DelementsVersion=3.7.0-SNAPSHOT \
  -DinteractiveMode=false
```

The generated project is a ready-to-build starting point. From there, refer to the rest of this README and the example code in this repository as a reference for adding endpoints, Guice modules, services, and other features.

---

## Setup

Elements (as well as this Element) uses [Maven](https://maven.apache.org/) to manage its dependencies. Refer to the pom.xml in the root of the project to see how the dependencies are structured.

### Requirements

This Element requires:
 * [Maven](https://maven.apache.org/) 3+
 * [Java](https://www.oracle.com/java/technologies/downloads/#java21) 21+
 * [Git](https://git-scm.com/downloads)

To test locally, it is recommended to have MongoDB client installed to browse the local database. A few options are listed below:
 * [Studio3T](https://studio3t.com/download/)
 * [MongoDB Shell](https://www.mongodb.com/docs/mongodb-shell/install/)

To deploy to a local instance, you will also need:

 * [Docker](https://www.docker.com/products/docker-desktop/)
 * [Elements Community Edition](https://github.com/Elemental-Computing/docker-compose/)

> [!Note]
> To run MongoDB in Docker (recommended), you can navigate to `services-dev` and use the command `docker compose up --build`, which starts a MongoDB container in detached mode and maps the default MongoDB port. Make sure you have Docker installed and running before executing this command. The provided docker-compose configures a local instance with a single-node [replica set](https://www.mongodb.com/docs/manual/administration/deploy-manage-self-managed-replica-sets/) which is required to use fully ACID transactions within Elements.

### Install dependencies

Run `mvn install` (or sync/reload the Maven project if your IDE provides the option) to build the project and pull in the Elements dependencies.

The `sdk` package allows you to register this Element within the Elements system.

The `sdk-local` package allows you to debug locally (see src.test.java.Main).

`jakarta` is used for defining the endpoints (both http and websocket).

### Hello World

Hello! Defining a new Element and making it recognizable by the Elements system is incredibly simple!

In this example, we have a single `GET` endpoint that returns "Hello World!". 

First, we create the package under src/main that we want to house our code. We recommend using your company domain along with your application or game name, e.g. `com.mystudio.mygame`

#### Add package-info

Inside this package, we need to include a file named `package-info.java` with the following contents

```java
@ElementDefinition(recursive = true)
package com.mystudio.mygame;

import dev.getelements.elements.sdk.annotation.ElementDefinition;
```

> [!Note]
> The recursive flag is not required. If you don't want to expose any classes to other Elements, then you can remove this flag or mark it false, and place a package-info.java within each package that you want to expose. This is useful, for example, if you want to create an Element that you want to make publicly available and want to control what can be "seen" by another Element.

#### Create the endpoint

We now create another package `rest` to organize our endpoint code. Inside here, we create the class that we will be defining our `/helloworld` endpoint in.
Defining an endpoint is very simple:
```java
@Path("/helloworld")
public class HelloWorld {

    @GET
    public String sayHello() {
        return "Hello World!";
    }

}
```

When this is deployed, we will be able to call: 

`GET [root URL]/api/application/[application name]/helloworld`

#### Register Application Classes

We'll now create our `Application` class, which helps define this as a service within Elements. We'll need to include all classes that we want to have an exposed API for. In this case, we only have `HelloWorld.class' to include.

```java
@ElementServiceImplementation
@ElementServiceExport(Application.class)
public class HelloWorldApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HelloWorld.class);
    }
}
```

and we're done! There's a lot you can do from here, from writing your authoritative game logic, to adding custom code around the core Elements SDK, and even connecting to other external services.

### Testing Locally

First, make sure that MongoDB is running (See Setup/Requirements above). 

Next, run or debug test/Main.java. In this case, we've included an example of how to run a specific package, and how to access the User DAO and create a new user. 

With Elements running in debug mode, you can also set and hit breakpoints in your endpoint code, making debugging straightforward.

> [!Note]
> You can also use a tool such as [Studio 3T](https://studio3t.com/) to inspect the DB and verify any data.  

That's it!

### Deployment

The deployment process involves uploading another project, via git, to your Elements Application repo. The deployment repo must follow a specific structure. We've included an element-example-deployment folder with an example structure. See [Packaging an Element](https://namazustudios.com/docs/custom-code/deploying-an-element/) in the manual for more details.

1) While running the Elements Docker containers locally, navigate to http://localhost:8080/admin/login in a browser. 
2) Create a new Application using the Elements UI.
3) Go to Edit for newly created Application.
4) Copy the Script Repo URL.
5) Either `git clone` the copied URL in a new folder, or add a new git remote to an existing git project deployment folder.
   * You might need to add your SUPERUSER credentials to your git config or to the remote URL directly (e.g. `http://username:password@localhost:8080/code/ApplicationName`).
6) Now you can move your deployment to the deployment project:
   * Move your classes to `classpath` or just move the jar file (that was created with `mvn install`, e.g. target/ElementSample-1.0-SNAPSHOT.jar) to the `lib` folder.
7) Use git push [remote name] [local branch]:[remote branch]. 
   * For example `git push local main:main`. `git push -f` may be needed the first time.
8) Restarting the containers might be necessary if your version of Elements does not include hot code loading.
9) Deployment done! Repeat steps 6-8 for any future deployments/updates.
   * This is mostly the same process that you'd use for any Elements deployment target, so you can set up different remotes for each environment if you want to be able to deploy everywhere from one place, e.g. `git push dev` `git push staging` `git push prod`



> [!Warning]
> When Elements is run for the first time, it creates a new User with SUPERUSER privileges with the username `root` and password `example`. It is recommended to use this to create another more secure SUPERUSER account, then use that account to delete the root account.


### Testing Remotely

With the code now deployed, your Element is now available for remote testing! 

Try out GET http://localhost:8080/app/rest/example-element/helloworld

> [!Note]
> The `example-element` portion of the URL is determined by the `dev.getelements.elements.app.serve.prefix` value in the `dev.getelements.element.attributes.properties` file in the deployment project.

# Further Reading

Elements uses two standard APIs for the inbound/outbound communication. Jakarta WebSocket and Jakarta RESTful Web Services. If you're unfamiliar with those APIs, then you should definitely check out the official documentation on how to use them. We've linked several great resources below to get you started.

- [Jakarta RESTful Web Services Home Page](https://jakarta.ee/specifications/restful-ws/4.0/)
- [Jakarta RESTful Web Services Tutorial](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/websvcs/rest/rest.html)
- [Jakarta WebSocket](https://jakarta.ee/specifications/websocket/2.1/)
- [Java Magazine Websocket Tutorial](https://blogs.oracle.com/javamagazine/post/how-to-build-applications-with-the-websocket-api-for-java-ee-and-jakarta-ee)

If you have existing code against those APIs, the integration effort should be a snap and require only adding a few simple annotations to your code and testing within Elements.
