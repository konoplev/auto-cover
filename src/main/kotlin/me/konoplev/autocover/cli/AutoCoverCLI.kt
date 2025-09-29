package me.konoplev.autocover.cli

import me.konoplev.autocover.services.ConfigurationHelper
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.*

@Component
class AutoCoverCLI(
    private val configurationHelper: ConfigurationHelper,
) : CommandLineRunner {

    private val scanner = Scanner(System.`in`)

    override fun run(vararg args: String?) {
        println("🤖 Auto-Cover CLI Application")
        println("=============================")
        println("Type 'help' for commands, 'quit' to exit")
        println()

        while (true) {
            print("🔤 Enter command: ")
            val input = try {
                scanner.nextLine().trim()
            } catch (e: NoSuchElementException) {
                break
            }

            when {
                configurationHelper.isConfigurationInstructionNeeded() -> {
                    println(configurationHelper.getInstructions())
                    break
                }
                input.isEmpty() -> continue
                input.equals("quit", ignoreCase = true) || input.equals("exit", ignoreCase = true) -> {
                    println("👋 Goodbye!")
                    break
                }
                input.equals("help", ignoreCase = true) -> {
                    showHelp()
                }
                input.equals("clear", ignoreCase = true) -> {
                    // Clear screen
                    print("\u001b[2J\u001b[H")
                    println("🤖 Auto-Cover CLI Application")
                    println("=============================")
                }
                else -> {
                    // do nothing
                }
            }
            println()
        }
    }

    private fun showHelp() {
        println("📖 Available Commands:")
        println("  help                           - Show this help message")
        println("  clear                          - Clear the screen")
        println("  quit / exit                    - Exit the application")
        println()
    }

}
