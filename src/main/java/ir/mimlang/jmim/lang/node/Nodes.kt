package ir.mimlang.jmim.lang.node

import ir.mimlang.jmim.lang.util.TextRange
import ir.mimlang.jmim.lang.util.ext.prepend

interface Node {
	val range: TextRange
}

data class NumberNode(
	val value: Number,
	override val range: TextRange
) : Node {
	override fun toString(): String = value.toString()
}

data class IdentifierNode(
	val name: String,
	override val range: TextRange
) : Node {
	override fun toString(): String = name
}

data class StringNode(
	val string: String,
	val isRaw: Boolean,
	override val range: TextRange
) : Node {
	override fun toString(): String = "\"$string\""
}

data class ParenthesizedOperationNode(
	val node: Node,
	override val range: TextRange
) : Node {
	override fun toString(): String = "($node)"
}

data class UnaryOperationNode(
	val operator: String,
	val operand: Node,
	val isPrefixed: Boolean,
	override val range: TextRange
) : Node {
	override fun toString(): String = if (isPrefixed) "$operator$operand" else "$operand$operator"
}

data class BinaryOperationNode(
	val lhs: Node,
	val operator: String,
	val rhs: Node,
	override val range: TextRange
) : Node {
	override fun toString(): String = "$lhs $operator $rhs"
}

data class StatementNode(
	val node: Node,
	override val range: TextRange
) : Node {
	override fun toString(): String = "$node;"
}

data class VariableDeclarationNode(
	val name: String,
	val value: Node?,
	override val range: TextRange
) : Node {
	override fun toString(): String = "var $name${value?.let { " = $value" } ?: ""};" //todo rewrite
}

data class FunctionDeclarationNode(
	val name: String,
	val params: List<String>,
	val body: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String = "func $name${
		params.run {
			if (isNotEmpty()) "(${joinToString()})" else ""
		}
	} {\n${body.joinToString("\n").prependIndent("\t")}\n}" // todo rewrite...
}

data class FunctionCallNode(
	val name: String,
	val params: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String = "$name(${params.joinToString()})"
}

data class MemberAccessNode(
	val name: String,
	val member: String,
	val params: List<Node>?,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val invocation = params?.let { "(${params.joinToString()})" } ?: ""
		return "$name.$member$invocation"
	}
}

data class IfStatementNode(
	val ifBranch: Pair<Node, List<Node>>,
	val elifBranches: List<Pair<Node, List<Node>>>?,
	val elseBranch: List<Node>?,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val tmpIf = "if (${ifBranch.first}) {\n${ifBranch.second.joinToString("\n").prependIndent("\t")}\n}"
		val tmpElif = elifBranches?.joinToString(" ") {
			"elif (${it.first}) {\n${it.second.joinToString("\n").prependIndent("\t")}\n}"
		} ?: ""
		val tmpElse = elseBranch?.let { "else {\n${it.joinToString("\n").prependIndent("\t")}\n}" } ?: ""
		
		return "$tmpIf $tmpElif $tmpElse"
	}
}

data class RepeatLoopStatement(
	val times: Node,
	val varName: String?,
	val body: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val tmpVarName = varName?.prepend(" as ") ?: ""
		return "repeat $times$tmpVarName {\n${body.joinToString("\n").prependIndent("\t")}\n}"
	}
}

data class WhileLoopStatement(
	val condition: Node,
	val body: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String = "while ($condition) {\n${body.joinToString("\n").prependIndent("\t")}\n}"
}
