package io.djy.klox

interface Executable {
  fun execute(env: Environment): Any?
}

abstract class Stmt : Executable {}

class ExpressionStmt(val expr: Expr) : Stmt() {
  override fun execute(env: Environment): Any? {
    return expr.evaluate(env)
  }
}

class PrintStmt(val expr: Expr) : Stmt() {
  override fun execute(env: Environment): Any? {
    println(stringify(expr.evaluate(env)))
    return null
  }
}

class VarStmt(val name: Token, val initializer: Expr?) : Stmt() {
  override fun execute(env: Environment): Any? {
    env.define(name.lexeme, initializer?.evaluate(env) ?: Undefined())
    return null
  }
}

class BlockStmt(val statements: List<Stmt>) : Stmt() {
  override fun execute(env: Environment): Any? {
    var innerEnv = Environment(env)
    for (statement: Stmt in statements) statement.execute(innerEnv)
    return null
  }
}
