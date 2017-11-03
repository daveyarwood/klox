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

fun isTruthy(x: Any?): Boolean {
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
  fun evaluate(env: Environment): Any?
}

abstract class Expr : Printable, Evaluable, Resolvable {}

class AssignExpr(val name: Token, val value: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize("assign", LiteralExpr(name.lexeme), value)
  }

  override fun rpn(): String {
    return "${value.rpn()} ${name.lexeme} assign"
  }

  override fun evaluate(env: Environment): Any? {
    val v = value.evaluate(env)
    val distance = env.locals.get(this)
    if (distance != null)
      env.assignAt(distance, name, v)
    else
      env.assign(name, v)
    return v
  }

  override fun resolve(resolver: Resolver): Void? {
    value.resolve(resolver)
    resolver.resolveLocal(this, name)
    return null
  }
}

class BinaryExpr(val left: Expr, val op: Token, val right: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize(op.lexeme, left, right)
  }

  override fun rpn(): String {
    return "${left.rpn()} ${right.rpn()} ${op.lexeme}"
  }

  override fun evaluate(env: Environment): Any? {
    val x: Any? = left.evaluate(env)
    val y: Any? = right.evaluate(env)

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

  override fun resolve(resolver: Resolver): Void? {
    left.resolve(resolver)
    right.resolve(resolver)
    return null
  }
}

class CallExpr(val callee: Expr, val paren: Token,
               val arguments: List<Expr>): Expr() {
  override fun ast(): String {
    return parenthesize(callee.ast(), *(arguments.toTypedArray()))
  }

  override fun rpn(): String {
    val args: String = arguments.joinToString(separator = " ",
                                              transform = { it.ast() })
    return "${callee.rpn()} $args"
  }

  override fun evaluate(env: Environment): Any? {
    val cValue = callee.evaluate(env)
    if (cValue !is LoxCallable)
      throw RuntimeError(paren, "Can only call functions and classes.")
    val callable = cValue as LoxCallable

    val nArgs = arguments.size
    val arity = callable.arity()
    if (nArgs != arity)
      throw RuntimeError(paren, "Expected $arity arguments but got $nArgs.")
    val args = arguments.map { it.evaluate(env) }

    return callable.call(env, args)
  }

  override fun resolve(resolver: Resolver): Void? {
    callee.resolve(resolver)
    arguments.forEach { it.resolve(resolver) }
    return null
  }
}

class GroupingExpr(val expr: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize("group", expr)
  }

  override fun rpn(): String {
    return expr.rpn()
  }

  override fun evaluate(env: Environment): Any? {
    return expr.evaluate(env)
  }

  override fun resolve(resolver: Resolver): Void? {
    expr.resolve(resolver)
    return null
  }
}

class LiteralExpr(val value: Any?) : Expr() {
  override fun ast(): String {
    return value.toString()
  }

  override fun rpn(): String {
    return value.toString()
  }

  override fun evaluate(env: Environment): Any? {
    return value
  }

  override fun resolve(resolver: Resolver): Void? {
    return null
  }
}

class LogicalExpr(val left: Expr, val op: Token, val right: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize(op.lexeme, left, right)
  }

  override fun rpn(): String {
    return "${left.rpn()} ${right.rpn()} ${op.lexeme}"
  }

  override fun evaluate(env: Environment): Any? {
    val l: Any? = left.evaluate(env)

    return when (op.type) {
      TokenType.OR  -> if (isTruthy(l)) l else right.evaluate(env)
      TokenType.AND -> if (isTruthy(l)) right.evaluate(env) else l
      else -> throw RuntimeError(op, "Unrecognized logical operator")
    }
  }

  override fun resolve(resolver: Resolver): Void? {
    left.resolve(resolver)
    right.resolve(resolver)
    return null
  }
}

class UnaryExpr(val op: Token, val right: Expr) : Expr() {
  override fun ast(): String {
    return parenthesize(op.lexeme, right)
  }

  override fun rpn(): String {
    return "${right.rpn()} ${op.lexeme}"
  }

  override fun evaluate(env: Environment): Any? {
    val x: Any? = right.evaluate(env)

    return when (op.type) {
      TokenType.MINUS -> { checkNumberOperand(op, x); -(x as Double) }
      TokenType.BANG  -> !isTruthy(x)
      else            -> null
    }
  }

  override fun resolve(resolver: Resolver): Void? {
    right.resolve(resolver)
    return null
  }
}

class VariableExpr(val name: Token): Expr() {
  override fun ast(): String {
    return name.lexeme
  }

  override fun rpn(): String {
    return name.lexeme
  }

  override fun evaluate(env: Environment): Any? {
    return env.lookUpVariable(name, this)
  }

  override fun resolve(resolver: Resolver): Void? {
    if (!resolver.scopes.isEmpty() &&
        resolver.scopes.peek().get(name.lexeme) == false)
      Lox.error(name, "Cannot read local variable in its own initializer.")

    resolver.resolveLocal(this, name)
    return null
  }
}

