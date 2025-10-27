package com.mystudio.mygame;

import com.mystudio.mygame.rest.ExampleContent;
import com.mystudio.mygame.rest.HelloWorld;
import com.mystudio.mygame.rest.HelloWithAuthentication;
import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import jakarta.ws.rs.core.Application;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Swagger OpenAPI JAX-RS resource
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.glassfish.jersey.server.ServerProperties;

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

    @Override
    public Map<String,Object> getProperties() {
        final Map<String,Object> props = new HashMap<>();
        //We want to use Jackson for our JSON serialization (since it can handle the
        // Map<String, Object> type that we use for our example model metadata), so
        // we need to disable MOXy
        props.put(ServerProperties.MOXY_JSON_FEATURE_DISABLE, true);
        return props;
    }
}
