package ir.mimlang.jmim.lang.util

data class Position(
	val line: Int,
	val column: Int
) {
	val singleRange: TextRange
		get() = TextRange(this, this)
}