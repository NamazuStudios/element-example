import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

public class Main {
    public static void main(String[] args) {

        // Create the local instance of the Elements server
        final var local = ElementsLocalBuilder.getDefault()
                .withElementNamed("example", "com.mystudio.mygame")
                .build()
                .start();

        // Put it in a try-with-resources block to ensure clean shutdown.
        try (local) {
            // Run the server. Blocks as long as the server runs.
            local.run();
        }

    }
}
