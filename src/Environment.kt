package io.djy.klox

// I'm not sure if this is the best way to do this in Kotlin...
// What I really want is a single, unique value that means `undefined`.
class Undefined() {}

class Environment(val parent: Environment? = null) {
  var globals = mutableMapOf<String, Any?>()
  var locals = mutableMapOf<Expr, Int>()

  fun define(name: String, value: Any?) {
    globals.put(name, value)
  }

  fun assign(name: Token, value: Any?) {
    if (globals.containsKey(name.lexeme))
      globals.put(name.lexeme, value)
    else if (parent != null)
      parent.assign(name, value)
    else
      throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
  }

  fun assignAt(distance: Int, name: Token, value: Any?) {
    var env: Environment = this
    for (i in 0 until distance) env = env.parent!!
    env.globals.put(name.lexeme, value)
  }

  fun get(name: Token): Any? {
    if (globals.containsKey(name.lexeme)) {
      val v = globals.get(name.lexeme)
      if (v is Undefined)
        throw RuntimeError(name, "Uninitialized variable '${name.lexeme}'")
      else
        return v
    } else if (parent != null) {
      return parent.get(name)
    } else {
      throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
  }

  fun getAt(distance: Int, name: String): Any? {
    var env: Environment = this
    for (i in 0 until distance) env = env.parent!!
    return env.globals.get(name)
  }

  fun lookUpVariable(name: Token, expr: Expr): Any? {
    val distance: Int? = locals.get(expr)
    return if (distance != null)
      getAt(distance, name.lexeme)
    else
      get(name)
  }
}
