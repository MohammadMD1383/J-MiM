package ir.mimlang.jmim.lang.lexer

import ir.mimlang.jmim.lang.util.Position

class LexerException(message: String) : Exception(message) {
	lateinit var position: Position
	
	infix fun at(pos: Position): LexerException = this.apply { position = pos }
}