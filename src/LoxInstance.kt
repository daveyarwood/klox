package io.djy.klox

class LoxInstance(val klass: LoxClass) {
  var fields = HashMap<String, Any?>()

  fun get(name: Token): Any? {
    if (fields.containsKey(name.lexeme))
      return fields.get(name.lexeme)

    val method: LoxFunction? = klass.findMethod(this, name.lexeme)
    if (method != null) return method

    throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
  }

  fun set(name: Token, value: Any?) {
    fields.put(name.lexeme, value)
  }

  override fun toString(): String {
    return "${klass.name} instance"
  }
}
