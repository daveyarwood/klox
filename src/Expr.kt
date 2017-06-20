package io.djy.klox

private fun parenthesize(name: String, vararg exprs: Expr) : String {
  return "($name ${exprs.joinToString(separator = " ",
                                      transform = { it.ast() })})"
}

private fun checkNumberOperand(op: Token, x: Any?) {
  if (x !is Double) {
    throw RuntimeError(op, "Operand must be a number.")
  }
}

private fun checkNumberOperands(op: Token, x: Any?, y: Any?) {
  if (x !is Double || y !is Double) {
    throw RuntimeError(op, "Operands must be numbers.")
  }
}

private fun isTruthy(x: Any?): Boolean {
  return (x ?: false) != false
}

interface Printable {
  // Returns a string representation of the expression as a Lisp-like AST.
  //
  // e.g. -123 * (45.67)  becomes  (* (- 123) (group 45.67))
  fun ast(): String

  // Returns a string representation of the expression in Reverse Polish
  // Notation.
  //
  // e.g. (1 + 2) * (4 - 3)  becomes  1 2 + 4 3 - *
  fun rpn(): String
}

interface Evaluable {
  fun evaluate(): Any?
}

abstract class Expr : Printable, Evaluable {}

class Binary(val left: Expr, val op: Token, val right: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize(op.lexeme, left, right)
  }

  override fun rpn(): String {
    return "${left.rpn()} ${right.rpn()} ${op.lexeme}"
  }

  override fun evaluate(): Any? {
    val x: Any? = left.evaluate()
    val y: Any? = right.evaluate()

    return when (op.type) {
      TokenType.MINUS -> {
        checkNumberOperands(op, x, y)
        x as Double - y as Double
      }
      TokenType.SLASH -> {
        checkNumberOperands(op, x, y)
        if (y == 0.0) throw RuntimeError(op, "Can't divide by zero.")
        x as Double / y as Double
      }
      TokenType.STAR -> {
        checkNumberOperands(op, x, y)
        x as Double * y as Double
      }
      TokenType.PLUS  -> {
        if (x is Double && y is Double) {
          x + y
        } else if (x is String && y is String) {
          x + y
        } else {
          val msg: String = "Operands must be two numbers or two strings."
          throw RuntimeError(op, msg)
        }
      }
      TokenType.GREATER -> {
        checkNumberOperands(op, x, y)
        x as Double > y as Double
      }
      TokenType.GREATER_EQUAL -> {
        checkNumberOperands(op, x, y)
        x as Double >= y as Double
      }
      TokenType.LESS -> {
        checkNumberOperands(op, x, y)
        (x as Double) < y as Double
      }
      TokenType.LESS_EQUAL -> {
        checkNumberOperands(op, x, y)
        x as Double <= y as Double
      }
      TokenType.BANG_EQUAL    -> x != y
      TokenType.EQUAL_EQUAL   -> x == y
      else -> null
    }
  }
}

class Grouping(val expr: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize("group", expr)
  }

  override fun rpn(): String {
    return expr.rpn()
  }

  override fun evaluate(): Any? {
    return expr.evaluate()
  }
}

class Literal(val value: Object?) : Expr() {
  override fun ast(): String {
    return value.toString()
  }

  override fun rpn(): String {
    return value.toString()
  }

  override fun evaluate(): Any? {
    return value
  }
}

class Unary(val op: Token, val right: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize(op.lexeme, right)
  }

  override fun rpn(): String {
    return "${right.rpn()} ${op.lexeme}"
  }

  override fun evaluate(): Any? {
    val x: Any? = right.evaluate()

    return when (op.type) {
      TokenType.MINUS -> { checkNumberOperand(op, x); -(x as Double) }
      TokenType.BANG  -> !isTruthy(x)
      else            -> null
    }
  }
}

