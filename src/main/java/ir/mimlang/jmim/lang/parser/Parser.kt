package ir.mimlang.jmim.lang.parser

import ir.mimlang.jmim.lang.node.*
import ir.mimlang.jmim.lang.token.TType
import ir.mimlang.jmim.lang.token.Token

class Parser(private val tokens: List<Token>) {
	private val nodes: MutableList<Node> = mutableListOf()
	
	private var tokenIndex = -1
	private val savedIndexes: MutableList<Int> = mutableListOf()
	private val currentToken: Token? get() = tokens.getOrNull(tokenIndex)
	private val peekedToken: Token? get() = tokens.getOrNull(tokenIndex + 1)
	private val nextToken: Token? get() = tokens.getOrNull(++tokenIndex)
	
	private fun save() {
		savedIndexes.add(tokenIndex)
	}
	
	private fun restore() {
		tokenIndex = savedIndexes.last()
		savedIndexes.removeLast()
	}
	
	private fun removeSaved() {
		savedIndexes.removeLast()
	}
	
	fun parse(): List<Node> = nodes.apply {
		while (peekedToken != null) add(
			expectFunctionDeclaration()
				?: expectVariableDeclaration()
				?: expectStatement()
				?: expectBinaryOperation()
				?: expectParenthesizedOperation()
				?: throw ParserException("couldn't parse...")
		)
	}
	
	private fun expectFunctionDeclaration(): FunctionDeclarationNode? {
	
	}
	
	private fun expectVariableDeclaration(): VariableDeclarationNode? {
		save()
		
		expectKeyword("var") ?: run { restore(); return null }
		val name = expectIdentifier() ?: run { restore(); return null }
		val value = expectOperator("=")?.run {
			expectNumber()
		}
		expectEndOfStatement() ?: run { restore(); return null }
		
		removeSaved()
		return VariableDeclarationNode(name.name, value)
	}
	
	private fun expectStatement(): StatementNode? {
		save()
		
		val node = expectBinaryOperation() ?: run { restore(); return null }
		expectEndOfStatement() ?: run { restore(); return null }
		
		removeSaved()
		return StatementNode(node)
	}
	
	private fun expectBinaryOperation(): BinaryOperationNode? {
		save()
		
		val lhs = expectParenthesizedOperation()
			?: expectIdentifier()
			?: expectNumber()
			?: run { restore(); return null }
		
		val op = expectOperator()
			?: run { restore(); return null }
		
		val rhs = expectBinaryOperation()
			?: expectParenthesizedOperation()
			?: expectIdentifier()
			?: expectNumber()
			?: run { restore(); return null }
		
		removeSaved()
		return BinaryOperationNode(lhs, op, rhs)
	}
	
	private fun expectParenthesizedOperation(): ParenthesizedOperationNode? {
		save()
		
		expectLeftParenthesis()
			?: run { restore(); return null }
		
		val node = expectBinaryOperation()
			?: expectParenthesizedOperation()
			?: run { restore(); return null }
		
		expectRightParenthesis()
			?: run { restore(); return null }
		
		removeSaved()
		return ParenthesizedOperationNode(node)
	}
	
	private fun expectKeyword(kw: String): Token? = if (peekedToken?.type == TType.ID && peekedToken!!.value == kw) nextToken else null
	
	private fun expectOperator(op: String): Token? = if (peekedToken?.type == TType.OP && peekedToken!!.value == op) nextToken else null
	
	private fun expectLeftParenthesis(): Token? {
		return when (peekedToken?.type) {
			TType.LPR -> nextToken
			else -> null
		}
	}
	
	private fun expectRightParenthesis(): Token? {
		return when (peekedToken?.type) {
			TType.RPR -> nextToken
			else -> null
		}
	}
	
	private fun expectOperator(): String? {
		return when (peekedToken?.type) {
			TType.OP -> nextToken!!.value
			else -> null
		}
	}
	
	private fun expectNumber(): NumberNode? {
		return when (peekedToken?.type) {
			TType.INT -> nextToken!!.value.toLong().node
			TType.FLT -> nextToken!!.value.toDouble().node
			else -> null
		}
	}
	
	private fun expectIdentifier(): IdentifierNode? {
		return when (peekedToken?.type) {
			TType.ID -> nextToken!!.value.node
			else -> null
		}
	}
	
	private fun expectEndOfStatement(): Token? {
		return when (peekedToken?.type) {
			TType.EOS -> nextToken
			else -> null
		}
	}
}