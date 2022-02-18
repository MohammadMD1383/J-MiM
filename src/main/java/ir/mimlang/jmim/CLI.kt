package ir.mimlang.jmim

import ir.mimlang.jmim.lang.lexer.Lexer
import ir.mimlang.jmim.lang.lexer.LexerException
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType

fun main(args: Array<String>) {
	val parser = ArgParser("mim")
	val filename by parser.argument(ArgType.String, "file-name", "file to run")
	
	parser.parse(args)
	// val file = File(filename)
	// if (!file.exists()) {
	// 	println("file $filename doesn't exist")
	// 	exitProcess(1)
	// }
	
	try {
		Lexer(
			"""
				var demo = 12.5;
			""".trimIndent()
		).lex().forEach(::println)
	} catch (e: LexerException) {
		println("error while lexing file: ${e.message}")
	}
}