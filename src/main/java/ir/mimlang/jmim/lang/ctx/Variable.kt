package ir.mimlang.jmim.lang.ctx

import javax.naming.OperationNotSupportedException

interface Variable {
	val name: String
	
	fun increment(returnBeforeJob: Boolean): Any? = this.error("increment")
	fun decrement(returnBeforeJob: Boolean): Any? = this.error("decrement")
	
	fun getValue(): Any? = this.error("getValue")
	fun setValue(value: Any?): Unit = this.error("setValue")
	
	fun getProperty(name: String): Any? = this.error("getProperty")
	fun setProperty(name: String, value: Any?): Unit = this.error("setProperty")
	
	fun invoke(context: Context): Any? = this.error("invoke")
	fun invokeMember(name: String, context: Context): Any? = this.error("invokeMember")
	
	private fun error(method: String): Nothing = throw OperationNotSupportedException("$method is not supported on $name")
}