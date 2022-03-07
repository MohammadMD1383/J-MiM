package ir.mimlang.jmim.lang.util

data class Position(
	val line: Int,
	val column: Int
) {
	override fun toString(): String = "$line:$column"
	
	override fun equals(other: Any?): Boolean {
		return other is Position
			&& this.line == other.line
			&& this.column == other.column
	}
	
	override fun hashCode(): Int = 31 * line + column
	
	val singleRange: TextRange get() = TextRange(this, this)
	operator fun rangeTo(position: Position): TextRange = TextRange(this, position)
}