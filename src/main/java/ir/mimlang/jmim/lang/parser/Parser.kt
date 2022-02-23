package ir.mimlang.jmim.lang.parser

import ir.mimlang.jmim.lang.node.*
import ir.mimlang.jmim.lang.token.*
import ir.mimlang.jmim.lang.util.Position
import ir.mimlang.jmim.lang.util.TextRange

class Parser(tokens: List<Token>) {
	private val tokens: List<Token>
	private val nodes: MutableList<Node> = mutableListOf()
	
	init {
		this.tokens = tokens.filterNot { it.type == TType.CMT }
	}
	
	private var tokenIndex = -1
	private val savedIndexes: MutableList<Int> = mutableListOf()
	private val currentToken: Token? get() = tokens.getOrNull(tokenIndex)
	private val peekedToken: Token? get() = tokens.getOrNull(tokenIndex + 1)
	private val nextToken: Token? get() = tokens.getOrNull(++tokenIndex)
	
	private var processedRange = 0 to 0
	
	private fun save() {
		savedIndexes.add(tokenIndex)
		
		if (tokenIndex < processedRange.first && tokenIndex > 0)
			processedRange = processedRange.copy(first = tokenIndex)
	}
	
	private fun restore() {
		if (tokenIndex > processedRange.second)
			processedRange = processedRange.copy(second = tokenIndex)
		
		tokenIndex = savedIndexes.last()
		savedIndexes.removeLast()
	}
	
	private fun removeSaved() {
		if (tokenIndex > processedRange.second)
			processedRange = processedRange.copy(second = tokenIndex)
		
		savedIndexes.removeLast()
	}
	
	fun parse(): List<Node> = nodes.apply {
		while (peekedToken != null) {
			add(
				expectRoot()
					?: throw ParserException() at tokens[processedRange.first].range.start..tokens[processedRange.second].range.end
			)
			processedRange = 0 to 0
		}
	}
	
	private fun expectRoot() = (
		expectIfStatement()
			?: expectFunctionDeclaration()
			?: expectVariableDeclaration()
			?: expectStatement()
		)
	
	private fun expectIfStatement(): IfStatementNode? {
		save()
		
		/* if (...) {...} */
		/*val ifKw =*/ expectKeyword("if") ?: run { restore(); return null }
		val ifCond = expectParenthesizedOperation()?.node ?: run { restore(); return null }
		expectLeftBracket() ?: run { restore(); return null }
		val ifBody = mutableListOf<Node>().apply {
			while (true) add(expectRoot() ?: break)
		}
		/*val ifEndBracket =*/ expectRightBracket() ?: run { restore(); return null }
		
		/* elif (...) {...} */
		var elifStatements: MutableList<Pair<Node, List<Node>>>? = mutableListOf()
		while (true) {
			save()
			
			if (expectKeyword("elif") == null) {
				restore()
				break
			}
			
			val elifCond = expectParenthesizedOperation()?.node
			if (elifCond == null) {
				restore()
				break
			}
			
			if (expectLeftBracket() == null) {
				restore()
				break
			}
			
			val elifBody = mutableListOf<Node>().apply {
				while (true) add(expectRoot() ?: break)
			}
			
			if (expectRightBracket() == null) {
				restore()
				break
			}
			
			removeSaved()
			elifStatements!!.add(elifCond to elifBody)
		}
		if (elifStatements!!.isEmpty()) elifStatements = null
		
		/* else {...} */
		var elseStatements: MutableList<Node>? = mutableListOf()
		val elseCaught = run {
			save()
			
			if (expectKeyword("else") == null) {
				restore()
				return@run false
			}
			
			if (expectLeftBracket() == null) {
				restore()
				return@run false
			}
			
			while (true) elseStatements!!.add(expectRoot() ?: break)
			
			if (expectRightBracket() == null) {
				restore()
				return@run false
			}
			
			removeSaved()
			return@run true
		}
		if (!elseCaught) elseStatements = null
		
		/* returning if statement */
		removeSaved()
		return IfStatementNode(ifCond to ifBody, elifStatements, elseStatements, TextRange(Position(0, 0), Position(0, 0)))
		//fixme: range
	}
	
