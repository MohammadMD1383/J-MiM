package ir.mimlang.jmim

import ir.mimlang.jmim.lang.interpreter.Interpreter
import ir.mimlang.jmim.lang.interpreter.InterpreterException
import ir.mimlang.jmim.lang.lexer.Lexer
import ir.mimlang.jmim.lang.lexer.LexerException
import ir.mimlang.jmim.lang.parser.Parser
import ir.mimlang.jmim.lang.parser.ParserException
import ir.mimlang.jmim.lang.std.StdContext
import ir.mimlang.jmim.util.color
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import kotlin.system.exitProcess


fun main(args: Array<String>) {
	val cliParser = ArgParser("mim")
	val filename by cliParser.argument(ArgType.String, "file-name", "file to run")
	val lexOnly by cliParser.option(ArgType.Boolean, "lex-only", "l", "outputs lexer result").default(false)
	val parseOnly by cliParser.option(ArgType.Boolean, "parse-only", "p", "outputs parser result").default(false)
	cliParser.parse(args)
	
	val file = File(filename)
	if (!file.exists() || !file.isFile) {
		println("file $filename doesn't exist")
		exitProcess(1)
	}
	
	// todo: add methods for insert/delete/get/replace in (map and list and string)
	
	try { // todo make better error reporting
		val tokens = Lexer(file).lex()
		if (lexOnly) {
			tokens.forEach(::println)
			exitProcess(0)
		}
		
		val nodes = Parser(tokens).parse()
		if (parseOnly) {
			nodes.forEach(::println)
			exitProcess(0)
		}
		
		val context = StdContext("<code>")
		val interpreter = Interpreter(context)
		nodes.forEach(interpreter::interpret)
	} catch (e: LexerException) {
		println("${color.red}Lexer Error: ${e.message}")
		println("at ${e.position}${color.normal}")
		
		// println(
		// 	"""
		// 		${color.red}error while lexing file: ${color.bold}${e.message}${color.normal}
		// 		${color.bold}at ${e.position.line}:${e.position.column}:${color.normal}
		// 		${code line e.position.line}
		// 		${color.red.bold}${" " * e.position.column}^${color.normal}
		// 	""".trimIndent()
		// )
	} catch (e: ParserException) {
		val errMsg = e.message ?: "couldn't parse statement"
		println("${color.red}Parser Error: $errMsg")
		println("at ${e.range}${color.normal}")
		
		// println("${color.red}syntax error: $errMsg at ${e.range}${color.normal}")
		// println(code lines e.range.start.line..e.range.end.line)
		// println("------------------------------------------------------")
		// println("stacktrace:")
		// e.printStackTrace()
	} catch (e: InterpreterException) {
		println("${color.red}Runtime Error: ${e.message}")
		println("at ${e.range}${color.normal}")
		
		// println(
		// 	"""
		// 		${color.red}${e.message}${color.normal}
		// 		${color.bold}at ${e.range}:${color.normal}
		// 		${code lines e.range.start.line..e.range.end.line}
		// 		-------------------------------------------------------
		// 		stacktrace:
		// 	""".trimIndent()
		// )
		// e.printStackTrace()
	}
}
