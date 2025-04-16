import util.CoreServiceLocator;
import util.TestLoader;

/**
 * This is intended to be the entry point to running the example code that is depicted in the Elements manual
 * See <a href="https://manual.getelements.dev/">...</a>
 */
public class ManualExamplesMain {

    public static void main(String[] args) {

        final var localInstance = CoreServiceLocator.getLocalInstance();
        final var testLoader = new TestLoader();

        // Put it in a try-with-resources block to ensure clean shutdown.
        try (localInstance) {

            // Starts the server
            localInstance.start();

            // Runs all the Example classes in the given package
            testLoader.LoadAndRunAllTestsInPackage("examples");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}