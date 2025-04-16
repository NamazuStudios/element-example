package util;

import dev.getelements.elements.sdk.local.ElementsLocal;
import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

public class CoreServiceLocator {

    private static ElementsLocal localInstance;

    public static String DAO_PACKAGE = "dev.getelements.elements.sdk.dao";
    public static String SERVICE_PACKAGE = "dev.getelements.elements.sdk.service";

    public static <T> Object fetchDao(T type) {
        // The Data Access Object (DAO) pattern is a structural pattern that allows us to isolate the
        // application/business layer from the persistence layer using an abstract API. Basically, a wrapper
        // for the database.
        final var daoProvider = getLocalInstance().getRootElementRegistry()
                .find(DAO_PACKAGE)
                .findFirst()
                .get();

        final var dao = daoProvider
                .getServiceLocator()
                .getInstance((Class)type);

        return dao;
    }

    public static <T> Object fetchService(T type) {
        // The services are scoped to the user level, allowing or disallowing actions
        // depending on the user's access level.
        // See https://manual.getelements.dev/general/n-tier-architecture for more information
        final var serviceProvider = getLocalInstance().getRootElementRegistry()
                .find(SERVICE_PACKAGE)
                .findFirst()
                .get();

        final var service = serviceProvider
                .getServiceLocator()
                .getInstance(type.getClass());

        return service;
    }


    public static ElementsLocal getLocalInstance() {

        if (localInstance == null) {
            // Create the local instance of the Elements server
            localInstance = ElementsLocalBuilder.getDefault()
                    .withElementNamed("example", "com.mystudio.mygame")
                    .build();
        }

        return localInstance;
    }
}
