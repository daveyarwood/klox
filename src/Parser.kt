package io.djy.klox

class ParseError() : RuntimeException() {}

class Parser(val tokens: List<Token>) {
  var current: Int = 0

  private fun peek(): Token {
    return tokens.get(current)
  }

  private fun previous(): Token {
    return tokens.get(current -1)
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

  // expression := equality
  private fun expression(): Expr {
    return equality()
  }

  // equality := comparison ( ( "!=" | "==" ) comparison )*
  private fun equality(): Expr {
    var expr: Expr = comparison()

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      val op: Token = previous()
      val right: Expr = comparison()
      expr = Binary(expr, op, right)
    }

    return expr
  }

  // comparison := term ( ( ">" | ">=" | "<" | "<=" ) term )*
  private fun comparison(): Expr {
    var expr: Expr = term()

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS,
                 TokenType.LESS_EQUAL)) {
      val op: Token = previous()
      val right: Expr = term()
      expr = Binary(expr, op, right)
    }

    return expr
  }

  // term := factor ( ( "-" | "+" ) factor )*
  private fun term(): Expr {
    var expr: Expr = factor()

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      val op: Token = previous()
      val right: Expr = factor()
      expr = Binary(expr, op, right)
    }

    return expr
  }

  // factor := unary ( ( "/" | "*" ) unary )*
  private fun factor(): Expr {
    var expr: Expr = unary()

    while (match(TokenType.SLASH, TokenType.STAR)) {
      val op: Token = previous()
      val right: Expr = unary()
      expr = Binary(expr, op, right)
    }

    return expr
  }

  // unary := ( "!" | "-" ) unary | primary
  private fun unary(): Expr {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      val op: Token = previous()
      val right: Expr = unary()
      return Unary(op, right)
    } else {
      return primary()
    }
  }

  // primary := NUMBER | STRING | "false" | "true" | "nil" | "(" expression ")"
  private fun primary(): Expr {
    when {
      match(TokenType.FALSE) -> return Literal(false as Object)
      match(TokenType.TRUE) -> return Literal(true as Object)
      match(TokenType.NIL) -> return Literal(null as Object?)
      match(TokenType.NUMBER, TokenType.STRING) -> {
        return Literal(previous().literal)
      }
      match(TokenType.LEFT_PAREN) -> {
        val expr: Expr = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
        return Grouping(expr)
      }
      else -> throw error(peek(), "Expect expression.")
    }
  }

  fun parse(): Expr? {
    return try {
      expression()
    } catch (e: ParseError) {
      null
    }
  }
}

