package ir.mimlang.jmim.lang.token

data class Token(
	val type: Type,
	val value: String
) {
	enum class Type(private val repr: String) {
		ID("Identifier"),
		INT("Integer"),
		FLT("Float"),
		OP("Operator"),
		STR("String Literal"),
		EOS("End of Statement"),
		PAC("Property Accessor"),
		LPR("Left Parenthesis"),
		RPR("Right Parenthesis");
		
		override fun toString(): String = repr
	}
	
	override fun toString(): String = "[$type: $value]"
}
