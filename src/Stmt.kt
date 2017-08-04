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

class ReturnStmt(val keyword: Token, val value: Expr?) : Stmt() {
  override fun execute(env: Environment): Any? {
    throw Return(value?.evaluate(env))
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

class FunctionStmt(val name: Token, val parameters: List<Token>,
                   val body: List<Stmt>): Stmt() {
  override fun execute(env: Environment): Any? {
    env.define(name.lexeme, LoxFunction(this, env))
    return null
  }
}

class IfStmt(val condition: Expr, val thenBranch: Stmt,
             val elseBranch: Stmt?) : Stmt() {
  override fun execute(env: Environment): Any? {
    return if (isTruthy(condition.evaluate(env)))
      thenBranch.execute(env)
    else
      elseBranch?.execute(env)
  }
}

class WhileStmt(val condition: Expr, val body: Stmt) : Stmt() {
  override fun execute(env: Environment): Any? {
    while (isTruthy(condition.evaluate(env))) body.execute(env)
    return null
  }
}
