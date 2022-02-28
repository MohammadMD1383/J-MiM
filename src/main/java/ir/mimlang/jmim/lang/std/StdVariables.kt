package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import kotlin.random.Random.Default.nextLong
import kotlin.system.exitProcess

val StdStream = object : Variable {
	override val name: String get() = "stdstream"
	
	private var end = ""
	private var sep = " "
	
	override fun increment(returnBeforeJob: Boolean): Unit = println()
	
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

val Null = object : Variable {
	override val name: String get() = "null"
	override fun getValue(): Any? = null
}

val True = object : Variable {
	override val name: String get() = "true"
	override fun getValue(): Boolean = true
}

val False = object : Variable {
	override val name: String get() = "false"
	override fun getValue(): Boolean = false
}

val Exit = object : Variable {
	override val name: String get() = "exit"
	
	override fun getValue(): Nothing = exitProcess(0)
	override fun invoke(context: Context): Any? {
		val params = context.getParams()
			?: throw Exception("params not found")
		
		if (params.size != 1)
			throw Exception("exit only accepts one integer parameter")
		
		exitProcess((params[0] as Long).toInt())
	}
}

val Unpack = object : Variable {
	override val name: String get() = "unpack"
	
	override fun invoke(context: Context): Any? {
		val params = context.getParams()
			?: throw Exception("params not found")
		
		context.parent!!.unpackParams(params.map { it as String })
		return null
	}
}

val BreakStatement = object : Variable {
	override val name: String get() = "break"
	
	override fun getValue(): Nothing = throw Break(0)
	override fun invoke(context: Context): Nothing {
		val params = context.getParams()
			?: throw Exception("params not found")
		
		if (params.size != 1)
			throw Exception("break only accepts one integer parameter")
		
		throw Break(params[0] as Long)
	}
}

val ContinueStatement = object : Variable {
	override val name: String get() = "continue"
	
	override fun getValue(): Nothing = throw Continue(0)
	override fun invoke(context: Context): Nothing {
		val params = context.getParams()
			?: throw Exception("params not found")
		
		if (params.size != 1)
			throw Exception("continue only accepts one integer parameter")
		
		throw Continue(params[0] as Long)
	}
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
