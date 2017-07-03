package io.djy.klox

// I'm not sure if this is the best way to do this in Kotlin...
// What I really want is a single, unique value that means `undefined`.
class Undefined() {}

class Environment(val parent: Environment? = null) {
  var values = mutableMapOf<String, Any?>()

  fun define(name: String, value: Any?) {
    values.put(name, value)
  }

  fun assign(name: Token, value: Any?) {
    if (values.containsKey(name.lexeme))
      values.put(name.lexeme, value)
    else if (parent != null)
      parent.assign(name, value)
    else
      throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
  }

  fun get(name: Token): Any? {
    if (values.containsKey(name.lexeme)) {
      val v = values.get(name.lexeme)
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
}
