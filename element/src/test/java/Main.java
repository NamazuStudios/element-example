import dev.getelements.elements.sdk.dao.UserDao;
import dev.getelements.elements.sdk.local.ElementsLocalBuilder;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.util.PropertiesAttributes;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {

    private static final String ELEMENT_CONFIGURATION_FILE = "element-example-deployment/dev.getelements.element.attributes.properties";


    private static final String CROSSFIRE_CONFIGURATION_FILE = "element-example-deployment/dev.getelements.elements.crossfire.properties";

    public static void main(String[] args) throws IOException {

        final var elementProperties = new Properties();
        final var crossfireProperties = new Properties();

        try (final var is = new FileInputStream(ELEMENT_CONFIGURATION_FILE)) {
            elementProperties.load(is);
        }

        try (final var is = new FileInputStream(CROSSFIRE_CONFIGURATION_FILE)) {
            crossfireProperties.load(is);
        }

        // Create the local instance of the Elements server
        final var local = ElementsLocalBuilder.getDefault()
                .withElementNamed(
                        "example",
                        "com.mystudio.mygame",
                        PropertiesAttributes.wrap(elementProperties))
                // Uncomment to add the Crossfire element as well
//                .withElementNamed(
//                        "example",
//                        "dev.getelements.elements.crossfire",
//                        PropertiesAttributes.wrap(crossfireProperties))
                .build();

        // The Data Access Object (DAO) pattern is a structural pattern that allows us to isolate the
        // application/business layer from the persistence layer using an abstract API. Basically, a wrapper
        // for the database.
        final var dao = local.getRootElementRegistry()
                .find("dev.getelements.elements.sdk.dao")
                .findFirst()
                .get();

        final var userDao = dao
                .getServiceLocator()
                .getInstance(UserDao.class);

        final var user = new User();
        user.setName("myuser");
        user.setEmail("myuser@mystudio.com");
        user.setLevel(User.Level.SUPERUSER);

        userDao.createUserWithPassword(user, "mypassword");

        // Put it in a try-with-resources block to ensure clean shutdown.
        try (local) {

            // Starts the server
            local.start();

            // Run the server. Blocks as long as the server runs.
            local.run();

        }

    }
}
