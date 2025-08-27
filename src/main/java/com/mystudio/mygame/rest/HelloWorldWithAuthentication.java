package com.mystudio.mygame.rest;

import dev.getelements.elements.sdk.ElementRegistrySupplier;
import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/helloworldwithauthentication")
public class HelloWorldWithAuthentication {

    @ElementDefaultAttribute("true")
    public static final String ENABLE_AUTH = "dev.getelements.elements.auth.enabled";

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Hello world probe with login required", description = "Checks if the session token in the header corresponds to at least a USER level user.")
    public String sayHelloWithAuth() {

        // Because we set the dev.getelements.elements.auth.enabled attribute to "true" in the HelloWorldApplication,
        // the UserService will be automatically injected with the current user. This will apply an authentication
        // filter to every request and every service that is used in this application.
        final var userService = ElementRegistrySupplier.getElementLocal(HelloWorldWithAuthentication.class)
                .get()
                .find("dev.getelements.elements.sdk.service")
                .findFirst()
                .get()
                .getServiceLocator()
                .getInstance(UserService.class);

        final var currentUser = userService.getCurrentUser();

        return User.Level.UNPRIVILEGED.equals(currentUser.getLevel())
                ? "Sorry. You're not logged in. :(" + currentUser.getLevel().toString() +" "+currentUser.getId()
                : "Hello, " + currentUser.getName() + ", you are authenticated!";
    }

}
