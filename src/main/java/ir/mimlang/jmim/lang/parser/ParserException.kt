package ir.mimlang.jmim.lang.parser

import ir.mimlang.jmim.lang.util.TextRange

class ParserException(message: String? = null) : Exception(message) {
	lateinit var range: TextRange
	
	infix fun at(textRange: TextRange): ParserException = this.apply { range = textRange }
}