package io.djy.klox

class LoxFunction(
  val declaration: FunctionStmt, val closure: Environment,
  val isInitializer: Boolean
  ) : LoxCallable {
  fun bind(instance: LoxInstance): LoxFunction {
    var env = Environment(closure)
    env.define("this", instance)
    return LoxFunction(declaration, env, isInitializer)
  }

  override fun call(environment: Environment, arguments: List<Any?>): Any? {
    val env = Environment(closure)
    (declaration.parameters zip arguments).forEach { (param, arg) ->
      env.define(param.lexeme, arg)
    }

    try {
      declaration.body.forEach { it.execute(env) }
      return if (isInitializer) closure.getAt(0, "this") else null
    } catch (r: Return) {
      return r.value
    }
  }

  override fun arity(): Int {
    return declaration.parameters.size
  }

  override fun toString(): String {
    return "<fn ${declaration.name.lexeme}>"
  }
}
