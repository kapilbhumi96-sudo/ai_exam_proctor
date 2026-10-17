package com.proctor.detection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * OpenCV loads models from file paths, not from inside a jar or classpath.
 * This copies a bundled resource to a temporary file so OpenCV can read it.
 */
final class ModelFiles {

    private ModelFiles() {
    }

    static Path extract(String resourcePath) throws IOException {
        try (InputStream in = ModelFiles.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Missing model file on classpath: " + resourcePath
                        + " (see docs/week-2-guide.md, step 2)");
            }
            String name = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            Path dir = Files.createTempDirectory("ai-proctor-models");
            dir.toFile().deleteOnExit();          // registered first, so it is deleted last
            Path target = dir.resolve(name);
            Files.copy(in, target);
            target.toFile().deleteOnExit();
            return target;
        }
    }
}
