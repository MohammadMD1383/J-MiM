package ir.mimlang.jmim.lang.token

import ir.mimlang.jmim.lang.util.TextRange

data class Token(
	val type: Type,
	val value: String,
	val range: TextRange
) {
	enum class Type(private val repr: String) {
		ID("Identifier"),
		INT("Integer"),
		FLT("Float"),
		OP("Operator"),
		STR("String Literal"),
		RSTR("Raw String"),
		EOS("End of Statement"),
		PAC("Property Accessor"),
		SEP("Separator"),
		LPR("Left Parenthesis"),
		RPR("Right Parenthesis"),
		LBR("Left Bracket"),
		RBR("Right Bracket"),
		CMT("Comment");
		
		override fun toString(): String = repr
	}
	
	override fun toString(): String = "[$type: $value]"
}
