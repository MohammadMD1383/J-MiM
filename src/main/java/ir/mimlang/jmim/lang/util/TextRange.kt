package ir.mimlang.jmim.lang.util

data class TextRange(
	val start: Position,
	val end: Position
) {
	override fun toString(): String = if (start == end) start.toString() else "[$start..$end]"
}
