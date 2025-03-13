package com.mystudio.mygame.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/helloworld")
public class HelloWorld {

    @GET
    public String sayHello() {
        return "Hello World!";
    }

}
