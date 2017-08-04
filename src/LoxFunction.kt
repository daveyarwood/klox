package io.djy.klox

class LoxFunction(val declaration: FunctionStmt,
                  val closure: Environment) : LoxCallable {
  override fun call(environment: Environment, arguments: List<Any?>): Any? {
    val env = Environment(closure)
    (declaration.parameters zip arguments).forEach { (param, arg) ->
      env.define(param.lexeme, arg)
    }

    try {
      declaration.body.forEach { it.execute(env) }
      return null
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
