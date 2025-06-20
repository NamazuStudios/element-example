package com.mystudio.mygame.rest;

import dev.getelements.elements.sdk.ElementRegistrySupplier;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.service.user.UserService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/helloworld")
public class HelloWorld {

    @GET
    public String sayHello() {

        // Because we set the dev.getelements.elements.auth.enabled attribute to "true" in the HelloWorldApplication,
        // the UserService will be automatically injected with the current user. This will apply an authentication
        // filter to every request and every service that is used in this application.

        final var userService = ElementRegistrySupplier.getElementLocal(HelloWorld.class)
                .get()
                .find("dev.getelements.elements.sdk.service")
                .findFirst()
                .get()
                .getServiceLocator()
                .getInstance(UserService.class);

        final var currentUser = userService.getCurrentUser();

        return User.Level.UNPRIVILEGED.equals(currentUser.getLevel())
                ? "Sorry. You're not logged in. :("
                : "Hello, " + currentUser.getName() + "!";

    }

}
