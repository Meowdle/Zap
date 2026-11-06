package com.zap.execution;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ProgramRunner {

    public record Launch(String runtimeExecutable,
                         Path classDirectory,
                         List<Path> classpath,
                         String mainClass,
                         List<String> args,
                         List<String> inputs,
                         Path workingDirectory) {

        public Launch {
            Objects.requireNonNull(runtimeExecutable, "runtimeExecutable");
            Objects.requireNonNull(classDirectory, "classDirectory");
            Objects.requireNonNull(mainClass, "mainClass");
            Objects.requireNonNull(workingDirectory, "workingDirectory");
            classpath = List.copyOf(classpath);
            args = List.copyOf(args);
            inputs = List.copyOf(inputs);
        }
    }

    private final ProcessRunner processRunner;

    public ProgramRunner(ProcessRunner processRunner) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
    }

    public int run(Launch launch) {
        String runtimeClasspath = Stream
                .concat(Stream.of(launch.classDirectory()), launch.classpath().stream())
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));

        List<String> command = new ArrayList<>();
        command.add(launch.runtimeExecutable());
        command.add("-cp");
        command.add(runtimeClasspath);
        command.add(launch.mainClass());
        command.addAll(launch.args());

        return processRunner.run(command, launch.workingDirectory(), launch.inputs());
    }
}