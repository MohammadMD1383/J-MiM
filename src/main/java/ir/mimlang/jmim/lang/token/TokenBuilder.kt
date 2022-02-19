package ir.mimlang.jmim.lang.token

import ir.mimlang.jmim.lang.util.Position
import ir.mimlang.jmim.lang.util.TextRange
import ir.mimlang.jmim.lang.util.wrapper.Column
import ir.mimlang.jmim.lang.util.wrapper.Line

data class TokenBuilder(
	val value: String,
	val range: TextRange
)

infix fun StringBuilder.locatedFrom(pair: Pair<Position, Position>): TokenBuilder = TokenBuilder(toString(), TextRange(pair.first, pair.second))
infix fun Char.locatedAt(position: Position): TokenBuilder = TokenBuilder(toString(), position.singleRange)
infix fun Column.at(line: Line): Position = Position(line.value, value)
