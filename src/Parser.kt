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
    types.forEach { if (check(it)) { advance(); return true } }
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
    val expr: Expr = or()

    if (match(TokenType.EQUAL)) {
      val equals: Token = previous()
      val value: Expr = assignment()

      when {
        expr is VariableExpr -> return AssignExpr(expr.name, value)

        expr is GetExpr -> {
          val getExpr = expr as GetExpr
          return SetExpr(getExpr.obj, getExpr.name, value)
        }

        else -> error(equals, "Invalid assignment target.")
      }
    }

    return expr
  }

  private fun or(): Expr {
    var expr: Expr = and()

    while (match(TokenType.OR)) {
      val op: Token = previous()
      val right: Expr = and()
      expr = LogicalExpr(expr, op, right)
    }

    return expr
  }

  private fun and(): Expr {
    var expr: Expr = equality()

    while (match(TokenType.AND)) {
      val op: Token = previous()
      val right: Expr = equality()
      expr = LogicalExpr(expr, op, right)
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
      return call()
    }
  }

  private fun call(): Expr {
    var expr: Expr = primary()

    loop@ while (true) {
      when {
        match(TokenType.LEFT_PAREN) -> {
          var arguments = ArrayList<Expr>()

          if (!check(TokenType.RIGHT_PAREN)) {
            do {
              if (arguments.size >= 32)
              error(peek(), "Cannot have more than 32 arguments.")
              arguments.add(expression())
            } while (match(TokenType.COMMA))
          }

          val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
          expr = CallExpr(expr, paren, arguments)
        }

        match(TokenType.DOT) -> {
          val name: Token = consume(TokenType.IDENTIFIER,
                                    "Expect property name after '.'.")
          expr = GetExpr(expr, name)
        }

        else -> break@loop
      }
    }

    return expr
  }

  private fun primary(): Expr {
    when {
      match(TokenType.FALSE)      -> return LiteralExpr(false)
      match(TokenType.TRUE)       -> return LiteralExpr(true)
      match(TokenType.NIL)        -> return LiteralExpr(null)
      match(TokenType.THIS)       -> return ThisExpr(previous())
      match(TokenType.IDENTIFIER) -> return VariableExpr(previous())

      match(TokenType.NUMBER, TokenType.STRING) -> {
        return LiteralExpr(previous().literal)
      }

      match(TokenType.SUPER) -> {
        val keyword: Token = previous()
        consume(TokenType.DOT, "Expect '.' after 'super'.")
        val method: Token = consume(
          TokenType.IDENTIFIER, "Expect superclass method name."
        )
        return SuperExpr(keyword, method)
      }

      match(TokenType.LEFT_PAREN) -> {
        val expr: Expr = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
        return GroupingExpr(expr)
      }

      else -> throw error(peek(), "Expect expression.")
    }
  }

  private fun printStatement(): Stmt {
    val expr = expression()
    consume(TokenType.SEMICOLON, "Expect ';' after value.")
    return PrintStmt(expr)
  }

  private fun returnStatement(): Stmt {
    val keyword: Token = previous()
    val value: Expr? = if (!check(TokenType.SEMICOLON))
                         expression()
                       else
                         null
    consume(TokenType.SEMICOLON, "Expect ';' after return value.")
    return ReturnStmt(keyword, value)
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

  private fun ifStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
    val condition: Expr = expression()
    consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
    val thenBranch: Stmt = statement()
    val elseBranch: Stmt? = if (match(TokenType.ELSE)) statement() else null
    return IfStmt(condition, thenBranch, elseBranch)
  }

  private fun whileStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
    val condition: Expr = expression()
    consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.")
    val body: Stmt = statement()
    return WhileStmt(condition, body)
  }

  private fun forStatement(): Stmt {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

    val initializer: Stmt? = when {
      match(TokenType.SEMICOLON) -> null
      match(TokenType.VAR)       -> varDeclaration()
      else                       -> expressionStatement()
    }

    val condition: Expr = if (!check(TokenType.SEMICOLON))
      expression()
    else
      LiteralExpr(true)
    consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")

    val increment: Expr? = if (!check(TokenType.RIGHT_PAREN))
      expression()
    else
      null
    consume(TokenType.RIGHT_PAREN, "Expect ')' after 'for' clauses.")

    var body: Stmt = statement()

    if (increment != null)
      body = BlockStmt(listOf(body, ExpressionStmt(increment)))

    body = WhileStmt(condition, body)

    if (initializer != null)
      body = BlockStmt(listOf(initializer, body))

    return body
  }

  private fun statement(): Stmt {
    return when {
      match(TokenType.PRINT)      -> printStatement()
      match(TokenType.RETURN)     -> returnStatement()
      match(TokenType.LEFT_BRACE) -> BlockStmt(block())
      match(TokenType.IF)         -> ifStatement()
      match(TokenType.WHILE)      -> whileStatement()
      match(TokenType.FOR)        -> forStatement()
      else                        -> expressionStatement()
    }
  }

  private fun varDeclaration(): Stmt {
    val name: Token = consume(TokenType.IDENTIFIER, "Expect variable name.")
    val initializer: Expr? = if (match(TokenType.EQUAL)) expression() else null
    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
    return VarStmt(name, initializer)
  }

  private fun function(kind: String): FunctionStmt {
    val name: Token = consume(TokenType.IDENTIFIER, "Expect $kind name.")

    consume(TokenType.LEFT_PAREN, "Expect '(' after $kind name.")

    var parameters = ArrayList<Token>()

    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size >= 32)
          error(peek(), "Cannot have more than 32 parameters.")
        parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
      } while (match(TokenType.COMMA))
    }

    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
    consume(TokenType.LEFT_BRACE, "Expect '{' before $kind body.'")

    val body: List<Stmt> = block()

    return FunctionStmt(name, parameters, body)
  }

  private fun classDeclaration(): ClassStmt {
    val name: Token = consume(TokenType.IDENTIFIER, "Expect class name.")

    val superclass: Expr? = if (match(TokenType.LESS)) {
      consume(TokenType.IDENTIFIER, "Expect superclass name.")
      VariableExpr(previous())
    } else {
      null
    }

    consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")

    var methods = ArrayList<FunctionStmt>()
    while (!check(TokenType.RIGHT_BRACE) && peek().type != TokenType.EOF) {
      methods.add(function("method"))
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
    return ClassStmt(name, superclass, methods)
  }

  private fun declaration(): Stmt? {
    try {
      return when {
        match(TokenType.VAR)   -> varDeclaration()
        match(TokenType.CLASS) -> classDeclaration()
        match(TokenType.FUN)   -> function("function")
        else                   -> statement()
      }
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

