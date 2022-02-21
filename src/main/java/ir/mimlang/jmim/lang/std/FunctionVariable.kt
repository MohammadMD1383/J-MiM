package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import ir.mimlang.jmim.lang.interpreter.Interpreter
import ir.mimlang.jmim.lang.node.Node
import ir.mimlang.jmim.lang.util.ext.then

class FunctionVariable(
	override val name: String,
	val params: List<String>,
	val body: List<Node>
) : Variable {
	override fun getValue(): FunctionVariable = this
	
	override fun invoke(context: Context): Any? {
		params.isNotEmpty() then { context.unpackParams(params) }
		val interpreter = Interpreter(context)
		val lastNode = body.lastOrNull() ?: return null
		
		for (i in 0 until body.lastIndex) {
			interpreter.interpret(body[i])
		}
		return interpreter.interpret(lastNode)
	}
}