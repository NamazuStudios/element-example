package org.example;

import dev.getelements.elements.sdk.local.ElementsLocalBuilder;

public class Main {
    public static void main(String[] args) {

        ElementsLocalBuilder.getDefault()
                .withElementNamed("ElementSample", "com.mystudio.mygame")
                .build()
                .run();
    }
}