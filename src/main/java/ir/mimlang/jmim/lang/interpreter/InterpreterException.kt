package ir.mimlang.jmim.lang.interpreter

import ir.mimlang.jmim.lang.util.TextRange

class InterpreterException(message: String) : Exception(message) {
	lateinit var range: TextRange
	
	infix fun at(textRange: TextRange): InterpreterException = this.apply { range = textRange }
	
	override fun toString(): String = "$message at $range"
}