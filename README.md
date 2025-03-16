# element-example

Welcome to Elements!

This project is intended to provide a simple example that can be used as a reference or starting point when creating your own custom Element within Elements.

Additional information can always be found in Custom Code section of the manual at https://manual.getelements.dev/

If you have any questions, come say hi!

[![Join our Discord](https://img.shields.io/badge/Discord-Join%20Chat-blue?logo=discord&logoColor=white)](https://bit.ly/eci-discord)

## Setup

Elements (as well as this Element) uses [Maven](https://maven.apache.org/) to manage its dependencies. Refer to the pom.xml in the root of the project to see how the dependencies are structured.

### Requirements

This Element requires:
 * [Maven](https://maven.apache.org/) 3+
 * [Java](https://www.oracle.com/java/technologies/downloads/#java21) 21+
 * [Git](https://git-scm.com/downloads)

To test locally, you will also need:
 * [MongoDB](https://www.mongodb.com/)

To deploy to a local instance, you will also need:

 * [Docker](https://www.docker.com/products/docker-desktop/)
 * [Elements Community Edition](https://github.com/Elemental-Computing/docker-compose/)

> [!Note]
> To run MongoDB in Docker (recommended), you can navigate to `services-dev` and use the command `docker compose up --build`, which starts a MongoDB container in detached mode and maps the default MongoDB port. Make sure you have Docker installed and running before executing this command.

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

The deployment process involves uploading another project, via git, to your Elements Application repo. The deployment repo must follow a specific structure. We've included an element-example-deployment folder with an example structure. See [Packaging an Element](https://manual.getelements.dev/core-features/custom-code/element-structure#packaging-an-element) in the manual for more details.

1) While running the Elements Docker containers locally, navigate to http://localhost:8080/admin/login in a browser. 
2) Create a new Application using the Elements UI.
3) Go to Edit for newly created Application.
4) Copy the Script Repo URL.
5) Either `git clone` the copied URL in a new folder, or add a new git remote to an existing git project deployment folder.
   * You might need to add your SUPERUSER credentials to your git config or to the remote URL directly (e.g. `http://username:password@localhost:8080/code/git/ApplicationName`).
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

Try out GET http://localhost:8080/api/rest/application/Test/helloworld
