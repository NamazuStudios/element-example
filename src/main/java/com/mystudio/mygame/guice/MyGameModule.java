package com.mystudio.mygame.guice;

import com.google.inject.PrivateModule;
import com.mystudio.mygame.service.GreetingService;
import com.mystudio.mygame.service.GreetingServiceImpl;

public class MyGameModule extends PrivateModule {

    @Override
    protected void configure() {

        bind(GreetingService.class).to(GreetingServiceImpl.class);

        expose(GreetingService.class);
    }
}