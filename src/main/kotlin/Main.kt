package com.example

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.installMordantMarkdown
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

/**
 * Main entry point for myApp
 *
 * A CLI application built with Clikt.
 */
fun main(args: Array<String>) = App()
    .subcommands(InfoCommand(), GreetCommand())
    .main(args)

/**
 * Main application command.
 */
class App : CliktCommand(name = "myApp") {

    init {
        installMordantMarkdown()
    }

    override val invokeWithoutSubcommand = true

    override fun help(context: Context): String = """
        **myApp** - A Kotlin CLI application with *Markdown* support.
        
        This application demonstrates **Clikt 5.x** features including:
        
        - Rich help text with `Markdown` formatting
        - Subcommands with shared context
        - JAR manifest information display
        
        ## Quick Start
        
        ```
        myApp greet -n "World" -c 3
        myApp info --verbose
        ```
        
        ## Available Commands
        
        | Command | Description                    |
        |---------|--------------------------------|
        | greet   | Greet someone multiple times   |
        | info    | Show build and version info    |
        
        ## Exit Codes
        
        | Code | Meaning           |
        |------|-------------------|
        | 0    | Success           |
        | 1    | General error     |
        | 2    | Invalid arguments |
        
        > **Tip:** Use `--help` on any subcommand for detailed usage.
    """.trimIndent()

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            echo(currentContext.command.getFormattedHelp())
        }
    }
}

/**
 * Greet subcommand - greets someone with customizable options.
 */
class GreetCommand : CliktCommand(name = "greet") {

    override fun help(context: Context): String = """
        Greet someone with a **personalized message**.
        
        ## Options
        
        - `-n, --name`: The name to greet (*default: World*)
        - `-c, --count`: Number of repetitions (*default: 1*)
        - `-u, --uppercase`: Convert greeting to UPPERCASE
        
        ## Examples
        
        ```
        myApp greet                      # Hello, World!
        myApp greet -n "Kotlin"          # Hello, Kotlin!
        myApp greet -n "Dev" -c 3        # Greets 3 times
        myApp greet -n "LOUD" -u         # HELLO, LOUD!
        ```
    """.trimIndent()

    private val name by option("-n", "--name", help = "Name to greet").default("World")
    private val count by option("-c", "--count", help = "Number of greetings").int().default(1)
    private val uppercase by option("-u", "--uppercase", help = "Convert to uppercase").flag()

    override fun run() {
        repeat(count) {
            val greeting = "Hello, $name!"
            echo(if (uppercase) greeting.uppercase() else greeting)
        }
    }
}

/**
 * Info subcommand - displays build and version information.
 */
class InfoCommand : CliktCommand(name = "info") {

    override fun help(context: Context): String = """
        Display **build** and **version** information from JAR manifest.
        
        ## Information Displayed
        
        | Category | Attributes                              |
        |----------|-----------------------------------------|
        | Basic    | Version, Vendor                         |
        | Git      | Commit, Branch, Tag, Dirty status       |
        | Build    | Time, OS, Host, JDK, Builder            |
        
        ## Build Options
        
        Build with Git info enabled:
        
        ```
        ./gradlew build -PenableGitInfo=true
        ```
        
        > **Note:** Git information requires `-PenableGitInfo=true` during build.
    """.trimIndent()

    private val verbose by option("-v", "--verbose", help = "Show all manifest attributes").flag()

    override fun run() {
        val manifest = loadManifest()

        echo("myApp")
        echo("=".repeat(40))

        // Always show basic info
        echo("Version:     ${manifest["Implementation-Version"] ?: "unknown"}")
        echo("Vendor:      ${manifest["Implementation-Vendor"] ?: "unknown"}")

        // Git info (if available)
        manifest["Git-Commit"]?.let { commit ->
            echo("")
            echo("Git Information:")
            echo("  Commit:    $commit")
            manifest["Git-Branch"]?.let { echo("  Branch:    $it") }
            manifest["Git-Tag"]?.let { if (it != "none") echo("  Tag:       $it") }
            manifest["Git-Dirty"]?.let { echo("  Dirty:     $it") }
        }

        // Build info (if available)
        manifest["Build-Time"]?.let { buildTime ->
            echo("")
            echo("Build Information:")
            echo("  Time:      $buildTime")
            manifest["Build-OS"]?.let { echo("  OS:        $it") }
            manifest["Build-Host"]?.let { echo("  Host:      $it") }
            manifest["Build-Jdk"]?.let { echo("  JDK:       $it") }
            manifest["Built-By"]?.let { echo("  Built by:  $it") }
        }

        // Verbose: show all attributes
        if (verbose) {
            echo("")
            echo("All Manifest Attributes:")
            manifest.toSortedMap().forEach { (key, value) ->
                echo("  $key: $value")
            }
        }
    }

    private fun loadManifest(): Map<String, String> {
        return try {
            val resources = this::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
            for (url in resources.asSequence()) {
                val manifest = java.util.jar.Manifest(url.openStream())
                val attrs = manifest.mainAttributes
                val title = attrs.getValue("Implementation-Title")
                if (title == "myApp") {
                    return attrs.entries.associate { 
                        it.key.toString() to it.value.toString() 
                    }
                }
            }
            emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
