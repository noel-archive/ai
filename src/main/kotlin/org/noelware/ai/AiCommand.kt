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

package org.noelware.ai

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.PrintWriter
import java.io.StringWriter
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
    internal val options = Options()

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
     *
     * @param args The left-over, positional arguments available.
     */
    abstract fun run(args: List<String>): AiPhase

    fun main(args: Array<out String>) = main(args.toList())

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

        val status = if (errored) 1 else 0
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

fun AiCommand.stringOrNull(name: String): String? {
    if (cli.hasOption(name))
        return cli.getOptionValue(name)

    return null
}

fun AiCommand.string(name: String): String = stringOrNull(name) ?: error("Missing required flag: $name")
