package io.djy.klox

import java.io.IOException
import java.io.File
import java.nio.file.Paths
import java.util.List

var hadError = false;

private fun report(line: Int, location: String, message: String) {
  println("[line $line] Error$location: $message")
  hadError = true
}

fun error(line: Int, message: String) {
  report(line, "", message)
}

private fun run(source: String) {
  val scanner = Scanner(source)
  val tokens = scanner.scanTokens()

  // For now, just print the tokens.
  for (token in tokens) {
    println(token)
  }
}

private fun runFile(path: String) {
  val input = File(path).readText()
  run(input)
  if (hadError) System.exit(65)
}

private fun runPrompt() {
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

fun main(args: Array<String>) {
  when (args.size) {
    0    -> runPrompt()
    1    -> runFile(args[0])
    else -> println("Usage: klox [script]")
  }
}
