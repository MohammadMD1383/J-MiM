package ir.mimlang.jmim.lang.ctx

import ir.mimlang.jmim.lang.std.ValueVariable

open class Context(
	val parent: Context?,
	val name: String
) {
	val variables: MutableList<Variable> = mutableListOf()
	
	fun findVariable(name: String): Variable? {
		var ctx: Context? = this
		while (ctx != null) {
			ctx.variables.find { it.name == name }?.let { return it }
			ctx = ctx.parent
		}
		return null
	}
	
	fun addVariable(variable: Variable): Boolean {
		variables.find { it.name == variable.name }?.let { return false }
		variables.add(variable)
		return true
	}
	
	fun getParams(): List<Any?>? = findVariable("__params__")?.getValue() as? List<Any?>
	
	@Throws(Exception::class)
	fun unpackParams(names: List<String>) {
		val params = getParams() ?: throw UnsupportedOperationException("couldn't find variable __params__ to unpack")
		if (names.size != params.size) throw IllegalArgumentException("__params__ size differs from arguments size")
		names.forEachIndexed { i, name -> addVariable(ValueVariable(name, params[i])) }
	}
	
	override fun toString(): String = name
}