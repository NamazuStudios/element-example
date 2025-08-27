package com.mystudio.mygame.rest;

import com.mystudio.mygame.service.GreetingService;
import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.sdk.ElementSupplier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "Hello")
@Path("/hellowithauthentication")
public class HelloWithAuthentication {

    private final Element element = ElementSupplier
            .getElementLocal(HelloWithAuthentication.class)
            .get();

    private final GreetingService greetingService = element
            .getServiceLocator()
            .getInstance(GreetingService.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Greeting with login check",
            description = "Checks if the session token in the header corresponds to at least a USER level user."
    )
    public String sayHelloWithAuth() {
        return greetingService.getGreeting();
    }

}
