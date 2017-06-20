package io.djy.klox

private fun stringify(x: Any?): String {
  return when (x) {
    null -> "nil"
    is Double -> {
      var text: String = x.toString()
      // HACK: work around Java adding ".0" to integer-valued doubles
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length - 2)
      }
      text
    }
    is String -> "\"${x.toString()}\""
    else -> x.toString()
  }
}

private fun runtimeError(e: RuntimeError) {
  println("${e.message}\n[line ${e.token.line}]")
  hadRuntimeError = true
}

class Interpreter {
  fun interpret(expr: Expr) {
    try {
      val value: Any? = expr.evaluate()
      println(stringify(value))
    } catch (e: RuntimeError) {
      runtimeError(e)
    }
  }
}
