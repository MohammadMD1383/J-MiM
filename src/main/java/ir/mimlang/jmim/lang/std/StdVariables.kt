package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable

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