	private fun expectFunctionDeclaration(): FunctionDeclarationNode? {
		save()
		
		val kw = expectKeyword("func") ?: run { restore(); return null }
		val name = expectIdentifier() ?: run { restore(); return null }
		val params = mutableListOf<String>().apply {
			save()
			expectLeftParenthesis() ?: run { restore(); return@apply }
			
			while (true) {
				add(expectIdentifier()?.name ?: break)
				expectSeparator() ?: break
			}
			
			expectRightParenthesis() ?: run { restore(); return@apply }
			removeSaved()
		}
		
		expectLeftBracket() ?: run { restore(); return null }
		val body = mutableListOf<Node>().apply {
			while (true) add(expectRoot() ?: break)
		}
		val rbr = expectRightBracket() ?: run { restore(); return null }
		
		removeSaved()
		return FunctionDeclarationNode(name.name, params, body, kw.range.start..rbr.range.end)
	}
	
	private fun expectFunctionCall(): FunctionCallNode? {
		save()
		
		val name = expectIdentifier() ?: run { restore(); return null }
		
		expectLeftParenthesis() ?: run { restore(); return null }
		val params = mutableListOf<Node>().apply {
			while (true) {
				add(
					expectUnaryOperation()
						?: expectBinaryOperation()
						?: expectFunctionCall()
						?: expectString()
						?: expectIdentifier()
						?: expectNumber()
						?: break
				)
				expectSeparator() ?: break
			}
		}
		val rpr = expectRightParenthesis() ?: run { restore(); return null }
		
		removeSaved()
		return FunctionCallNode(name.name, params, name.range.start..rpr.range.end)
	}
	
	private fun expectVariableDeclaration(): VariableDeclarationNode? {
		save()
		
		val kw = expectKeyword("var") ?: run { restore(); return null }
		val name = expectIdentifier() ?: run { restore(); return null }
		val value = expectOperator("=")?.run {
			expectUnaryOperation()
				?: expectBinaryOperation()
				?: expectFunctionCall()
				?: expectParenthesizedOperation()
				?: expectIdentifier()
				?: expectNumber()
				?: expectString()
				?: run { restore(); return null }
		}
		val eos = expectEndOfStatement() ?: run { restore(); return null }
		
		removeSaved()
		return VariableDeclarationNode(name.name, value, kw.range.start..eos.range.end)
	}
	
	private fun expectStatement(): StatementNode? {
		save()
		
		val node = expectUnaryOperation()
			?: expectBinaryOperation()
			?: expectFunctionCall()
			?: expectParenthesizedOperation()
			?: expectMemberAccess()
			?: expectIdentifier()
			?: expectNumber()
			?: expectString()
			?: run { restore(); return null }
		
		val eos = expectEndOfStatement() ?: run { restore(); return null }
		
		removeSaved()
		return StatementNode(node, node.range.start..eos.range.end)
	}
	
	private fun expectMemberAccess(): MemberAccessNode? {
		save()
		
		val name = expectIdentifier() ?: run { restore(); return null }
		expectPropertyAccessor() ?: run { restore(); return null }
		val member = expectIdentifier() ?: run { restore(); return null }
		
		removeSaved()
		return MemberAccessNode(name.name, member.name, name.range.start..member.range.end)
	}
	
	private fun expectUnaryOperation(): UnaryOperationNode? {
		save()
		
		val prefix = expectPrefixUnaryOperator()
		val operand = expectParenthesizedOperation()
		// ?: expectUnaryOperation()
			?: expectFunctionCall()
			?: expectIdentifier()
			?: expectNumber()
			?: run { restore(); return null }
		
		prefix?.let {
			removeSaved()
			return UnaryOperationNode(prefix.value, operand, true, prefix.range.start..operand.range.end)
		}
		
		val postfix = expectPostfixUnaryOperator() ?: run { restore(); return null }
		
		removeSaved()
		return UnaryOperationNode(postfix.value, operand, false, operand.range.start..postfix.range.end)
	}
	
