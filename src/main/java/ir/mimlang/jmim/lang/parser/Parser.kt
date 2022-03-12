package ir.mimlang.jmim.lang.parser

import ir.mimlang.jmim.lang.node.*
import ir.mimlang.jmim.lang.token.*
import ir.mimlang.jmim.lang.util.TextRange
import ir.mimlang.jmim.lang.util.ext.then

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
		processedRange = 0 to 0
		
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
	
	private fun expectRoot() = expectLoopStatement()
		?: expectNamedBlock()
		?: expectWhenExpression()
		?: expectIfStatement()
		?: expectFunctionDeclaration()
		?: expectVariableDeclaration()
		?: expectStatement()
	
	private fun expectNamedBlock(): NamedBlockNode? {
		save()
		
		val name = expectIdentifier() ?: run { restore(); return null }
		val body = expectRootBlock() ?: run { restore(); return null }
		
		removeSaved()
		return NamedBlockNode(name.name, body, name.range.start..body.range.end)
	}
	
	private fun expectLoopStatement(): Node? = expectRepeatLoop()
		?: expectWhileLoop()
		?: expectDoWhileLoop()
		?: expectForLoop()
	
	private fun expectWhileLoop(): WhileLoopStatementNode? {
		save()
		
		val whileKw = expectKeyword("while") ?: run { restore(); return null }
		val condition = expectParenthesizedOperation()?.node ?: run { restore(); return null }
		expectLeftBracket() ?: run { restore(); return null }
		val body = mutableListOf<Node>().apply {
			while (true) add(expectRoot() ?: break)
		}
		val whileEndBracket = expectRightBracket() ?: run { restore(); return null }
		
		removeSaved()
		return WhileLoopStatementNode(condition, body, whileKw.range.start..whileEndBracket.range.end)
	}
	
	private fun expectDoWhileLoop(): DoWhileLoopStatementNode? {
		save()
		
		val doKw = expectKeyword("do") ?: run { restore(); return null }
		val body = expectRootBlock() ?: run { restore(); return null }
		val whileKw = expectKeyword("while")
			?: throw ParserException("expected while keyword after do body in a do-while loop") at body.range
		val condition = expectParenthesizedOperation()
			?: throw ParserException("expected condition after while keyword in a do-while loop") at whileKw.range
		val eos = expectEndOfStatement()
			?: throw ParserException("expected ; at the end of do-while loop") at condition.range
		
		removeSaved()
		return DoWhileLoopStatementNode(condition, body, doKw.range.start..eos.range.end)
	}
	
	private fun expectRepeatLoop(): RepeatLoopStatementNode? {
		save()
		
		val repeatKw = expectKeyword("repeat") ?: run { restore(); return null }
		val times = expectMemberAccess()
			?: expectIdentifier()
			?: expectNumber()
			?: run { restore(); return null }
		val assignedTo = expectKeyword("as")?.let {
			expectIdentifier() ?: throw ParserException("expected identifier after 'as'") at it.range
		}
		
		expectLeftBracket() ?: run { restore(); return null }
		val body = mutableListOf<Node>().apply {
			while (true) add(expectRoot() ?: break)
		}
		val repeatEndBracket = expectRightBracket() ?: run { restore(); return null }
		
		removeSaved()
		return RepeatLoopStatementNode(times, assignedTo?.name, body, repeatKw.range.start..repeatEndBracket.range.end)
	}
	
	// todo make precise error reporting for all methods
	private fun expectForLoop(): ForLoopStatementNode? {
		save()
		
		val forKw = expectKeyword("for") ?: run { restore(); return null }
		val first = expectIdentifier() ?: run { restore(); return null }
		val second = expectSeparator()?.let { expectIdentifier() }
		val inKw = expectKeyword("in") ?: throw ParserException("expected 'in' keyword") at (second?.range ?: first.range)
		val iterable = expectNamedBlock()
			?: expectFunctionCall()
			?: expectParenthesizedOperation()
			?: expectMemberAccess()
			?: expectIdentifier()
			?: expectString()
			?: throw ParserException("invalid token for iterable part of for loop") at inKw.range
		val body = expectRootBlock() ?: throw ParserException("expected block body") at iterable.range
		
		val keyHolder: String?
		val valueHolder: String
		if (second != null) {
			keyHolder = first.name
			valueHolder = second.name
		} else {
			keyHolder = null
			valueHolder = first.name
		}
		
		removeSaved()
		return ForLoopStatementNode(keyHolder, valueHolder, iterable, body, forKw.range.start..body.range.end)
	}
	
	private fun expectWhenExpression(): WhenExpressionNode? {
		save()
		
		val whenKw = expectKeyword("when") ?: run { restore(); return null }
		val whenOperand = expectParenthesizedOperation() ?: run { restore(); return null }
		val whenComparator = expectComparisonOperator()
		
		//todo make better
		val whenLbr = expectLeftBracket()
			?: throw ParserException("expected { for when expression body") at (whenComparator?.range ?: whenOperand.range)
		
		val whenCases = mutableListOf<FullCaseNode>().apply {
			while (true) {
				val orGroup = expectCaseOrGroup() ?: break
				val body = expectRootBlock()
					?: throw ParserException("expected block body after case") at orGroup.groups.last().range
				
				add(FullCaseNode(orGroup, body, orGroup.range.start..body.range.end))
			}
		}
		
		val default = expectKeyword("default")?.let {
			expectRootBlock() ?: throw ParserException("expected block body after default") at it.range
		}
		
		val whenRbr = expectRightBracket()
			?: throw ParserException("expected } at the end of the when expression") at (
				default?.range
					?: whenCases.lastOrNull()?.body?.range // todo make better
					?: whenLbr.range
				)
		
		removeSaved()
		return WhenExpressionNode(whenOperand, whenComparator?.value, whenCases, default, whenKw.range.start..whenRbr.range.end)
	}
	
	private fun expectCase(): CaseNode? {
		save()
		
		val caseKw = expectKeyword("case") ?: run { restore(); return null }
		val caseComparator = expectComparisonOperator()
		val caseOperand = expectParenthesizedOperation() ?: run { restore(); return null }
		
		removeSaved()
		return CaseNode(caseComparator?.value, caseOperand, caseKw.range.start..caseOperand.range.end)
	}
	
	private fun expectCaseAndGroup(): CaseAndGroupNode? {
		save()
		
		val cases = mutableListOf<CaseNode>().apply {
			add(expectCase() ?: run { restore(); return null })
			while (true) add(expectCase() ?: break)
		}
		
		removeSaved()
		return CaseAndGroupNode(cases, cases.first().range.start..cases.last().range.end)
	}
	
	private fun expectCaseOrGroup(): CaseOrGroupNode? {
		save()
		
		val cases = mutableListOf<CaseAndGroupNode>().apply {
			add(expectCaseAndGroup() ?: run { restore(); return null })
			while (true) {
				expectSeparator() ?: break
				add(expectCaseAndGroup() ?: break)
			}
		}
		
		removeSaved()
		return CaseOrGroupNode(cases, cases.first().range.start..cases.last().range.end)
	}
	
	private fun expectIfStatement(): IfStatementNode? { // todo add precise error reporting - make use of expectRootBlock if possible
		save()
		
		/* if (...) {...} */
		val ifKw = expectKeyword("if") ?: run { restore(); return null }
		val ifCond = expectParenthesizedOperation()?.node ?: run { restore(); return null }
		val ifBody = expectRootBlock() ?: run { restore(); return null }
		
		/* elif (...) {...} */
		var elifStatements: MutableList<Pair<Node, List<Node>>>? = mutableListOf()
		var elifRbr: Token? = null
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
			
			val prevElifRbr = elifRbr
			elifRbr = expectRightBracket()
			if (elifRbr == null) {
				elifRbr = prevElifRbr
				restore()
				break
			}
			
			removeSaved()
			elifStatements!!.add(elifCond to elifBody)
		}
		if (elifStatements!!.isEmpty()) elifStatements = null
		
		/* else {...} */
		var elseStatements: MutableList<Node>? = mutableListOf()
		var elseRbr: Token? = null
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
			
			elseRbr = expectRightBracket()
			if (elseRbr == null) {
				restore()
				return@run false
			}
			
			removeSaved()
			return@run true
		}
		if (!elseCaught) elseStatements = null
		
		/* returning if statement */
		val endRange = when {
			elseCaught -> elseRbr!!.range.end
			elifRbr != null -> elifRbr.range.end
			else -> ifBody.range.end
		}
		
		removeSaved()
		return IfStatementNode(ifCond to ifBody.nodes, elifStatements, elseStatements, ifKw.range.start..endRange)
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
		val params = expectParenthesizedParams() ?: run { restore(); return null }
		
		removeSaved()
		return FunctionCallNode(name.name, params.first, name.range.start..params.second.end)
	}
	
	private fun expectVariableDeclaration(): VariableDeclarationNode? {
		save()
		
		var isConst = false
		val kw = expectKeyword("var")
			?: (expectKeyword("val") then { isConst = true })
			?: run { restore(); return null }
		val name = expectIdentifier() ?: run { restore(); return null }
		val value = expectOperator("=")?.run {
			expectNamedBlock()
				?: expectIfStatement()
				?: expectBinaryOperation()
				?: expectUnaryOperation()
				?: expectFunctionCall()
				?: expectParenthesizedOperation()
				?: expectMemberAccess()
				?: expectIdentifier()
				?: expectNumber()
				?: expectString()
				?: run { restore(); return null }
		}
		val eos = expectEndOfStatement() ?: run { restore(); return null }
		
		removeSaved()
		return VariableDeclarationNode(name.name, value, isConst, kw.range.start..eos.range.end)
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
		val params = expectParenthesizedParams()
		var endRange = params?.second?.end ?: member.range.end
		
		val moreAccessors = mutableListOf<Pair<String, List<Node>?>>()
		while (true) {
			save()
			
			if (expectPropertyAccessor() == null) {
				restore()
				break
			}
			
			val chainedMember = expectIdentifier()
			if (chainedMember == null) {
				restore()
				break
			}
			
			val chainedMemberParams = expectParenthesizedParams()
			endRange = chainedMemberParams?.second?.end ?: chainedMember.range.end
			
			removeSaved()
			moreAccessors.add(chainedMember.name to chainedMemberParams?.first)
		}
		moreAccessors.add(0, member.name to params?.first)
		
		removeSaved()
		return MemberAccessNode(name.name, moreAccessors, name.range.start..endRange) // todo make cleaner
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
		
		val lhs = expectIfStatement()
			?: expectUnaryOperation()
			?: expectFunctionCall()
			?: expectParenthesizedOperation()
			?: expectMemberAccess()
			?: expectIdentifier()
			?: expectNumber()
			?: expectString()
			?: run { restore(); return null }
		
		val op = expectBinaryOperator()
			?: run { restore(); return null }
		
		val rhs = expectNamedBlock()
			?: expectIfStatement()
			?: expectBinaryOperation()
			?: expectUnaryOperation()
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
		
		val node = expectNamedBlock()
			?: expectIfStatement()
			?: expectBinaryOperation()
			?: expectUnaryOperation()
			?: expectFunctionCall()
			?: expectParenthesizedOperation()
			?: expectMemberAccess()
			?: expectIdentifier()
			?: expectNumber()
			?: expectString()
			?: run { restore(); return null }
		
		val rpr = expectRightParenthesis()
			?: run { restore(); return null }
		
		removeSaved()
		return ParenthesizedOperationNode(node, lpr.range.start..rpr.range.end)
	}
	
	private fun expectRootBlock(): BlockNode? {
		save()
		
		val lbr = expectLeftBracket() ?: run { restore(); return null }
		val body = mutableListOf<Node>().apply {
			while (true) add(expectRoot() ?: break)
		}
		val rbr = expectRightBracket() ?: run { restore(); return null }
		
		removeSaved()
		return BlockNode(body, lbr.range.start..rbr.range.end)
	}
	
	private fun expectParenthesizedParams(): Pair<List<Node>, TextRange>? {
		save()
		
		val lpr = expectLeftParenthesis() ?: run { restore(); return null }
		val params = mutableListOf<Node>().apply {
			while (true) {
				add(
					expectNamedBlock()
						?: expectBinaryOperation()
						?: expectUnaryOperation()
						?: expectFunctionCall()
						?: expectParenthesizedOperation()
						?: expectString()
						?: expectMemberAccess()
						?: expectIdentifier()
						?: expectNumber()
						?: break
				)
				
				expectSeparator() ?: break
			}
		}
		val rpr = expectRightParenthesis() ?: run { restore(); return null }
		
		removeSaved()
		return params to lpr.range.start..rpr.range.end
	}
	
	private fun expectKeyword(kw: String): Token? =
		if (peekedToken?.type == TType.ID && peekedToken!!.value == kw) nextToken!! else null
	
	private fun expectOperator(op: String): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value == op) nextToken!! else null
	
	private fun expectBinaryOperator(): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value in BIN_OPS) nextToken!! else null
	
	private fun expectComparisonOperator(): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value in COMP_OPS) nextToken!! else null
	
	private fun expectPrefixUnaryOperator(): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value in PRE_UN_OPS) nextToken!! else null
	
	private fun expectPostfixUnaryOperator(): Token? =
		if (peekedToken?.type == TType.OP && peekedToken!!.value in POS_UN_OPS) nextToken!! else null
	
	private fun expectToken(type: TType): Token? = if (peekedToken?.type == type) nextToken!! else null
	
	private fun expectLeftParenthesis(): Token? = expectToken(TType.LPR)
	private fun expectRightParenthesis(): Token? = expectToken(TType.RPR)
	private fun expectLeftBracket(): Token? = expectToken(TType.LBR)
	private fun expectRightBracket(): Token? = expectToken(TType.RBR)
	private fun expectEndOfStatement(): Token? = expectToken(TType.EOS)
	private fun expectSeparator(): Token? = expectToken(TType.SEP)
	private fun expectPropertyAccessor(): Token? = expectToken(TType.PAC)
	
	private fun expectNumber(): NumberNode? = when (peekedToken?.type) {
		TType.INT -> NumberNode(nextToken!!.value.toLong(), currentToken!!.range)
		TType.FLT -> NumberNode(nextToken!!.value.toDouble(), currentToken!!.range)
		else -> null
	}
	
	private fun expectIdentifier(): IdentifierNode? = when (peekedToken?.type) {
		TType.ID -> IdentifierNode(nextToken!!.value, currentToken!!.range)
		else -> null
	}
	
	private fun expectString(): StringNode? = when (peekedToken?.type) {
		TType.STR -> StringNode(nextToken!!.value.removeSurrounding("\""), false, currentToken!!.range)
		TType.RSTR -> StringNode(nextToken!!.value.removeSurrounding("'"), true, currentToken!!.range)
		else -> null
	}
}