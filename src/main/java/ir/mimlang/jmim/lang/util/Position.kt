package ir.mimlang.jmim.lang.util

data class Position(
	val line: Int,
	val column: Int
) {
	override fun toString(): String = "$line:$column"
	
	val singleRange: TextRange get() = TextRange(this, this)
	
	operator fun rangeTo(position: Position): TextRange = TextRange(this, position)
}