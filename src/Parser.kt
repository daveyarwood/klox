package io.djy.klox

class ParseError() : RuntimeException() {}

class Parser(val tokens: List<Token>) {
  var current: Int = 0

  private fun peek(): Token {
    return tokens.get(current)
  }

  private fun previous(): Token {
    return tokens.get(current - 1)
  }

  private fun check(tokenType: TokenType): Boolean {
    if (peek().type == TokenType.EOF) {
      return false
    } else {
      return peek().type == tokenType
    }
  }

  private fun advance(): Token {
    if (peek().type != TokenType.EOF) {
      current++
    }
    return previous()
  }

  private fun match(vararg types: TokenType): Boolean {
    for (type: TokenType in types) {
      if (check(type)) {
        advance()
        return true
      }
    }

    return false
  }

  private fun error(token: Token, message: String): ParseError {
    Lox.error(token, message)
    return ParseError()
  }

  private fun consume(type: TokenType, message: String): Token {
    if (check(type)) return advance()
    throw error(peek(), message)
  }

  private fun synchronize() {
    advance()

    while (peek().type != TokenType.EOF) {
      if (previous().type == TokenType.SEMICOLON) return

      when (peek().type) {
        TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR,
        TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN ->
          return
      }

      advance()
    }
  }

  private fun expression(): Expr {
    return assignment()
  }

  private fun assignment(): Expr {
    val expr: Expr = equality()

    if (match(TokenType.EQUAL)) {
      val equals: Token = previous()
      val value: Expr = assignment()

      if (expr is VariableExpr)
        return AssignExpr(expr.name, value)
      else
        error(equals, "Invalid assignment target.")
    }

    return expr
  }

  private fun equality(): Expr {
    var expr: Expr = comparison()

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      val op: Token = previous()
      val right: Expr = comparison()
      expr = BinaryExpr(expr, op, right)
    }

    return expr
  }

  private fun comparison(): Expr {
    var expr: Expr = term()

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS,
                 TokenType.LESS_EQUAL)) {
      val op: Token = previous()
      val right: Expr = term()
      expr = BinaryExpr(expr, op, right)
    }

    return expr
  }

  private fun term(): Expr {
    var expr: Expr = factor()

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      val op: Token = previous()
      val right: Expr = factor()
      expr = BinaryExpr(expr, op, right)
    }

    return expr
  }

  private fun factor(): Expr {
    var expr: Expr = unary()

    while (match(TokenType.SLASH, TokenType.STAR)) {
      val op: Token = previous()
      val right: Expr = unary()
      expr = BinaryExpr(expr, op, right)
    }

    return expr
  }

  private fun unary(): Expr {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      val op: Token = previous()
      val right: Expr = unary()
      return UnaryExpr(op, right)
    } else {
      return primary()
    }
  }

  private fun primary(): Expr {
    when {
      match(TokenType.FALSE) -> return LiteralExpr(false as Object)
      match(TokenType.TRUE) -> return LiteralExpr(true as Object)
      match(TokenType.NIL) -> return LiteralExpr(null as Object?)
      match(TokenType.NUMBER, TokenType.STRING) -> {
        return LiteralExpr(previous().literal)
      }
      match(TokenType.LEFT_PAREN) -> {
        val expr: Expr = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
        return GroupingExpr(expr)
      }
      match(TokenType.IDENTIFIER) -> return VariableExpr(previous())
      else -> throw error(peek(), "Expect expression.")
    }
  }

  private fun printStatement(): Stmt {
    val expr = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after value.")
    return PrintStmt(expr)
  }

  private fun expressionStatement(): Stmt {
    val expr = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after expression.")
    return ExpressionStmt(expr)
  }

  private fun block(): List<Stmt> {
    var statements = ArrayList<Stmt>()

    while (!check(TokenType.RIGHT_BRACE) && peek().type != TokenType.EOF) {
      val declaration = declaration()
      if (declaration != null) statements.add(declaration!!)
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")

    return statements
  }

  private fun statement(): Stmt {
    return if (match(TokenType.PRINT))
      printStatement()
    else if (match(TokenType.LEFT_BRACE))
      BlockStmt(block())
    else
      expressionStatement()
  }

  private fun varDeclaration(): Stmt {
    val name: Token = consume(TokenType.IDENTIFIER, "Expect variable name.")
    val initializer: Expr? = if (match(TokenType.EQUAL)) expression() else null
    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
    return VarStmt(name, initializer)
  }

  private fun declaration(): Stmt? {
    try {
      return if (match(TokenType.VAR)) varDeclaration() else statement()
    } catch (e: ParseError) {
      synchronize()
      return null
    }
  }

  fun parse(): List<Stmt> {
    var statements = ArrayList<Stmt>()

    while (peek().type != TokenType.EOF) {
      val declaration: Stmt? = declaration()
      if (declaration != null) statements.add(declaration!!)
    }

    return statements
  }
}

