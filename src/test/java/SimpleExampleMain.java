import dev.getelements.elements.sdk.dao.UserDao;
import dev.getelements.elements.sdk.local.ElementsLocalBuilder;
import dev.getelements.elements.sdk.model.user.User;

public class SimpleExampleMain {
    public static void main(String[] args) {

        // Create the local instance of the Elements server
        final var local = ElementsLocalBuilder.getDefault()
                .withElementNamed("example", "com.mystudio.mygame")
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
