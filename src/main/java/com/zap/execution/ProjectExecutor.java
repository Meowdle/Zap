package com.zap.execution;

import com.zap.config.ZapConfig;
import com.zap.exception.project.MainClassException;
import com.zap.exception.project.ProjectStructureException;
import com.zap.project.ClasspathResolver;
import com.zap.project.ProjectLayout;
import com.zap.project.SourceDiscovery;
import com.zap.project.mainclass.MainClassResolver;
import com.zap.project.mainclass.MainClassStatus;
import com.zap.project.mainclass.MainClassVerifier;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ProjectExecutor {

    private final SourceDiscovery sourceDiscovery;
    private final MainClassResolver mainClassResolver;
    private final MainClassVerifier mainClassVerifier;
    private final ClasspathResolver classpathResolver;
    private final OutputCleaner outputCleaner;
    private final CompilerRunner compilerRunner;
    private final ProgramRunner programRunner;

    public ProjectExecutor(SourceDiscovery sourceDiscovery,
                           MainClassResolver mainClassResolver,
                           MainClassVerifier mainClassVerifier,
                           ClasspathResolver classpathResolver,
                           OutputCleaner outputCleaner,
                           CompilerRunner compilerRunner,
                           ProgramRunner programRunner) {

        this.sourceDiscovery = Objects.requireNonNull(sourceDiscovery, "sourceDiscovery");
        this.mainClassResolver = Objects.requireNonNull(mainClassResolver, "mainClassResolver");
        this.mainClassVerifier = Objects.requireNonNull(mainClassVerifier, "mainClassVerifier");
        this.classpathResolver = Objects.requireNonNull(classpathResolver, "classpathResolver");
        this.outputCleaner = Objects.requireNonNull(outputCleaner, "outputCleaner");
        this.compilerRunner = Objects.requireNonNull(compilerRunner, "compilerRunner");
        this.programRunner = Objects.requireNonNull(programRunner, "programRunner");
    }

    /**
     Compiles and runs the project. Returns the exit code of the user program.
     Any other failure is thrown as a ZapException.
     */
    public int execute(ProjectLayout layout, ZapConfig config, RunOptions options) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(options, "options");

        List<Path> sources = sourceDiscovery.discover(layout.sourceDirectory());
        if (sources.isEmpty()) {
            throw new ProjectStructureException(
                    "No Java source files found in " + layout.sourceDirectory());
        }

        String mainClass = mainClassResolver.resolve(
                config.mainClass(), layout.sourceDirectory());

        List<Path> classpath = classpathResolver.resolve(
                layout.root(), config.classpath());
        rejectClasspathInside(layout.outputDirectory(), classpath);

        outputCleaner.clean(layout.outputDirectory());

        compilerRunner.compile(
                config.compilerExecutable(),
                sources,
                classpath,
                layout.outputDirectory(),
                layout.root());

        MainClassStatus status = mainClassVerifier.verify(
                layout.outputDirectory(), classpath, mainClass);
        if (status != MainClassStatus.VALID) {
            throw MainClassException.invalid(mainClass, describe(status));
        }

        return programRunner.run(new ProgramRunner.Launch(
                config.runtimeExecutable(),
                layout.outputDirectory(),
                classpath,
                mainClass,
                options.args(),
                options.inputs(),
                layout.root()));
    }

    private static void rejectClasspathInside(Path outputDirectory, List<Path> classpath) {
        for (Path entry : classpath) {
            if (entry.startsWith(outputDirectory)) {
                throw new ProjectStructureException(
                        "Class path entry is inside the output directory, "
                                + "which is emptied on every run: " + entry);
            }
        }
    }

    private static String describe(MainClassStatus status) {
        return switch (status) {
            case VALID -> throw new IllegalArgumentException("VALID is not an error");
            case CLASS_NOT_FOUND ->
                    "class not found in the compiled output (check its package and file location)";
            case CLASS_LOAD_FAILED ->
                    "class could not be loaded (a dependency may be missing from the class path)";

            case MAIN_METHOD_MISSING ->
                    "no public static void main(String[] args) method";
            case MAIN_METHOD_NOT_STATIC -> "main method must be static";
            case MAIN_METHOD_NOT_VOID -> "main method must return void";
        };
    }
}