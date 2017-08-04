package io.djy.klox

interface LoxCallable {
  fun call(environment: Environment, arguments: List<Any?>): Any?
  fun arity(): Int
}
