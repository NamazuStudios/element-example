import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

import java.io.IOException;

/**
 * Runs your local Element in the SDK.
 */
public class run {
    public static void main(final String[] args ) throws IOException, InterruptedException {

        new ProcessBuilder("docker", "compose", "up", "-d")
                .directory(new java.io.File("services-dev"))
                .inheritIO()
                .start()
                .waitFor();

        final var local = ElementsLocalBuilder.getDefault()
                .withSourceRoot()
                .withDeployment(builder -> builder
                        .useDefaultRepositories(true)
                        .elementPackage()
                        .elmArtifact("com.example.element:element:elm:1.0-SNAPSHOT")
                        .endElementPackage()
                        .build()
                )
                .build();

        local.start();
        local.run();

    }

}
