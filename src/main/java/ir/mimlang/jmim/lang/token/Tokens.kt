package ir.mimlang.jmim.lang.token

import ir.mimlang.jmim.lang.util.ext.equals
import ir.mimlang.jmim.lang.util.ext.or

typealias TType = Token.Type

val ID_STARTER_CHAR = Regex("[a-zA-Z_]")
val ID_SECONDARY_CHAR = Regex("[a-zA-Z0-9_]")
val NUMBER_CHAR = Regex("[0-9]")
val OP_CHAR = Regex("[-+*/%=!~|&<^>]")
val EOS_CHAR = Regex("[;]")
val PROPERTY_ACCESSOR_CHAR = Regex("[.]")
val SEPARATOR_CHAR = Regex("[,]")
val STRING_CHAR = Regex("[\"]")
val RAW_STRING_CHAR = Regex("[']")
val LPR_CHAR = Regex("[(]")
val RPR_CHAR = Regex("[)]")
val LBR_CHAR = Regex("[{]")
val RBR_CHAR = Regex("[}]")
val COMMENT_CHAR = Regex("[#]")
val IGNORED_CHAR = Regex("[\n\t ]")

val PRE_UN_OPS = listOf("!", "~", "-", "--", "++")
val POS_UN_OPS = listOf("--", "++")
val BIN_OPS = listOf(
	"=", "==", "!=",
	"&", "&&", "&=",
	"|", "||", "|=",
	">", ">=", "<", "<=",
	"+", "+=", "-", "-=",
	"*", "*=", "/", "/=",
	"%", "%=", "^", "^=",
	"<<", "<<=", ">>", ">>=", ">>>",
	"~="
)
val COMP_OPS = listOf(
	"==", "!=",
	"&&", "||",
	">", ">=", "<", "<="
)

infix fun MutableList<Token>.addIdentifier(builder: TokenBuilder) {
	add(Token(TType.ID, builder.value, builder.range))
}

infix fun MutableList<Token>.addNumber(builder: TokenBuilder) {
	val type = if ('.' in builder.value) TType.FLT else TType.INT
	add(Token(type, builder.value, builder.range))
}

infix fun MutableList<Token>.addOperator(builder: TokenBuilder) {
	add(Token(TType.OP, builder.value, builder.range))
}

infix fun MutableList<Token>.addString(builder: TokenBuilder) {
	add(Token(TType.STR, builder.value, builder.range))
}

infix fun MutableList<Token>.addRawString(builder: TokenBuilder) {
	add(Token(TType.RSTR, builder.value, builder.range))
}

infix fun MutableList<Token>.addEndOfStatement(builder: TokenBuilder) {
	add(Token(TType.EOS, builder.value, builder.range))
}

infix fun MutableList<Token>.addPropertyAccessor(builder: TokenBuilder) {
	add(Token(TType.PAC, builder.value, builder.range))
}

infix fun MutableList<Token>.addRangeBuilder(builder: TokenBuilder) {
	add(Token(TType.RNG, builder.value, builder.range))
}

infix fun MutableList<Token>.addSeparator(builder: TokenBuilder) {
	add(Token(TType.SEP, builder.value, builder.range))
}

infix fun MutableList<Token>.addLeftParenthesis(builder: TokenBuilder) {
	add(Token(TType.LPR, builder.value, builder.range))
}

infix fun MutableList<Token>.addRightParenthesis(builder: TokenBuilder) {
	add(Token(TType.RPR, builder.value, builder.range))
}

infix fun MutableList<Token>.addLeftBracket(builder: TokenBuilder) {
	add(Token(TType.LBR, builder.value, builder.range))
}

infix fun MutableList<Token>.addRightBracket(builder: TokenBuilder) {
	add(Token(TType.RBR, builder.value, builder.range))
}

infix fun MutableList<Token>.addComment(builder: TokenBuilder) {
	add(Token(TType.CMT, builder.value, builder.range))
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
	"<" -> other equals ('=' or '<')
	"^" -> other equals ('=')
	">" -> other equals ('=' or '>')
	">>" -> other equals ('=' or '>')
	"<<" -> other equals ('=')
	else -> false
}
