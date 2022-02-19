package ir.mimlang.jmim.lang.node

interface Node

data class NumberNode(val value: Number) : Node {
	override fun toString(): String = value.toString()
}

data class IdentifierNode(val name: String) : Node {
	override fun toString(): String = name
}

data class StringNode(val string: String) : Node {
	override fun toString(): String = string
}

data class ParenthesizedOperationNode(val node: Node) : Node {
	override fun toString(): String = "($node)"
}

data class BinaryOperationNode(
	val lhs: Node,
	val operator: String,
	val rhs: Node
) : Node {
	override fun toString(): String = "$lhs $operator $rhs"
}

data class StatementNode(val node: Node) : Node {
	override fun toString(): String = "$node;"
}

data class VariableDeclarationNode(
	val name: String,
	val value: Any?
) : Node {
	override fun toString(): String = "var $name${value?.let { " = $value" } ?: ""};"
}

data class FunctionDeclarationNode(
	val name: String,
	val params: List<String>,
	val body: List<Node>
) : Node {
	override fun toString(): String = "func $name(${params.joinToString()}) {\n${body.joinToString("\n")}\n}"
}

data class FunctionCallNode(
	val name: String,
	val params: List<Node>
) : Node {
	override fun toString(): String = "$name(${params.joinToString()})"
}
