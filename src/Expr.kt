package io.djy.klox

private fun parenthesize(name: String, vararg exprs: Expr) : String {
  return "($name ${exprs.joinToString(separator = " ",
                                      transform = { it.ast() })})"
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

abstract class Expr : Printable {}

class Binary(val left: Expr, val op: Token, val right: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize(op.lexeme, left, right)
  }

  override fun rpn(): String {
    return "${left.rpn()} ${right.rpn()} ${op.lexeme}"
  }
}

class Grouping(val expr: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize("group", expr)
  }

  override fun rpn(): String {
    return expr.rpn()
  }
}

class Literal(val value: Object?) : Expr() {
  override fun ast(): String {
    return value.toString()
  }

  override fun rpn(): String {
    return value.toString()
  }
}

class Unary(val op: Token, val right: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize(op.lexeme, right)
  }

  override fun rpn(): String {
    return "${right.rpn()} ${op.lexeme}"
  }
}

