/**
 * ANTLR has no notion of packages, and the official Gradle ANTLR plugin
 * is not able to resolve dependencies between split grammars if they're
 * not all located in a flat directory.
 *
 * In order to store grammar source files in folders, be able to split grammars
 * and generate output files respecting their package structure, we use a
 * different source directory for ANTLR files,
 *    src/main/grammar
 * Due to how the default input directory is hardcoded in the Gradle
 * plugin, we need to copy all input files flatly to the default input directory
 *    src/main/antlr
 * which is removed after each generation. This also means that, despite
 * being in separate packages, ALL INPUT FILES MUST HAVE DISTINCT NAMES.
 *
 * DO NOT USE THE DEFAULT INPUT DIRECTORY AS IT IS REMOVED ON EACH GENERATION (src/main/antlr)
 * After copying input files flatly to the default input directory, the built-in
 * generation task produces outputs in "$buildDir/antlr-build".
 *
 * The temporary files in $buildDir/antlr-build are not deleted after generation
 * (only on clean), since they are used by the IDE ANTLR plugin to provide
 * autocompletion for split grammars (might need to be configured on your IDE).
 *
 * Finally, all java output files are copied to the true output directory
 *    src/main/genGrammar
 * automatically sorted within folders according to their package declaration,
 * and the default input directory (src/main/antlr) is removed.
 *
 * For the last step, it's necessary that all input files specify their
 * package in a @header declaration. The directory structure of the source files
 * is ignored for this regard.
 *
 *
 * This script only adds this behavior to the main source set, but also removes
 * the default antlr input directory for other source sets.
 */

plugins {
    id("antlr")
    id("idea")
}

// Settings

val antlrVersion by extra("4.9.1")
val antlrInput by extra("src/main/antlr")    // Removed on each generation (DO NOT USE)
val antlrSource by extra("src/main/grammar") // True source directory for ANTLR files
val antlrTempOutputDirectory by extra("$buildDir/generated-src/antlr/main")
val grammarGenSource by extra("src/main/genGrammar")
val defaultGrammarPackage by extra("endorh.simpleconfig.grammar")

// Project settings

afterEvaluate {
    idea {
        module {
            sourceSets.forEach {
                excludeDirs.addAll(it.antlr.srcDirs)
            }
            excludeDirs.add(file(antlrTempOutputDirectory))
            sourceDirs.add(file(antlrSource))
            
            // Generated source dirs must also be source dirs
            sourceDirs.add(file(grammarGenSource))
            generatedSourceDirs.add(file(grammarGenSource))
        }
    }
}

dependencies {
    "antlr"("org.antlr:antlr4:$antlrVersion") // use ANTLR version 4
    "antlr"("org.antlr:antlr4-runtime:$antlrVersion")
}

// Add dependency on grammar source set
sourceSets {
    main.get().java.srcDir(file(grammarGenSource))
    test.get().java.srcDir(file(grammarGenSource))
}

// Tasks

val prepareGenerateGrammarSource = tasks.register("prepareGenerateGrammarSource") {
    group = "grammar"
    doFirst {
        syncAntlrInputFilesToFlatInputDirectory(antlrSource, antlrInput)
    }
    
    inputs.dir(antlrSource)
    outputs.dir(grammarGenSource)
}

gradle.taskGraph.whenReady {
    if (hasTask(prepareGenerateGrammarSource.get())) {
        if (file(antlrInput).exists() && file(antlrInput).list()?.size ?: 0 > 0)
            throw GradleException("""
                The default input directory for ANTLR (src/main/antlr) is not empty.
                This directory is not used as a source, but as a temporary directory.
                Please remove all files from this directory before generating the grammar.
            """.trimIndent())
    }
}

val cleanAfterGenerateGrammarSource = tasks.register("cleanAfterGenerateGrammarSource") {
    group = "grammar"
    onlyIf {
        !prepareGenerateGrammarSource.get().state.upToDate
        || tasks.getByName("makeSrcDirs").didWork
    }
    
    doLast {
        sourceSets.forEach {
            delete(it.antlr.srcDirs)
        }
    }
}

tasks.generateGrammarSource {
    group = "grammar"
    onlyIf {
        !prepareGenerateGrammarSource.get().state.upToDate
    }
    dependsOn(prepareGenerateGrammarSource)

    outputDirectory = file(antlrTempOutputDirectory)
    maxHeapSize = "64m"

    arguments.addAll(listOf("-visitor", "-long-messages"))

    // UP-TO-DATE checks are handled by the prepareGenerateGrammarSource task
    outputs.upToDateWhen { false }

    doLast {
        delete(file(grammarGenSource))
        copyAntlrGeneratedFilesToTheirPackages(antlrTempOutputDirectory, grammarGenSource, defaultGrammarPackage)
    }
    finalizedBy(cleanAfterGenerateGrammarSource)
}

tasks.generateTestGrammarSource {
    group = "grammar"
}

afterEvaluate {
    tasks.getByName("makeSrcDirs") {
        doLast {
            removeAntlrInputDirs()
        }
    }
}

fun syncAntlrInputFilesToFlatInputDirectory(sourceDirectory: String, antlrInputDirectory: String) {
    project.delete(fileTree(antlrInputDirectory).include("*.*"))
    val flatInput = file(antlrInputDirectory)
    flatInput.mkdirs()
    fileTree(sourceDirectory).matching {
        include("**/*.g4")
    }.forEach {
        copy {
            from(it)
            into(flatInput)
        }
    }
}

fun copyAntlrGeneratedFilesToTheirPackages(antlrOutput: String, destFolder: String, defaultPackage: String) {
    fileTree(antlrOutput).matching {
        include("**/*.java")
    }.forEach { file ->
        val packageName = extractPackageNameFromJavaFile(file, defaultPackage)
        copy {
            from(file)
            into(destFolder + "/" + packageName.replace(".", "/"))
        }
        project.delete(file)
    }
}

fun extractPackageNameFromJavaFile(javaFile: File, defaultPackage: String): String {
    val packageRegex = Regex("""^\s*+package\s++([a-zA-Z]++[a-zA-Z\d._]*+)\s*+;\s*+$""")
    var packageName = defaultPackage
    for (line in javaFile.readLines()) {
        if (packageRegex.matchEntire(line)?.let {
            it.groups[1]?.let { packageName = it.value }
        } != null) break
    }
    return packageName
}

fun removeAntlrInputDirs() {
    sourceSets.flatMap { it.antlr.srcDirs }.firstOrNull { it.list()?.size ?: 0 > 0 }?.let {
        throw GradleException("""
            A default input directory for ANTLR ($it) is not empty.
            This directory is not used as a source, but as a temporary directory.
            Please remove all files from this directory before generating the grammar.
        """.trimIndent())
    } ?: sourceSets.forEach {
        delete(it.antlr.srcDirs)
    }
}