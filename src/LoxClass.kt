package io.djy.klox

class LoxClass(val name: String, val superclass: LoxClass?,
               val methods: Map<String, LoxFunction>) : LoxCallable {
  override fun call(environment: Environment, arguments: List<Any?>): Any? {
    val instance = LoxInstance(this)
    methods.get("init")?.bind(instance)?.call(environment, arguments)
    return instance
  }

  fun findMethod(instance: LoxInstance, name: String): LoxFunction? {
    if (methods.containsKey(name))
      return methods.get(name)!!.bind(instance)
    else
      return superclass?.findMethod(instance, name)
  }

  override fun arity(): Int {
    return methods.get("init")?.arity() ?: 0
  }

  override fun toString(): String { return name }
}
