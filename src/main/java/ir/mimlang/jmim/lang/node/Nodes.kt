package ir.mimlang.jmim.lang.node

import ir.mimlang.jmim.lang.util.TextRange
import ir.mimlang.jmim.lang.util.ext.prepend

const val indent = "   "

interface Node {
	val range: TextRange
}

data class NumberNode(
	val value: Number,
	override val range: TextRange
) : Node {
	override fun toString(): String = "Number($value) at $range"
}

data class IdentifierNode(
	val name: String,
	override val range: TextRange
) : Node {
	override fun toString(): String = "Identifier($name) at $range"
}

data class StringNode(
	val string: String,
	val isRaw: Boolean,
	override val range: TextRange
) : Node {
	override fun toString(): String = "String($string) at $range" prepend if (isRaw) "Raw" else ""
}

data class ParenthesizedOperationNode(
	val node: Node,
	override val range: TextRange
) : Node {
	override fun toString(): String = "ParenthesizedOperation at $range:\n${node.toString().prependIndent(indent)}"
}

data class UnaryOperationNode(
	val operator: String,
	val operand: Node,
	val isPrefixed: Boolean,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val prefixed = if (isPrefixed) " (prefixed)" else ""
		val detail = "operator: $operator$prefixed\noperand: $operand"
		return "UnaryOperation at $range:\n${detail.prependIndent(indent)}"
	}
}

data class BinaryOperationNode(
	val lhs: Node,
	val operator: String,
	val rhs: Node,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "operator: $operator\nleft:\n${lhs.toString().prependIndent(indent)}\nright:\n${rhs.toString().prependIndent(indent)}"
		return "BinaryOperation at $range:\n${detail.prependIndent(indent)}"
	}
}

data class StatementNode(
	val node: Node,
	override val range: TextRange
) : Node {
	override fun toString(): String = "Statement at $range:\n${node.toString().prependIndent(indent)}"
}

data class VariableDeclarationNode(
	val name: String,
	val value: Node?,
	val isConst: Boolean,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val const = if (isConst) " (constant)" else ""
		val detail = "name: $name$const\nvalue:\n${value.toString().prependIndent(indent)}"
		return "VariableDeclaration at $range:\n${detail.prependIndent(indent)}"
	}
}

data class FunctionDeclarationNode(
	val name: String,
	val params: List<String>,
	val body: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "name: $name\nparameters: $params\nbody:\n${body.joinToString("\n").prependIndent(indent)}"
		return "FunctionDeclaration at $range:\n${detail.prependIndent(indent)}"
	}
}

data class FunctionCallNode(
	val name: String,
	val params: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "name: $name\npassed parameters:\n${params.joinToString("\n").prependIndent(indent)}"
		return "FunctionCall at $range:\n${detail.prependIndent(indent)}"
	}
}

data class MemberAccessNode(
	val name: String,
	val accessors: List<Pair<String, List<Node>?>>,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val access = accessors.joinToString("\n") {
			val params = "\nparameters passed:\n${it.second?.joinToString("\n")?.prependIndent(indent)}"
			".${it.first}${if (it.second != null) ":" + params.prependIndent(indent) else ""}"
		}
		val detail = "name: $name\naccess chain:\n${access.prependIndent(indent)}"
		return "MemberAccess at $range:\n${detail.prependIndent(indent)}"
	}
}

data class IfStatementNode(
	val ifBranch: Pair<Node, List<Node>>,
	val elifBranches: List<Pair<Node, List<Node>>>?,
	val elseBranch: List<Node>?,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val tmpElse = "body:\n${elseBranch?.joinToString("\n")?.prependIndent(indent) ?: "(empty)"}"
		val elseDetail = "else branch:\n${tmpElse.prependIndent(indent)}"
		
		val tmpElif = elifBranches?.joinToString("\n") {
			val elifContent =
				"condition:\n${it.first.toString().prependIndent(indent)}\nbody:\n${it.second.joinToString("\n").prependIndent(indent)}"
			"elif:\n${elifContent.prependIndent(indent)}"
		}
		val elifDetail = "elif branches:\n${tmpElif?.prependIndent(indent) ?: "(empty)"}"
		
		val tmpIf =
			"condition:\n${ifBranch.first.toString().prependIndent(indent)}\nbody:\n${ifBranch.second.joinToString("\n").prependIndent(indent)}"
		val ifDetail = "if branch:\n${tmpIf.prependIndent(indent)}"
		
		return "IfStatement at $range:\n${ifDetail.prependIndent(indent)}\n${elifDetail.prependIndent(indent)}\n${elseDetail.prependIndent(indent)}"
	}
}

