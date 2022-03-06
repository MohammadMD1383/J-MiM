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
	val isConst: Boolean,
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
	val accessors: List<Pair<String, List<Node>?>>,
	override val range: TextRange
) : Node // todo toString()

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

data class RepeatLoopStatementNode(
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

data class WhileLoopStatementNode(
	val condition: Node,
	val body: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String = "while ($condition) {\n${body.joinToString("\n").prependIndent("\t")}\n}"
}

data class ForLoopStatementNode(
	val key: String?,
	val value: String,
	val iterable: Node,
	val body: BlockNode,
	override val range: TextRange
) : Node

data class BlockNode(
	val nodes: List<Node>,
	override val range: TextRange
) : Node // todo

data class NamedBlockNode(
	val name: String,
	val body: BlockNode,
	override val range: TextRange
) : Node {
	override fun toString(): String = "$name {\n${body.nodes.joinToString("\n").prependIndent("\t")}\n}"
}

data class WhenExpressionNode(
	val operand: ParenthesizedOperationNode,
	val comparator: String?,
	val cases: List<FullCaseNode>,
	val default: BlockNode?,
	override val range: TextRange
) : Node // todo

data class CaseNode(
	val operator: String?,
	val operand: ParenthesizedOperationNode,
	override val range: TextRange
) : Node // todo toString

data class CaseAndGroupNode(
	val cases: List<CaseNode>,
	override val range: TextRange
) : Node // todo

data class CaseOrGroupNode(
	val groups: List<CaseAndGroupNode>,
	override val range: TextRange
) : Node // todo

data class FullCaseNode(
	val condition: CaseOrGroupNode,
	val body: BlockNode,
	override val range: TextRange
) : Node // todo

// todo migrate to use BlockNode