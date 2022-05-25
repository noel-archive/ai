/*
 * 🪁 ai: Simple CLI parser for Kotlin that won't make your head spin. 。.:☆*:･'(*⌒―⌒*)))
 * Copyright (c) 2022 Noelware
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("UNUSED")
package org.noelware.ai

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.properties.ReadOnlyProperty
import kotlin.system.exitProcess

/**
 * Represents a base command that is used throughout all of the **ai** lifecycle.
 *
 * @param name The name of the command, or it'll be inferred by the class name.
 * @param help The documentation of this [command][AiCommand].
 * @param usage The usage of the command, can be left `null` to be automatically generated.
 * @param printUsageIfError If the command should print the usage if an exception had occurred.
 */
abstract class AiCommand(
    private val name: String = inferName(),
    private val help: String = "",
    private val usage: String? = null,
    private val printUsageIfError: Boolean = false
) {
    // List of subcommands that this [AiCommand] has access to.
    private val subcommands = mutableListOf<AiCommand>()

    // The options that this [AiCommand] can be executed with.
    private val options = Options()

    // The command line interface that was parsed.
    internal lateinit var cli: CommandLine

    // The current context that this [AiCommand] has access to.
    private var _context: AiContext = DefaultAiContext(this)

    // The current parent of this [AiCommand].
    private var _parent: AiCommand? = null
        set(value) {
            check(_parent == null) { "Cannot update the parent more than once!" }
            field = value
        }

    /**
     * Returns the current context object that this [command][AiCommand] has access
     * to. Use the [setContext] function to use a different context.
     */
    val context: AiContext = _context

    /**
     * Sets the current context of this [AiCommand].
     * @param ctx The context object to set.
     * @param T The [AiContext] object to use.
     */
    fun <T: AiContext> setContext(ctx: T) {
        _context = ctx
    }

    /**
     * Adds a subcommand to this [AiCommand], making the subcommand's parent
     * this command object.
     *
     * @param command The subcommand to add.
     */
    fun addSubcommand(command: AiCommand) {
        command._parent = this
        subcommands.add(command)
    }

    /**
     * Adds more than one subcommands to this [AiCommand], making the subcommand's parent
     * this command object.
     *
     * @param commands The list of subcommands to add.
     */
    fun addSubcommands(vararg commands: AiCommand) {
        for (command in commands) {
            addSubcommand(command)
        }
    }

    /**
     * Adds a boolean flag, that must require a value (if needed).
     * @param name The name of the argument flag.
     * @param help The help message of this argument flag.
     * @param longName The long name to use.
     * @param required If the argument flag is required or not.
     */
    fun flag(name: String, help: String = "", longName: String? = null, required: Boolean = false) {
        val builder = Option.builder(name)
            .hasArg(false)
            .desc(help)
            .required(required)

        if (longName != null) {
            builder.longOpt(longName)
        }

        options.addOption(builder.build())
    }

    /**
     * Adds an argument flag, that must require a value (if needed).
     * @param name The name of the argument flag.
     * @param help The help message of this argument flag.
     * @param longName The long name to use.
     * @param required If the argument flag is required or not.
     */
    fun arg(name: String, help: String = "", longName: String? = null, required: Boolean = false) {
        val builder = Option.builder(name)
            .hasArg()
            .desc(help)
            .required(required)

        if (longName != null) {
            builder.longOpt(longName)
        }

        options.addOption(builder.build())
    }

    /**
     * Executes the command and returns the phase of the command as the return type.
     *
     * The **AiPhase** refers to what we need to do next once we have ran the command. The following phases
     * are:
     *
     *   - [AiPhase.PRINT_USAGE]: If we should print the usage to the standard output.
     *   - [AiPhase.FINISHED]: The leading command has finished, and we do nothing.
     *   - [AiPhase.FAIL]: The command has failed to do a specific action.
     *
     * @param args The left-over, positional arguments available.
     */
    abstract fun run(args: List<String>): AiPhase

    /**
     * Executes the command with the arguments used.
     *
     * ## Example
     * ```kotlin
     * object MyCommand: AiCommand("owo", "uwu!") {
     *    override fun run(args: List<String>): AiPhase = AiPhase.PRINT_USAGE
     * }
     *
     * fun main(args: Array<String>) = MyCommand.main(args)
     * ```
     */
    fun main(args: Array<out String>) = main(args.toList())

    /**
     * Executes the command with the arguments used.
     *
     * ## Example
     * ```kotlin
     * object MyCommand: AiCommand("owo", "uwu!") {
     *    override fun run(args: List<String>): AiPhase = AiPhase.PRINT_USAGE
     * }
     *
     * fun main(args: Array<String>) = MyCommand.main(args)
     * ```
     */
    fun main(args: List<String>) {
        val parser = DefaultParser()
        var errored = false
        val _cli = try {
            parser.parse(options, args.toTypedArray())
        } catch (e: Throwable) {
            if (printUsageIfError) {
                printUsageAndExit()
            }

            println("Unable to parse command line: ${e.message}")
            exitProcess(1)
        }

        fun AiCommand.executeCommand(c: CommandLine): AiPhase {
            this.cli = c

            return try {
                run(cli.argList)
            } catch (e: Throwable) {
                errored = true
                if (printUsageIfError) AiPhase.PRINT_USAGE else AiPhase.FINISHED
            }
        }

        val phase = if (args.isEmpty()) {
            executeCommand(_cli)
        } else {
            if (_cli.argList.isEmpty()) {
                executeCommand(_cli)
            } else {
                if (_cli.argList.isEmpty()) {
                    executeCommand(_cli)
                } else {
                    val subcommand = subcommands.firstOrNull { it.name == _cli.argList.first() }
                    subcommand?.executeCommand(parser.parse(options, args.drop(1).toTypedArray())) ?: executeCommand(_cli)
                }
            }
        }

        if (phase == AiPhase.PRINT_USAGE) {
            printUsageAndExit()
        }

        val status = if (errored) 1 else phase.exitCode
        exitProcess(status)
    }

    private fun printUsageAndExit() {
        val formatter = HelpFormatter()
        val writer = StringWriter()
        val help = buildString {
            appendLine("USAGE :: ${usage ?: "$name ${if (subcommands.isNotEmpty()) "[COMMAND] [...ARGS]" else "[...ARGS]"}"}")
            appendLine("> ${help.ifEmpty { "This command doesn't have a help section." }}")
            if (subcommands.isNotEmpty()) {
                appendLine()
                appendLine("SUBCOMMANDS ::")
            }

            for (sub in subcommands) {
                appendLine("  * $name ${sub.name} ${if (sub.subcommands.isNotEmpty()) "[COMMAND] [...ARGS]" else "[...ARGS]"} - ${sub.help.ifEmpty { "This command doesn't have a help section." }}")
            }

            if (subcommands.isNotEmpty()) {
                appendLine()
            }

            appendLine("OPTIONS ::")
        }

        writer.write(help)

        formatter.printOptions(PrintWriter(writer), formatter.width, options, 3, 1)
        println(writer.toString().replace("::usage:", "::\n").trim())

        exitProcess(130)
    }

    companion object {
        private fun inferName(): String = this::class
            .qualifiedName
            ?.replace("Command", "")
            ?: this::class.java.name.replace("Command", "")
    }
}

// +=+ BUILT INS THINGIES :D +=+

/**
 * Returns the option as a string, if it has the option or `null`.
 */
fun AiCommand.stringOrNull(name: String): ReadOnlyProperty<Any?, String?> = ReadOnlyProperty { _, _ ->
    if (cli.hasOption(name))
        cli.getOptionValue(name)
    else
        null
}

/**
 * Returns the option as a string, if it wasn't found, then it throws an [IllegalStateException]
 * @throws IllegalStateException If the option wasn't found.
 */
fun AiCommand.string(name: String): ReadOnlyProperty<Any?, String> = ReadOnlyProperty { _, _ ->
    if (cli.hasOption(name))
        cli.getOptionValue(name)
    else
        throw IllegalStateException("Missing required flag: $name")
}

/**
 * Returns the option as a boolean, if it has the option, or `false`.
 */
fun AiCommand.bool(name: String): ReadOnlyProperty<Any?, Boolean> = ReadOnlyProperty { _, _ ->
    if (cli.hasOption(name)) {
        val value = cli.getOptionValue(name)
        if (value.matches("true|yes|0|t|si$".toRegex()))
            return@ReadOnlyProperty true

        if (value.matches("false|no|1|f|nu*$".toRegex()))
            return@ReadOnlyProperty false
    }

    false
}

/**
 * Returns the option as an integer, if it has the option, or `null`.
 */
fun AiCommand.intOrNull(name: String): ReadOnlyProperty<Any?, Int?> = ReadOnlyProperty { _, _ ->
    if (cli.hasOption(name))
        Integer.parseInt(cli.getOptionValue(name))
    else
        null
}

/**
 * Returns the option as a string, if it wasn't found, then it throws an [IllegalStateException]
 * @throws IllegalStateException If the option wasn't found.
 */
fun AiCommand.int(name: String): ReadOnlyProperty<Any?, Int> = ReadOnlyProperty { _, _ ->
    if (cli.hasOption(name))
        Integer.parseInt(cli.getOptionValue(name))
    else
        throw IllegalStateException("Missing required flag: $name")
}

/**
 * Returns the option as a file, if the name is an absolute path.
 * @param name The name of the argument
 * @param mustBeDir If the file must be a directory, cannot be linked with [mustBeFile].
 * @param mustBeFile If the fist MUST be a file object, cannot be linked with [mustBeDir].
 * @param mustExist If the file MUST exist, if not, it'll bark.
 */
fun AiCommand.file(
    name: String,
    mustBeDir: Boolean = false,
    mustBeFile: Boolean = false,
    mustExist: Boolean = true
): ReadOnlyProperty<Any?, File> = ReadOnlyProperty { _, _ ->
    if (!cli.hasOption(name))
        throw IllegalArgumentException("You must have the flag $name.")

    val path = cli.getOptionValue(name)
    val actualFilePath = when {
        path.startsWith("./") -> System.getProperty("user.dir") + path.replaceFirstChar { "" }
        path.startsWith("~/") -> System.getProperty("user.home") + path
        File(path).isAbsolute -> path
        else -> error("Path $path was not an absolute path.")
    }

    val file = File(actualFilePath)

    if (mustExist && !file.exists())
        throw IllegalStateException("File or directory $actualFilePath doesn't exist.")

    if (mustBeDir && !file.isDirectory)
        throw IllegalStateException("Directory $actualFilePath was not a directory.")

    if (mustBeFile && !file.isFile)
        throw IllegalStateException("File $actualFilePath was not a file.")

    file
}
