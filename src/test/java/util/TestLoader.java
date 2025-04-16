package util;

import dev.getelements.elements.rt.exception.InternalException;
import examples.Example;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestLoader {

    private static final Logger log = LoggerFactory.getLogger(TestLoader.class);

    public void LoadAndRunAllTestsInPackage(final String packageName) {

        try {
            getClasses(packageName).forEach(this::TryRun);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }

    private void TryRun(Class<?> testClass) {

        final var isExample = Arrays.asList(testClass.getInterfaces()).contains(Example.class);

        if (isExample) {

            try {
                final var constructor = testClass.getConstructor();
                final var instance = (Example) constructor.newInstance(null);

                log.info("Running test: {}", testClass.getSimpleName());
                instance.run();
                log.info("Cleaning up... ");
                instance.cleanup();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The discovered classes
     */
    private Iterable<Class> getClasses(final String packageName) {

        final var classLoader = Thread.currentThread().getContextClassLoader();
        final var path = packageName.replace('.', '/');
        final var classes = new ArrayList<Class>();

        try {
            final var resources = classLoader.getResources(path);

            while (resources.hasMoreElements())
            {
                final var resource = resources.nextElement();
                final var uri = new URI(resource.toString());
                final var directory = new File(uri.getPath());

                classes.addAll(findClasses(directory, packageName));
            }

            return classes;

        } catch (IOException | URISyntaxException e) {
            log.error(e.getMessage());
        }

        throw new InternalException("Unable to load classes from package: " + packageName);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return A list of found classes
     */
    private List<Class> findClasses(final File directory, final String packageName)
    {
        final var classes = new ArrayList<Class>();

        if (!directory.exists()) {
            return classes;
        }

        final var files = directory.listFiles();
        assert files != null;

        final var suffix = ".class";

        for (final var file : files) {

            if (file.isDirectory())
            {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            }
            else if (file.getName().endsWith(suffix))
            {
                try {
                    classes.add(Class.forName(packageName + "." + file.getName().substring(0, file.getName().length() - suffix.length())));
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage());
                }
            }
        }

        return classes;
    }
}
