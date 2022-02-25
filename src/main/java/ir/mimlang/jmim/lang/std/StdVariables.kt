package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import kotlin.random.Random.Default.nextLong

val StdStream = object : Variable {
	override val name: String get() = "stdstream"
	
	private var end = ""
	private var sep = " "
	
	override fun getValue(): String = readln()
	override fun setValue(value: Any?) = print("$value$end")
	
	override fun getProperty(name: String): String = when (name) {
		"end" -> end
		"sep" -> sep
		else -> throw Exception("no member named $name")
	}
	
	override fun setProperty(name: String, value: Any?) {
		when (name) {
			"end" -> end = value.toString()
			"sep" -> sep = value.toString()
			else -> throw Exception("no member named $name")
		}
	}
	
	override fun invoke(context: Context): String {
		val params = context.getParams()
		print("${params?.joinToString(sep) ?: ""}$end")
		return readln()
	}
}

val True = object : Variable {
	override val name: String get() = "true"
	override fun getValue(): Boolean = true
}

val False = object : Variable {
	override val name: String get() = "false"
	override fun getValue(): Boolean = false
}

val IntValueOf = object : Variable {
	override val name: String get() = "int"
	
	override fun invoke(context: Context): Long {
		val params = context.getParams()
			?: throw Exception("params not found")
		
		if (params.size == 1) return (params[0] as String).toLong()
		else throw Exception("integer function only accepts one parameter")
	}
}

val StringValueOf = object : Variable {
	override val name: String get() = "str"
	
	override fun invoke(context: Context): String {
		val params = context.getParams()
			?: throw Exception("params not found")
		
		if (params.size == 1) return params[0].toString()
		else throw Exception("integer function only accepts one parameter")
	}
}

val Random = object : Variable {
	override val name: String get() = "random"
	
	override fun getValue(): Long = nextLong()
	override fun invoke(context: Context): Long {
		val params = context.getParams() ?: throw Exception("params not found")
		return when (params.size) {
			0 -> nextLong()
			1 -> nextLong(params[0] as Long)
			2 -> nextLong(params[0] as Long, params[1] as Long)
			else -> throw Exception("random function only accepts 1 or 2 parameters")
		}
	}
}
