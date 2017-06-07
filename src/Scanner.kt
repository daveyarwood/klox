package io.djy.klox

val keywords = mapOf(
  "and" to TokenType.AND,
  "class" to TokenType.CLASS,
  "else" to TokenType.ELSE,
  "false" to TokenType.FALSE,
  "for" to TokenType.FOR,
  "fun" to TokenType.FUN,
  "if" to TokenType.IF,
  "nil" to TokenType.NIL,
  "or" to TokenType.OR,
  "print" to TokenType.PRINT,
  "return" to TokenType.RETURN,
  "super" to TokenType.SUPER,
  "this" to TokenType.THIS,
  "true" to TokenType.TRUE,
  "var" to TokenType.VAR,
  "while" to TokenType.WHILE
)

class Scanner(val source: String) {
  val tokens = ArrayList<Token>()
  private var start = 0
  private var current = 0
  private var line = 1

  private fun peek(): Char {
    if (current < source.length) {
      return source.get(current)
    } else {
      return '\u0000'
    }
  }

  private fun peekNext(): Char {
    if (current + 1 < source.length) {
      return source.get(current + 1)
    } else {
      return '\u0000'
    }
  }

  private fun advance(): Char {
    val char: Char = source.get(current)
    current++
    return char
  }

  private fun match(expected: Char): Boolean {
    if (current >= source.length) return false
    if (source.get(current) != expected) return false

    current++
    return true
  }

  private fun addToken(tokenType: TokenType) {
    addToken(tokenType, null)
  }

  private fun addToken(tokenType: TokenType, literal: Object?) {
    val text: String = source.substring(start, current)
    tokens.add(Token(tokenType, text, literal, line))
  }

  private fun skipComment() {
    while (peek() != '\n' && current < source.length) advance()
  }

  private fun skipBlockComment() {
    var level = 1
    while (level > 0) {
      if (current >= source.length) {
        Lox.error(line, "Unterminated block comment.")
        return
      }

      when (peek()) {
        '\n' -> { line++; advance() }
        '/'  -> if (peekNext() == '*') {
          level++
          advance()
          advance()
        } else {
          advance()
        }
        '*' -> if (peekNext() == '/') {
          level--
          advance()
          advance()
        } else {
          advance()
        }
        else -> advance()
      }
    }
  }

  private fun parseString() {
    while (peek() != '"' && current < source.length) {
      if (peek() == '\n') line++
      advance()
    }

    if (current >= source.length) {
      Lox.error(line, "Unterminated string.")
      return
    }

    // Consume the closing ".
    advance()

    // Trim the surrounding quotes.
    val contents: String = source.substring(start + 1, current - 1)
    addToken(TokenType.STRING, contents as Object?)
  }

  private fun parseNumber() {
    while (peek() in '0'..'9') advance()

    // Look for a fractional part.
    if (peek() == '.' && peekNext() in '0'..'9') {
      // Consume the '.'
      advance()
      while (peek() in '0'..'9') advance()
    }

    val number: Double = source.substring(start, current).toDouble()
    addToken(TokenType.NUMBER, number as Object?)
  }

  private fun parseIdentifier() {
    var char: Char = peek()
    while ((char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' ||
    char == '_') && current < source.length) {
      advance()
      char = peek()
    }

    val text: String = source.substring(start, current)
    val token: TokenType = keywords.getOrDefault(text, TokenType.IDENTIFIER)
    addToken(token)
  }

  private fun scanToken() {
    val char: Char = advance()
    when (char) {
      ' ', '\r', '\t' -> "ignore whitespace"
      '\n' -> line++
      '(' -> addToken(TokenType.LEFT_PAREN)
      ')' -> addToken(TokenType.RIGHT_PAREN)
      '{' -> addToken(TokenType.LEFT_BRACE)
      '}' -> addToken(TokenType.RIGHT_BRACE)
      ',' -> addToken(TokenType.COMMA)
      '.' -> addToken(TokenType.DOT)
      '-' -> addToken(TokenType.MINUS)
      '+' -> addToken(TokenType.PLUS)
      ';' -> addToken(TokenType.SEMICOLON)
      '*' -> addToken(TokenType.STAR)
      '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
      '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
      '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
      '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
      '/' -> when {
        match('/') -> skipComment()
        match('*') -> skipBlockComment()
        else -> addToken(TokenType.SLASH)
      }
      '"' -> parseString()
      in '0'..'9' -> parseNumber()
      in 'a'..'z', in 'A'..'Z', '_' -> parseIdentifier()
      else -> Lox.error(line, "Unexpected character: $char")
    }
  }

  fun scanTokens(): ArrayList<Token> {
    while (current < source.length) {
      // We are at the beginning of the next lexeme.
      start = current
      scanToken()
    }

    tokens.add(Token(TokenType.EOF, "", null, line))
    return tokens
  }
}