data class RepeatLoopStatementNode(
	val times: Node,
	val varName: String?,
	val body: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "assigned to: ${varName ?: "(nothing)"}\nrepeat count:\n${times.toString().prependIndent(indent)}\nbody:\n${
			body.joinToString("\n").prependIndent(indent)
		}"
		return "RepeatLoop at $range:\n${detail.prependIndent(indent)}"
	}
}

data class WhileLoopStatementNode(
	val condition: Node,
	val body: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "condition:\n${condition.toString().prependIndent(indent)}\nbody:\n${body.joinToString("\n").prependIndent(indent)}"
		return "WhileLoop at $range:\n${detail.prependIndent(indent)}"
	}
}

data class DoWhileLoopStatementNode(
	val condition: Node,
	val body: BlockNode,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "condition:\n${condition.toString().prependIndent(indent)}\nbody:\n${body.nodes.joinToString("\n").prependIndent(indent)}"
		return "DoWhileLoop at $range:\n${detail.prependIndent(indent)}"
	}
}

data class ForLoopStatementNode(
	val key: String?,
	val value: String,
	val iterable: Node,
	val body: BlockNode,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val type = (if (key == null) "value" else "key-value") + " based"
		val detail =
			"type: $type\nassigned key: ${key ?: "(empty)"}\nassigned value: $value\niterable:\n${
				iterable.toString().prependIndent(indent)
			}\nbody:\n${body.nodes.joinToString("\n").prependIndent(indent)}"
		return "ForLoop at $range:\n${detail.prependIndent(indent)}"
	}
}

data class BlockNode(
	val nodes: List<Node>,
	override val range: TextRange
) : Node {
	override fun toString(): String = "CodeBlock at $range:\n${nodes.joinToString("\n").prependIndent(indent)}"
}

data class NamedBlockNode(
	val name: String,
	val body: BlockNode,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "name: $name\nbody:\n${body.nodes.joinToString("\n").prependIndent(indent)}"
		return "NamedBlock at $range:\n${detail.prependIndent(indent)}"
	}
}

data class WhenExpressionNode(
	val operand: ParenthesizedOperationNode,
	val comparator: String?,
	val cases: List<FullCaseNode>,
	val default: BlockNode?,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "operator: ${comparator ?: "(empty)"}\noperand:\n${operand.node.toString().prependIndent(indent)}\ncases:\n${
			cases.joinToString("\n").prependIndent(indent)
		}\ndefault:${default?.nodes?.joinToString("\n")?.prependIndent(indent)?.prepend("\n") ?: "(empty)"}"
		return "WhenExpression at $range:\n${detail.prependIndent(indent)}"
	}
}

data class CaseNode(
	val operator: String?,
	val operand: ParenthesizedOperationNode,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "operator: ${operator ?: "(empty)"}\noperand:\n${operand.node.toString().prependIndent(indent)}"
		return "Case at $range:\n${detail.prependIndent(indent)}"
	}
}

data class CaseAndGroupNode(
	val cases: List<CaseNode>,
	override val range: TextRange
) : Node {
	override fun toString(): String = "AndGroup at $range:\n${cases.joinToString("\n").prependIndent(indent)}"
}

data class CaseOrGroupNode(
	val groups: List<CaseAndGroupNode>,
	override val range: TextRange
) : Node {
	override fun toString(): String = "OrGroup at $range:\n${groups.joinToString("\n").prependIndent(indent)}"
}

data class FullCaseNode(
	val condition: CaseOrGroupNode,
	val body: BlockNode,
	override val range: TextRange
) : Node {
	override fun toString(): String {
		val detail = "condition:\n${condition.toString().prependIndent()}\nbody:\n${body.nodes.joinToString("\n").prependIndent(indent)}"
		return "FullCase at $range:\n${detail.prependIndent(indent)}"
	}
}

// todo migrate to use BlockNode
// todo make better use of range in toString()
// todo make use of nodes