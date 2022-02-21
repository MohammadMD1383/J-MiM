package ir.mimlang.jmim.lang.parser

import ir.mimlang.jmim.lang.util.TextRange

class ParserException(message: String) : Exception(message) {
	var range: TextRange? = null
	
	infix fun at(textRange: TextRange?): ParserException = this.apply { range = textRange }
}