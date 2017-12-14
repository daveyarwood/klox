package io.djy.klox

import java.util.Stack

class Resolver(val interpreter: Interpreter) {
  var scopes = Stack<MutableMap<String, Boolean>>()
  var currentFunction = FunctionType.NONE
  var currentClass = ClassType.NONE

  fun beginScope() {
    scopes.push(mutableMapOf<String, Boolean>())
  }

  fun endScope() {
    scopes.pop()
  }

  fun declare(name: Token) {
    if (scopes.isEmpty()) return
    val scope = scopes.peek()
    if (scope.containsKey(name.lexeme))
      Lox.error(name, "Variable with this name already declared in this scope.")
    scope.put(name.lexeme, false)
  }

  fun define(name: Token) {
    if (scopes.isEmpty()) return
    scopes.peek().put(name.lexeme, true)
  }

  fun resolveLocal(expr: Expr, name: Token) {
    for (i in (scopes.size - 1) downTo 0) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size - 1 - i)
        return
      }
    }

    // Not found. Assume it is global.
  }

  fun resolveFunction(function: FunctionStmt, type: FunctionType) {
    val enclosingFunction = currentFunction
    currentFunction = type

    beginScope()
    for (param: Token in function.parameters) {
      declare(param)
      define(param)
    }
    function.body.forEach { it.resolve(this) }
    endScope()

    currentFunction = enclosingFunction
  }

  fun resolveClass(klass: ClassStmt) {
    val enclosingClass = currentClass
    currentClass = ClassType.CLASS

    if (klass.superclassExpr != null) {
      currentClass = ClassType.SUBCLASS;
      klass.superclassExpr.resolve(this)
      beginScope()
      scopes.peek().put("super", true)
    }

    beginScope()
    scopes.peek().put("this", true)
    for (method in klass.methods) {
      val declaration = if (method.name.lexeme.equals("init"))
        FunctionType.INITIALIZER
      else
        FunctionType.METHOD
      resolveFunction(method, declaration)
    }
    endScope()

    if (klass.superclassExpr != null)
      endScope();

    currentClass = enclosingClass
  }
}

// Used by Expr and Stmt
interface Resolvable {
  fun resolve(resolver: Resolver): Void?
}

