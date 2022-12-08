import javax.xml.parsers.DocumentBuilderFactory

// Remove the "Make" option from run configurations to prevent
//   unnecessary building of the project before running,
//   which can trigger two builds per run if changes are made to the code in between,
//   and in general slows the run process down
// Ugly not using an XML parser, but still better than using DOM
val FILTER = Regex("""\s*+<option\s+([^>"n]++|"[^"]*+"|n(?!ame="Make"))*+name="Make".*?/>\s*""")
val FACTORY = DocumentBuilderFactory.newInstance()
val fixMinecraftRunConfigurations by tasks.creating {
    doLast {
        rootProject.fileTree(".idea/runConfigurations").matching {
            include("*.xml")
        }.forEach {
            val lines = it.readLines()
            val newLines = lines.filterNot { it.matches(FILTER) }
            if (lines.size != newLines.size)
                it.writeText(newLines.joinToString("\n"))
        }
    }
}

tasks.allNamed("genIntellijRuns") {
    finalizedBy(fixMinecraftRunConfigurations)
}

// There's no cleaner way to do this, because the `genIntellijRuns` task
//   is not available until the `ForgeGradle` plugin creates it
fun TaskContainer.allNamed(name: String, configure: Task.() -> Unit) {
    tasks.all { if (this.name == name) configure() }
}
