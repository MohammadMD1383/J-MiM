package ir.mimlang.jmim.lang.util

data class TextRange(
	val start: Position,
	val end: Position
) {
	override fun toString(): String = "[$start..$end]"
}
