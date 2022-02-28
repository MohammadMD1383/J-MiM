package ir.mimlang.jmim

import ir.mimlang.jmim.lang.interpreter.Interpreter
import ir.mimlang.jmim.lang.interpreter.InterpreterException
import ir.mimlang.jmim.lang.lexer.Lexer
import ir.mimlang.jmim.lang.lexer.LexerException
import ir.mimlang.jmim.lang.parser.Parser
import ir.mimlang.jmim.lang.parser.ParserException
import ir.mimlang.jmim.lang.std.Break
import ir.mimlang.jmim.lang.std.Continue
import ir.mimlang.jmim.lang.std.StdContext
import ir.mimlang.jmim.util.color
import ir.mimlang.jmim.util.ext.line
import ir.mimlang.jmim.util.ext.lines
import ir.mimlang.jmim.util.ext.times
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
			val a = list { run { 1 + 0; } 2; 3; };
			stdstream = a.size;
			stdstream++;
			stdstream = a;
		""".trimIndent() // todo add support for map
	
	// todo add support for list.get()
	// todo add support for map.get()
	
	try { // todo make better error reporting
		val tokens = Lexer(code).lex()
		val nodes = Parser(tokens).parse()
		val context = StdContext("<code>")
		val interpreter = Interpreter(context)
		nodes.forEach(interpreter::interpret)
	} catch (e: LexerException) {
		println(
			"""
				${color.red}error while lexing file: ${color.bold}${e.message}${color.normal}
				${color.bold}at ${e.position.line}:${e.position.column}:${color.normal}
				${code line e.position.line}
				${color.red.bold}${" " * e.position.column}^${color.normal}
			""".trimIndent()
		)
	} catch (e: ParserException) {
		val errMsg = e.message ?: "couldn't parse statement"
		println("${color.red}syntax error: $errMsg at ${e.range}${color.normal}")
		println(code lines e.range.start.line..e.range.end.line)
		println("------------------------------------------------------")
		println("stacktrace:")
		e.printStackTrace()
	} catch (e: InterpreterException) {
		println(
			"""
				${color.red}${e.message}${color.normal}
				${color.bold}at ${e.range}:${color.normal}
				${code lines e.range.start.line..e.range.end.line}
				-------------------------------------------------------
				stacktrace:
			""".trimIndent()
		)
		e.printStackTrace()
	}
}
