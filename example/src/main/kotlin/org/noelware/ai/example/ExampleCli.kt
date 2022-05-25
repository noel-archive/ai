package org.noelware.ai.example

import org.noelware.ai.AiCommand
import org.noelware.ai.AiPhase
import org.noelware.ai.stringOrNull

object ExampleCli: AiCommand("example", printUsageIfError = true) {
    init {
        addSubcommand(SomeSubcommand)

        arg("a", "heck!", "aaaa")
    }

    override fun run(args: List<String>): AiPhase {
        val arg by stringOrNull("a")

        println("hi :DDDDD (arg=$arg)")
        return AiPhase.FINISHED
    }
}

object SomeSubcommand: AiCommand("h") {
    override fun run(args: List<String>): AiPhase {
        return AiPhase.PRINT_USAGE
    }
}

fun main(args: Array<String>) = ExampleCli.main(args)
