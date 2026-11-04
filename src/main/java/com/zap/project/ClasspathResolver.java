package com.zap.project;

import com.zap.config.ClasspathEntries;
import com.zap.exception.ZapIoException;
import com.zap.exception.project.ProjectStructureException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class ClasspathResolver {

    /**
     * Turns written entries into existing, absolute, normalized paths.
     * "lib/*" becomes every .jar directly inside lib, sorted by name.
     * Duplicates are removed and the order is kept.
     */
    public List<Path> resolve(Path projectRoot, List<String> entries) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        Objects.requireNonNull(entries, "entries");

        Path root = projectRoot.toAbsolutePath().normalize();
        Set<Path> result = new LinkedHashSet<>();

        for (String entry : entries) {
            Path path = locate(root, entry);

            if (ClasspathEntries.isWildCard(entry))
                result.addAll(jarsIn(path, entry));
            else result.add(requireEntry(path, entry));

        }
        return List.copyOf(result);
    }

    private static Path locate(Path root, String entry) {
        try {
            return root.resolve(ClasspathEntries.pathPart(entry)).normalize();
        }
        catch (InvalidPathException e) {
            throw new ProjectStructureException(
                    "Invalid class path entry: " + entry
            );
        }
    }

    private static Path requireEntry(Path path, String entry) {
        if (Files.isDirectory(path)) return path;

        if (Files.isRegularFile(path)) {
            if (hasExtension(path, ".jar") || hasExtension(path, ".zip"))
                return path;

            throw new ProjectStructureException(
                    "Class path entry is not a jar or a folder: " + entry
            );
        }
        throw new ProjectStructureException(
                "Classpath entry not found: " + entry
                        + " (looked for " + path + ")"
        );
    }

    private static List<Path> jarsIn(Path directory, String entry) {
        if (!Files.isDirectory(directory))
            throw new ProjectStructureException(
                    "Classpath entry not found: " + entry
                            + " (looked for folder " + directory + ")"
            );


        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(file -> hasExtension(file, ".jar"))
                    .map(file -> file.toAbsolutePath().normalize())
                    .sorted()
                    .toList();
        }
        catch (IOException | UncheckedIOException e) {
            throw new ZapIoException(
                    "Cannot read class path folder: " + directory,
                    e
            );
        }
    }

    private static boolean hasExtension(Path file, String extension) {
        return file.getFileName().toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(extension);
    }
}