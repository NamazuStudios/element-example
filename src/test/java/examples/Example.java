package examples;

public interface Example {

    void run();

    default void cleanup() {}
}
