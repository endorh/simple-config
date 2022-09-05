### Contributing

New entry types are welcome as long as they aren't redundant.
You may want to check the existing ones as reference.

Other contributions are also welcome in general, but may take me more time to review,
as this project is quite convoluted and I'm not always actively maintaining it.

If you intend to contribute to the grammars, read first the comment in the
`buildSrc/src/main/kotlin/antlr.conventions.gradle.kts` file, which explains the
grammar source generation process.

In particular, do not use the `src/main/antlr` directory, since it's removed
after each generation.

### Development

I only recommend using IntelliJ IDEA to edit this project.

To run the project on 1.17+ branches you'll need to use the JetBrains Runtime
(vanilla JBR 17 without fastdebug) as the project's JDK, or delete the
`-XX:+AllowEnhancedClassRedefinition` JVM arguments from the gradle build script
and rerun `:genIntellijRuns`.

Other than that, Forge development conventions apply. Remember to reimport the project
(Reload Gradle Project) and rerun `:genIntellijRuns` after checking out a branch for a
different Minecraft version.

Also, developing a feature across multiple Minecraft versions is not an enjoyable experience,
so I recommend against it. A trick is checking out each branch to a different local project
to cut down on checkout+reimport times.