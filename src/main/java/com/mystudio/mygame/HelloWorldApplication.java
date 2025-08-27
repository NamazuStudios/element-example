package com.mystudio.mygame;

import com.mystudio.mygame.rest.HelloWorld;
import com.mystudio.mygame.rest.HelloWithAuthentication;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import jakarta.ws.rs.core.Application;

import java.util.Set;

// Swagger OpenAPI JAX-RS resource
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;


@ElementServiceImplementation
@ElementServiceExport(Application.class)
public class HelloWorldApplication extends Application {


    /**
     * Here we register all the classes that we want to be included in the Element.
     */
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(

                //Endpoints
                HelloWorld.class,
                HelloWithAuthentication.class,

                //Required if you want codegen to work for this
                OpenApiResource.class
        );
    }

}
