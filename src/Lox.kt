package io.djy.klox

import java.io.IOException
import java.io.File
import java.nio.file.Paths
import java.util.List

var hadError = false;

object Lox {
  private fun report(line: Int, location: String, message: String) {
    println("[line $line] Error$location: $message")
    hadError = true
  }

  fun error(token: Token, message: String) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message)
    } else {
      report(token.line, " at '${token.lexeme}'", message)
    }
  }

  fun error(line: Int, message: String) {
    report(line, "", message)
  }

  private fun run(source: String) {
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()
    val parser = Parser(tokens)
    val expr: Expr? = parser.parse()

    // Stop if there was a syntax error.
    if (hadError || expr == null) return

    // Print the AST.
    println(expr!!.ast())
  }

  fun runFile(path: String) {
    val input = File(path).readText()
    run(input)
    if (hadError) System.exit(65)
  }

  fun runPrompt() {
    var running = true

    while (running) {
      print("> ")
      val input = readLine()

      if (input == null) {
        running = false
      } else {
        run(input)
      }

      hadError = false // so we don't exit
    }
  }
}

fun main(args: Array<String>) {
  when (args.size) {
    0    -> Lox.runPrompt()
    1    -> Lox.runFile(args[0])
    else -> println("Usage: klox [script]")
  }
}

