package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import ir.mimlang.jmim.lang.util.ext.then

class ValueVariable(
	override val name: String,
	private var value: Any? = null,
	private val isConst: Boolean = false
) : Variable {
	override fun getValue(): Any? = value
	override fun setValue(value: Any?) {
		isConst then { constThrow() }
		
		this.value = value
	}
	
	override fun decrement(returnBeforeJob: Boolean): Any? {
		isConst then { constThrow() }
		
		val initialValue = value
		
		value = when (value) {
			is Long -> (value as Long).dec()
			is Double -> (value as Double).dec()
			else -> throw UnsupportedOperationException("operand of -- operator must be number")
		}
		
		return if (returnBeforeJob) initialValue else value
	}
	
	override fun increment(returnBeforeJob: Boolean): Any? {
		isConst then { constThrow() }
		
		val initialValue = value
		
		value = when (value) {
			is Long -> (value as Long).inc()
			is Double -> (value as Double).inc()
			else -> throw UnsupportedOperationException("operand of ++ operator must be number")
		}
		
		return if (returnBeforeJob) initialValue else value
	}
	
	override fun getProperty(name: String): Any {
		when (name) {
			"size" -> {
				return (value as? Collection<*>)?.size
					?: throw UnsupportedOperationException("property 'size' exists only for lists")
			}
			
			else -> throw IllegalArgumentException("property $name doesn't exist on ${this.name}")
		}
	}
	
	override fun invoke(context: Context): Any? = (value as? FunctionVariable)?.invoke(context) ?: super.invoke(context)
	
	private fun constThrow(): Nothing = throw UnsupportedOperationException("cannot change the value of constant $name")
}