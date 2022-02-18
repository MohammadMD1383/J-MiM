package ir.mimlang.jmim.lang.token

import ir.mimlang.jmim.util.ext.equals
import ir.mimlang.jmim.util.ext.or

typealias TType = Token.Type

val ID_STARTER_CHAR = Regex("[a-zA-Z_]")
val ID_SECONDARY_CHAR = Regex("[a-zA-Z0-9_]")
val NUMBER_CHAR = Regex("[0-9]")
val OP_CHAR = Regex("[-+*/%=!~|&<^>]")
val EOS_CHAR = Regex("[;]")
val PROPERTY_ACCESSOR_CHAR = Regex("[.]")
val STRING_CHAR = Regex("[\"]")
val LPR_CHAR = Regex("[(]")
val RPR_CHAR = Regex("[)]")
val IGNORED_CHAR = Regex("[\n\t ]")

infix fun MutableList<Token>.addIdentifier(value: StringBuilder) {
	add(Token(TType.ID, value.toString()))
}

infix fun MutableList<Token>.addNumber(value: StringBuilder) {
	add(Token(if ('.' in value) TType.FLT else TType.INT, value.toString()))
}

infix fun MutableList<Token>.addOperator(value: StringBuilder) {
	add(Token(TType.OP, value.toString()))
}

infix fun MutableList<Token>.addString(value: StringBuilder) {
	add(Token(TType.STR, value.toString()))
}

infix fun MutableList<Token>.addEndOfStatement(value: StringBuilder) {
	add(Token(TType.EOS, value.toString()))
}

infix fun MutableList<Token>.addPropertyAccessor(value: StringBuilder) {
	add(Token(TType.PAC, value.toString()))
}

infix fun MutableList<Token>.addLeftParenthesis(value: StringBuilder) {
	add(Token(TType.LPR, value.toString()))
}

infix fun MutableList<Token>.addRightParenthesis(value: StringBuilder) {
	add(Token(TType.RPR, value.toString()))
}

infix fun StringBuilder.operatorCanMergeWith(other: Char?): Boolean = when (this.toString()) {
	"-" -> other equals ('-' or '=')
	"+" -> other equals ('+' or '=')
	"*" -> other equals ('=')
	"/" -> other equals ('=')
	"%" -> other equals ('=')
	"=" -> other equals ('=')
	"!" -> other equals ('=')
	"~" -> other equals ('=')
	"|" -> other equals ('|' or '=')
	"&" -> other equals ('&' or '=')
	"<" -> other equals ('=')
	"^" -> other equals ('=')
	">" -> other equals ('=')
	else -> false
}
