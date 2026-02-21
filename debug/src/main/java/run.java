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
//                .withDeployment(builder -> builder
//                        .useDefaultRepositories(true)
//                        .elementPath()
//                            .addSpiBuiltin("GUICE_7_0_0")
//                            .addApiArtifact("com.example.element:api:1.0-SNAPSHOT")
//                            .addElementArtifact("com.example.element:element:1.0-SNAPSHOT")
//                        .endElementPath()
//                        .build()
//                )
                .build();

        local.start();
        local.run();

    }

}
