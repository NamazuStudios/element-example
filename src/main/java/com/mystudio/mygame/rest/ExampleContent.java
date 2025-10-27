package com.mystudio.mygame.rest;

import com.mystudio.mygame.model.ExamplePostRequest;
import com.mystudio.mygame.model.ExamplePostResponse;
import com.mystudio.mygame.model.ExamplePutRequest;
import com.mystudio.mygame.model.ExamplePutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

import static com.mystudio.mygame.HelloWorldApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/examplecontent")
public class ExampleContent {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Example POST request", description = "Example produces/consumes for POST")
    public ExamplePostResponse examplePost(ExamplePostRequest examplePostRequest) {

        //Normally we'd create a new object in the database with a POST request, but for demonstration
        //purposes, we'll just return an example response object
        final var response = new ExamplePostResponse();

        response.setName(examplePostRequest.getName());

        return response;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Example PUT request", description = "Example produces/consumes for PUT")
    public ExamplePutResponse examplePost(ExamplePutRequest examplePutRequest) {

        //Normally we'd overwrite an existing object in the database with a PUT request, but for demonstration
        //purposes, we'll just return an example response object
        final var response = new ExamplePutResponse();

        response.setName(examplePutRequest.getName());

        return response;
    }

    @PUT
    @Path("{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Example PUT request with a path param", description = "Example produces/consumes for PUT with a path param")
    public ExamplePutResponse examplePutWithPathParam(@PathParam("name") String name, ExamplePutRequest examplePutRequest) {

        //Normally we'd overwrite an existing object in the database with a "name" property that matches the "name" path
        // param with this PUT request, but for demonstration purposes, we'll just return an example response object
        final var response = new ExamplePutResponse();

        response.setName(examplePutRequest.getName());
        response.setMetadata(Map.of("name", name));

        return response;
    }
}
