package ir.mimlang.jmim.lang.lexer

import ir.mimlang.jmim.lang.token.*
import ir.mimlang.jmim.util.ext.builder
import ir.mimlang.jmim.util.ext.contains
import ir.mimlang.jmim.util.ext.toCharOrNull
import java.io.BufferedReader
import java.io.File

@Suppress("BlockingMethodInNonBlockingContext")
class Lexer {
	private val tokens: MutableList<Token> = mutableListOf()
	private val bufferedReader: BufferedReader
	private var current: Char? = null
	
	constructor(file: File) {
		bufferedReader = file.inputStream().bufferedReader()
	}
	
	constructor(str: String) {
		bufferedReader = str.byteInputStream().bufferedReader()
	}
	
	private val peeked: Char?
		get() = with(bufferedReader) {
			mark(1)
			val res = read()
			reset()
			return res.toCharOrNull()
		}
	
	private val next: Char?
		get() = bufferedReader.read().toCharOrNull().also { current = it }
	
	@Throws(LexerException::class)
	fun lex(): List<Token> {
		while (next != null) {
			when (current!!) {
				in ID_STARTER_CHAR -> tokens addIdentifier current!!.builder().apply {
					while (peeked in ID_SECONDARY_CHAR) append(next)
				}
				
				in NUMBER_CHAR -> tokens addNumber current!!.builder().apply {
					while (peeked in NUMBER_CHAR) append(next)
					if (peeked == '.') do append(next ?: throw LexerException("Reached EOF while lexing a Float")) while (peeked in NUMBER_CHAR)
				}
				
				in OP_CHAR -> tokens addOperator current!!.builder().apply {
					while (this operatorCanMergeWith peeked) append(next)
				}
				
				in STRING_CHAR -> tokens addString current!!.builder().apply {
					while (peeked != null && peeked !in STRING_CHAR) {
						append(next)
						if (current == '\\') append(next ?: throw LexerException("Reached EOF while lexing an escape sequence"))
					}
					append(next ?: throw LexerException("Found EOF instead of '\"' character"))
				}
				
				in PROPERTY_ACCESSOR_CHAR -> tokens.addPropertyAccessor()
				in EOS_CHAR -> tokens.addEndOfStatement()
				in LPR_CHAR -> tokens.addLeftParenthesis()
				in RPR_CHAR -> tokens.addRightParenthesis()
				
				!in IGNORED_CHAR -> throw LexerException("Invalid character '$current'")
			}
		}
		
		return tokens
	}
}