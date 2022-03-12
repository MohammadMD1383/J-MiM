package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import ir.mimlang.jmim.lang.util.ext.then

class ValueVariable(
	override val name: String,
	private var value: Any? = null,
	private val isConst: Boolean = false
) : Variable {
	init {
		if (value is ValueVariable) throw IllegalArgumentException("ValueVariable cannot hold child of same type")
	}
	
	override fun getValue(): Any? {
		return if (value is Variable) (value as Variable).getValue() else value
	}
	
	override fun setValue(value: Any?) {
		if (this.value is Variable) return (this.value as Variable).setValue(value)
		
		isConst then { constThrow() }
		this.value = value
	}
	
	override fun decrement(returnBeforeJob: Boolean): Any? {
		if (value is Variable) return (value as Variable).decrement(returnBeforeJob)
		
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
		if (value is Variable) return (value as Variable).increment(returnBeforeJob)
		
		isConst then { constThrow() }
		val initialValue = value
		value = when (value) {
			is Long -> (value as Long).inc()
			is Double -> (value as Double).inc()
			else -> throw UnsupportedOperationException("operand of ++ operator must be number")
		}
		return if (returnBeforeJob) initialValue else value
	}
	
	override fun getProperty(name: String): Any? {
		if (value is Variable) return (value as Variable).getProperty(name)
		
		when (name) {
			"size" -> {
				return (value as? List<*>)?.size
					?: throw UnsupportedOperationException("property 'size' exists only for lists")
			}
			
			else -> throw IllegalArgumentException("property $name doesn't exist on ${this.name}")
		}
	}
	
	override fun setProperty(name: String, value: Any?) =
		if (this.value is Variable) (this.value as Variable).setProperty(name, value) else super.setProperty(name, value)
	
	override fun invoke(context: Context): Any? = if (value is Variable) (value as Variable).invoke(context) else super.invoke(context)
	
	override fun invokeMember(name: String, context: Context): Any? {
		if (value is Variable) return (value as Variable).invokeMember(name, context)
		// fixme: if the user wants to invoke the value not invoke member ...
		
		return when (name) {
			"invoke" -> invoke(context)
			
			"get" -> {
				val getParam = context.getParams()!!.single() // todo make better
				
				when (value) {
					is List<*> -> (value as List<*>)[(getParam as Long).toInt()]
					is Map<*, *> -> (value as Map<*, *>)[getParam as String]
					is String -> (value as String)[(getParam as Long).toInt()]
					
					else -> throw UnsupportedOperationException("method get exists only for lists, maps and strings")
				}
			}
			
			else -> throw IllegalArgumentException("method $name doesn't exist on ${this.name}")
		}
	}
	
	private fun constThrow(): Nothing = throw UnsupportedOperationException("cannot change the value of constant $name")
}