	private fun expectBinaryOperation(): BinaryOperationNode? {
		save()
		
		val lhs = expectUnaryOperation()
			?: expectFunctionCall()
			?: expectParenthesizedOperation()
			?: expectMemberAccess()
			?: expectIdentifier()
			?: expectNumber()
			?: expectString()
			?: run { restore(); return null }
		
		val op = expectBinaryOperator()
			?: run { restore(); return null }
		
		val rhs = expectUnaryOperation()
			?: expectBinaryOperation()
			?: expectFunctionCall()
			?: expectParenthesizedOperation()
			?: expectMemberAccess()
			?: expectIdentifier()
			?: expectNumber()
			?: expectString()
			?: run { restore(); return null }
		
		removeSaved()
		return BinaryOperationNode(lhs, op.value, rhs, lhs.range.start..rhs.range.end)
	}
	
	private fun expectParenthesizedOperation(): ParenthesizedOperationNode? {
		save()
		
		val lpr = expectLeftParenthesis()
			?: run { restore(); return null }
		
		val node = expectUnaryOperation()
			?: expectBinaryOperation()
			?: expectParenthesizedOperation()
			?: expectIdentifier()
			?: run { restore(); return null }
		
		val rpr = expectRightParenthesis()
			?: run { restore(); return null }
		
		removeSaved()
		return ParenthesizedOperationNode(node, lpr.range.start..rpr.range.end)
	}
	
	private fun expectKeyword(kw: String): Token? =
		if (peekedToken?.type == TType.ID && peekedToken!!.value == kw) nextToken else null
	
	private fun expectOperator(op: String): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value == op) nextToken else null
	
	private fun expectBinaryOperator(): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value in BIN_OPS) nextToken!! else null
	
	private fun expectPrefixUnaryOperator(): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value in PRE_UN_OPS) nextToken!! else null
	
	private fun expectPostfixUnaryOperator(): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value in POS_UN_OPS) nextToken!! else null
	
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
	
	private fun expectLeftBracket(): Token? {
		return when (peekedToken?.type) {
			TType.LBR -> nextToken
			else -> null
		}
	}
	
	private fun expectRightBracket(): Token? {
		return when (peekedToken?.type) {
			TType.RBR -> nextToken
			else -> null
		}
	}
	
	private fun expectNumber(): NumberNode? {
		return when (peekedToken?.type) {
			TType.INT -> NumberNode(nextToken!!.value.toLong(), currentToken!!.range)
			TType.FLT -> NumberNode(nextToken!!.value.toDouble(), currentToken!!.range)
			else -> null
		}
	}
	
	private fun expectIdentifier(): IdentifierNode? {
		return when (peekedToken?.type) {
			TType.ID -> IdentifierNode(nextToken!!.value, currentToken!!.range)
			else -> null
		}
	}
	
	private fun expectString(): StringNode? {
		return when (peekedToken?.type) {
			TType.STR -> StringNode(nextToken!!.value.removeSurrounding("\""), false, currentToken!!.range)
			TType.RSTR -> StringNode(nextToken!!.value.removeSurrounding("'"), true, currentToken!!.range)
			else -> null
		}
	}
	
	private fun expectEndOfStatement(): Token? {
		return when (peekedToken?.type) {
			TType.EOS -> nextToken
			else -> null
		}
	}
	
	private fun expectSeparator(): Token? {
		return when (peekedToken?.type) {
			TType.SEP -> nextToken
			else -> null
		}
	}
	
	private fun expectPropertyAccessor(): Token? {
		return when (peekedToken?.type) {
			TType.PAC -> nextToken
			else -> null
		}
	}
}