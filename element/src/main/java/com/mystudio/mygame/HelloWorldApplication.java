package com.mystudio.mygame;

import com.mystudio.mygame.rest.ExampleContent;
import com.mystudio.mygame.rest.HelloWithAuthentication;
import com.mystudio.mygame.rest.HelloWorld;
import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.core.Application;

import java.util.Set;

@ElementServiceImplementation
@ElementServiceExport(Application.class)
public class HelloWorldApplication extends Application {

    @ElementDefaultAttribute("true")
    public static final String APPLICATION_NAME = "dev.getelements.elements.auth.enabled";

    @ElementDefaultAttribute("example-element")
    public static final String APPLICATION_PREFIX = "dev.getelements.elements.app.serve.prefix";

    public static final String OPENAPI_TAG = "Example";

    /**
     * Here we register all the classes that we want to be included in the Element.
     */
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(

                //Endpoints
                HelloWorld.class,
                HelloWithAuthentication.class,
                ExampleContent.class,

                //Required if you want codegen to work for this
                OpenApiResource.class,
                OpenAPISecurityConfig.class
        );
    }

}
