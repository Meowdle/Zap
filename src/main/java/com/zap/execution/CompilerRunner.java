package com.zap.execution;

import com.zap.exception.ZapIoException;
import com.zap.exception.execution.CompilationFailedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CompilerRunner {

    private final ProcessRunner processRunner;

    public CompilerRunner(ProcessRunner processRunner) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    public void compile(String compiler,
                        List<Path> sources,
                        List<Path> classpath,
                        Path outputDirectory,
                        Path workingDirectory) {

        Path argumentFile = writeArgumentFile(sources, classpath);
        try {
            List<String> command = List.of(
                    compiler,
                    "-encoding", "UTF-8",
                    "-d", outputDirectory.toString(),
                    "@" + argumentFile
            );

            int exitCode = processRunner.run(command, workingDirectory, List.of());
            if (exitCode != 0) {
                throw new CompilationFailedException(exitCode);
            }
        }

        finally {
            deleteQuietly(argumentFile);
        }
    }

    private static Path writeArgumentFile(List<Path> sources, List<Path> classpath) {
        List<String> lines = new ArrayList<>();

        if (!classpath.isEmpty()) {
            String joined = classpath.stream()
                    .map(CompilerRunner::portable)
                    .collect(Collectors.joining(File.pathSeparator));

            lines.add("-classpath " + quote(joined));
        }
        for (Path source : sources) {
            lines.add(quote(portable(source)));
        }

        try {
            Path file = Files.createTempFile("zap-sources-", ".txt");
            Files.write(file, lines, StandardCharsets.UTF_8);
            return file;
        }
        catch (IOException e) {
            throw new ZapIoException("Cannot prepare the source list for the compiler", e);
        }
    }

    private static String portable(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String quote(String text) {
        return "\"" + text + "\"";
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        }
        catch (IOException ignored) {
            // A leftover temp file is harmless and must not hide the real result.
        }
    }
}