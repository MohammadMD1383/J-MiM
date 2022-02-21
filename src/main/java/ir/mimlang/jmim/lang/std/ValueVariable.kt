package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable

class ValueVariable(
	override val name: String,
	private var value: Any? = null
) : Variable {
	override fun getValue(): Any? = value
	override fun setValue(value: Any?) {
		this.value = value
	}
	
	override fun decrement(returnBeforeJob: Boolean): Any? {
		val initialValue = value
		
		value = when (value) {
			is Long -> (value as Long).dec()
			is Double -> (value as Double).dec()
			else -> throw UnsupportedOperationException("operand of -- operator must be number")
		}
		
		return if (returnBeforeJob) initialValue else value
	}
	
	override fun increment(returnBeforeJob: Boolean): Any? {
		val initialValue = value
		
		value = when (value) {
			is Long -> (value as Long).inc()
			is Double -> (value as Double).inc()
			else -> throw UnsupportedOperationException("operand of ++ operator must be number")
		}
		
		return if (returnBeforeJob) initialValue else value
	}
	
	override fun invoke(context: Context): Any? = (value as? FunctionVariable)?.invoke(context) ?: super.invoke(context)
}