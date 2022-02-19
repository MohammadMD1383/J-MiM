package ir.mimlang.jmim.util

class ConsoleColor {
	private val colors: MutableList<Byte> = mutableListOf()
	
	// @formatter:off
	val normal: ConsoleColor get() = this.also { colors.add(0) }
	val bold:   ConsoleColor get() = this.also { colors.add(1) }
	
	val red:    ConsoleColor get() = this.also { colors.add(31) }
	// @formatter:on
	
	override fun toString(): String = "\u001b[${colors.joinToString(";")}m"
}

val color: ConsoleColor get() = ConsoleColor()
