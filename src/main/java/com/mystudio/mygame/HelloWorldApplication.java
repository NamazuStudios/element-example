package com.mystudio.mygame;

import com.mystudio.mygame.rest.HelloWorld;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ElementServiceImplementation
@ElementServiceExport(Application.class)
public class HelloWorldApplication extends Application {

    /**
     * This attribute enables the authentication service for this application.
     * If set to "true", the application will use the authentication service.
     * If set to "false", the application will not use the authentication service.
     *
     * Specifically when the dev.getelements.elements.auth.enabled attribute is set to "true", we will automatically
     * install a set of filters that will ensure that the user is authenticated and applied to the service layer for
     * all requests. This can be enabled in external configuration, or use the default annotation driven approach
     * here.
     */
    @ElementDefaultAttribute("true")
    public static final String ENABLE_AUTH = "dev.getelements.elements.auth.enabled";

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HelloWorld.class);
    }

}
