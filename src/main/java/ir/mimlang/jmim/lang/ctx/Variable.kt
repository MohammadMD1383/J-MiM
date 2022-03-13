package ir.mimlang.jmim.lang.ctx

import javax.naming.OperationNotSupportedException

interface Variable {
	val name: String
	
	@Throws(Exception::class)
	fun increment(returnBeforeJob: Boolean): Any? = this.error("increment")
	@Throws(Exception::class)
	fun decrement(returnBeforeJob: Boolean): Any? = this.error("decrement")
	
	@Throws(Exception::class)
	fun getValue(): Any? = this.error("getValue")
	@Throws(Exception::class)
	fun setValue(value: Any?): Unit = this.error("setValue")
	
	@Throws(Exception::class)
	fun getProperty(name: String): Any? = this.error("getProperty")
	@Throws(Exception::class)
	fun setProperty(name: String, value: Any?): Unit = this.error("setProperty")
	
	@Throws(Exception::class)
	fun invoke(context: Context): Any? = this.error("invoke")
	@Throws(Exception::class)
	fun invokeMember(name: String, context: Context): Any? = this.error("invokeMember")
	
	@Throws(Exception::class)
	private fun error(method: String): Nothing = throw OperationNotSupportedException("$method is not supported on $name")
}