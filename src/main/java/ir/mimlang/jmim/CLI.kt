package ir.mimlang.jmim

import ir.mimlang.jmim.lang.lexer.Lexer
import ir.mimlang.jmim.lang.lexer.LexerException
import ir.mimlang.jmim.lang.parser.Parser
import ir.mimlang.jmim.util.color
import ir.mimlang.jmim.util.ext.line
import ir.mimlang.jmim.util.ext.times
import ir.mimlang.jmim.util.printTree
import ir.mimlang.jmim.util.visual
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
	
	val code =
		"""
			stdstream += "Hello World";
		""".trimIndent()
	
	try {
		val tokens = Lexer(code).lex()
		val nodes = Parser(tokens).parse()
		nodes.forEach { printTree(it.visual) }
	} catch (e: LexerException) {
		println(
			"""
				${color.red}error while lexing file: ${color.bold}${e.message}${color.normal}
				${color.bold}at ${e.position.line}:${e.position.column}:${color.normal}
				${code line e.position.line}
				${color.red.bold}${" " * e.position.column}^${color.normal}
			""".trimIndent()
		)
	}
}
