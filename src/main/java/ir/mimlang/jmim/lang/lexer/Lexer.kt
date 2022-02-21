package ir.mimlang.jmim.lang.lexer

import ir.mimlang.jmim.lang.token.*
import ir.mimlang.jmim.lang.util.Position
import ir.mimlang.jmim.lang.util.ext.builder
import ir.mimlang.jmim.lang.util.ext.contains
import ir.mimlang.jmim.lang.util.ext.toCharOrNull
import ir.mimlang.jmim.lang.util.wrapper.column
import ir.mimlang.jmim.lang.util.wrapper.line
import java.io.BufferedReader
import java.io.File
import java.io.IOException

@Suppress("BlockingMethodInNonBlockingContext")
class Lexer {
	private val tokens: MutableList<Token> = mutableListOf()
	private val bufferedReader: BufferedReader
	
	constructor(file: File) {
		bufferedReader = file.inputStream().bufferedReader()
	}
	
	constructor(str: String) {
		bufferedReader = str.byteInputStream().bufferedReader()
	}
	
	private var line = 1.line
	private var column = 0.column
	private val currentPosition: Position get() = column at line
	
	private var currentChar: Char? = null
	private val peekedChar: Char?
		get() = with(bufferedReader) {
			mark(1)
			val res = read()
			reset()
			return res.toCharOrNull()
		}
	private val nextChar: Char?
		get() = bufferedReader.read().toCharOrNull().also {
			currentChar = it
			when {
				it == '\n' -> {
					line++
					column = 0.column
				}
				it != null -> column++
			}
		}
	
	@Throws(
		LexerException::class,
		IOException::class
	)
	fun lex(): List<Token> {
		while (nextChar != null) {
			val startingPosition = column at line
			
			when (currentChar!!) {
				in ID_STARTER_CHAR -> tokens addIdentifier currentChar!!.builder().apply {
					while (peekedChar in ID_SECONDARY_CHAR) append(nextChar)
				}.locatedFrom(startingPosition to currentPosition)
				
				in NUMBER_CHAR -> tokens addNumber currentChar!!.builder().apply {
					while (peekedChar in NUMBER_CHAR) append(nextChar)
					
					if (peekedChar == '.') do append(
						nextChar ?: throw LexerException("Reached EOF while lexing a Float") at currentPosition
					) while (peekedChar in NUMBER_CHAR)
				}.locatedFrom(startingPosition to currentPosition)
				
				in OP_CHAR -> tokens addOperator currentChar!!.builder().apply {
					while (this operatorCanMergeWith peekedChar) append(nextChar)
				}.locatedFrom(startingPosition to currentPosition)
				
				in STRING_CHAR -> tokens addString currentChar!!.builder().apply {
					while (peekedChar != null && peekedChar !in STRING_CHAR) {
						append(nextChar)
						
						if (currentChar == '\\') append(
							nextChar ?: throw LexerException("Reached EOF while lexing an escape sequence") at currentPosition
						)
					}
					append(nextChar ?: throw LexerException("Found EOF instead of '\"' character") at currentPosition)
				}.locatedFrom(startingPosition to currentPosition)
				
				in RAW_STRING_CHAR -> tokens addRawString currentChar!!.builder().apply {
					while (peekedChar != null && peekedChar !in RAW_STRING_CHAR) {
						append(nextChar)
					}
					append(nextChar ?: throw LexerException("Found EOF instead of ' character") at currentPosition)
				}.locatedFrom(startingPosition to currentPosition)
				
				in COMMENT_CHAR -> tokens addComment currentChar!!.builder().apply {
					while (peekedChar != null && peekedChar != '\n' && peekedChar !in COMMENT_CHAR) append(nextChar)
					if (peekedChar in COMMENT_CHAR) append(nextChar)
				}.locatedFrom(startingPosition to currentPosition)
				
				in PROPERTY_ACCESSOR_CHAR -> tokens addPropertyAccessor currentChar!!.locatedAt(currentPosition)
				in SEPARATOR_CHAR -> tokens addSeparator currentChar!!.locatedAt(currentPosition)
				in EOS_CHAR -> tokens addEndOfStatement currentChar!!.locatedAt(currentPosition)
				in LPR_CHAR -> tokens addLeftParenthesis currentChar!!.locatedAt(currentPosition)
				in RPR_CHAR -> tokens addRightParenthesis currentChar!!.locatedAt(currentPosition)
				in LBR_CHAR -> tokens addLeftBracket currentChar!!.locatedAt(currentPosition)
				in RBR_CHAR -> tokens addRightBracket currentChar!!.locatedAt(currentPosition)
				
				!in IGNORED_CHAR -> throw LexerException("Invalid character '$currentChar'") at currentPosition
			}
		}
		
		bufferedReader.close()
		return tokens
	}
}