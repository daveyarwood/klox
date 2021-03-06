package io.djy.klox

interface Executable {
  fun execute(env: Environment): Any?
}

abstract class Stmt : Executable, Resolvable {}

class ExpressionStmt(val expr: Expr) : Stmt() {
  override fun execute(env: Environment): Any? {
    return expr.evaluate(env)
  }

  override fun resolve(resolver: Resolver): Void? {
    expr.resolve(resolver)
    return null
  }
}

class PrintStmt(val expr: Expr) : Stmt() {
  override fun execute(env: Environment): Any? {
    println(stringify(expr.evaluate(env)))
    return null
  }

  override fun resolve(resolver: Resolver): Void? {
    expr.resolve(resolver)
    return null
  }
}

class ReturnStmt(val keyword: Token, val value: Expr?) : Stmt() {
  override fun execute(env: Environment): Any? {
    throw Return(value?.evaluate(env))
  }

  override fun resolve(resolver: Resolver): Void? {
    if (resolver.currentFunction == FunctionType.NONE)
      Lox.error(keyword, "Cannot return from top-level code.")

    if (resolver.currentFunction == FunctionType.INITIALIZER)
      Lox.error(keyword, "Cannot return a value from an initializer.")

    value?.resolve(resolver)
    return null
  }
}

class VarStmt(val name: Token, val initializer: Expr?) : Stmt() {
  override fun execute(env: Environment): Any? {
    env.define(name.lexeme, initializer?.evaluate(env) ?: Undefined())
    return null
  }

  override fun resolve(resolver: Resolver): Void? {
    resolver.declare(name)
    initializer?.resolve(resolver)
    resolver.define(name)
    return null
  }
}

class BlockStmt(val statements: List<Stmt>) : Stmt() {
  override fun execute(env: Environment): Any? {
    var innerEnv = Environment(env)
    statements.forEach { it.execute(innerEnv) }
    return null
  }

  override fun resolve(resolver: Resolver): Void? {
    resolver.beginScope()
    statements.forEach { it.resolve(resolver) }
    resolver.endScope()
    return null
  }
}

class ClassStmt(val name: Token, val superclassExpr: Expr?,
                val methods: List<FunctionStmt>) : Stmt() {
  override fun execute(env: Environment): Any? {
    env.define(name.lexeme, null)

    val superclass = superclassExpr?.evaluate(env)

    if (superclass != null && superclass !is LoxClass)
      throw RuntimeError(name, "Superclass must be a class.")

    val classEnv = Environment(env)
    classEnv.define("super", superclass)

    var classMethods = HashMap<String, LoxFunction>()
    for (method in methods) {
      val fn = LoxFunction(method, classEnv, method.name.lexeme.equals("init"))
      classMethods.put(method.name.lexeme, fn)
    }

    val klass = LoxClass(name.lexeme, superclass as LoxClass?, classMethods)
    env.assign(name, klass)
    return null
  }

  override fun resolve(resolver: Resolver): Void? {
    resolver.declare(name)
    resolver.define(name)
    resolver.resolveClass(this)
    return null
  }
}

class FunctionStmt(val name: Token, val parameters: List<Token>,
                   val body: List<Stmt>): Stmt() {
  override fun execute(env: Environment): Any? {
    env.define(name.lexeme, LoxFunction(this, env, false))
    return null
  }

  override fun resolve(resolver: Resolver): Void? {
    resolver.declare(name)
    resolver.define(name)
    resolver.resolveFunction(this, FunctionType.FUNCTION)
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

  override fun resolve(resolver: Resolver): Void? {
    condition.resolve(resolver)
    thenBranch.resolve(resolver)
    elseBranch?.resolve(resolver)
    return null
  }
}

class WhileStmt(val condition: Expr, val body: Stmt) : Stmt() {
  override fun execute(env: Environment): Any? {
    while (isTruthy(condition.evaluate(env))) body.execute(env)
    return null
  }

  override fun resolve(resolver: Resolver): Void? {
    condition.resolve(resolver)
    body.resolve(resolver)
    return null
  }
}
