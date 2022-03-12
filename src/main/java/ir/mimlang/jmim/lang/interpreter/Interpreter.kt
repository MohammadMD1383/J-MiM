package ir.mimlang.jmim.lang.interpreter

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import ir.mimlang.jmim.lang.node.*
import ir.mimlang.jmim.lang.std.FunctionVariable
import ir.mimlang.jmim.lang.std.ValueVariable
import ir.mimlang.jmim.lang.util.ext.*
import kotlin.math.pow

class Interpreter(private var context: Context) {
	fun interpret(node: Node): Any? {
		when (node) {
			is StatementNode -> return interpret(node.node)
			is ParenthesizedOperationNode -> return interpret(node.node)
			
			is StringNode -> return node.string // todo: template strings
			is NumberNode -> return node.value
			
			is IdentifierNode -> {
				val variable = context.findVariable(node.name)
					?: throw InterpreterException("no variable named ${node.name}") at node.range
				
				return variable.getValue()
			}
			
			is MemberAccessNode -> { // todo make cleaner
				var variable = context.findVariable(node.name)
					?: throw InterpreterException("no variable named ${node.name}") at node.range
				
				
				for (i in 0 until node.accessors.lastIndex) {
					variable = if (node.accessors[i].second != null) {
						val result = variable.invokeMember(
							node.accessors[i].first,
							Context(context, node.accessors[i].first).apply {
								addVariable(ValueVariable("__params__", node.accessors[i].second!!.map(this@Interpreter::interpret), true))
							}
						)
						ValueVariable("access", result)
					} else {
						val result = variable.getProperty(node.accessors[i].first)
						ValueVariable("access", result)
					}
				}
				
				return if (node.accessors.last().second != null) {
					variable.invokeMember(
						node.accessors.last().first,
						Context(context, node.accessors.last().first).apply {
							addVariable(ValueVariable("__params__", node.accessors.last().second!!.map(this@Interpreter::interpret), true))
						}
					)
				} else variable.getProperty(node.accessors.last().first)
			}
			
			is VariableDeclarationNode -> {
				val value = node.value?.let(::interpret)
				val variable = ValueVariable(node.name, value, node.isConst)
				
				if (context.addVariable(variable)) return value
				else throw InterpreterException("variable ${node.name} already exists") at node.range
			}
			
			is UnaryOperationNode -> {
				when (node.operator) { // fixme: result of function mustn't come across elvis operator
					"!" -> return (interpret(node.operand) as? Boolean)?.not()
						?: throw InterpreterException("operand of ! operator must be boolean") at node.range
					
					"~" -> return (interpret(node.operand) as? Long)?.inv()
						?: throw InterpreterException("operand of ~ operator must be integer") at node.range
					
					"-" -> {
						val operand = interpret(node.operand)
						return (operand as? Long)?.unaryMinus()
							?: (operand as? Double)?.unaryMinus()
							?: throw InterpreterException("operand of - operator must be number") at node.range
					}
					
					"--", "++" -> {
						if (node.operand is IdentifierNode) {
							val variable = context.findVariable(node.operand.name)
								?: throw InterpreterException("variable ${node.operand.name} doesn't exist") at node.operand.range
							
							node.isPrefixed.not().let {
								return if (node.operator == "++") variable.increment(it) else variable.decrement(it)
							}
						} else throw InterpreterException("operand of (++ or --) operator must be identifier") at node.range
					}
					
					else -> throw InterpreterException("Unsupported Operator") at node.range
				}
			}
			
			is BinaryOperationNode -> {
				val leftNode: Node
				val rightNode: Node
				
				if (node.rhs is BinaryOperationNode && node.operator.precedence >= node.rhs.operator.precedence) {
					leftNode = node.lhs
					rightNode = node.rhs.lhs
					
					// 2 * (4 + 6) will change to:
					// (2 * 4) + 6
					return interpret(
						BinaryOperationNode(
							BinaryOperationNode(
								leftNode,
								node.operator,
								rightNode,
								leftNode.range.start..rightNode.range.end
							),
							node.rhs.operator,
							node.rhs.rhs,
							node.range
						)
					)
				} else {
					leftNode = node.lhs
					rightNode = node.rhs
				}
				
				when (node.operator) {
					"=" -> {
						when (leftNode) {
							is IdentifierNode -> {
								val variable = context.findVariable(leftNode.name)
									?: throw InterpreterException("no variable named ${leftNode.name}") at leftNode.range
								val value = interpret(rightNode)
								return value.also { variable.setValue(it) }
							}
							
							is MemberAccessNode -> { // todo make it work better
								val variable = context.findVariable(leftNode.name)
									?: throw InterpreterException("no variable named ${leftNode.name}") at leftNode.range
								
								leftNode.accessors.single().second then { throw InterpreterException("cannot assign to member invocation") at node.range }
								
								val value = interpret(rightNode)
								return value.also { variable.setProperty(leftNode.accessors.single().first, it) }
							}
							
							else -> throw InterpreterException("Left hand side in an assignment must be an identifier or member access") at leftNode.range
						}
					}
					
					"+" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is String -> left + right
							left is Double && right is String -> "$left$right"
							left is Long && right is String -> "$left$right"
							
							left is Long && right is Long -> left + right
							left is Long && right is Double -> left + right
							left is Double && right is Double -> left + right
							left is Double && right is Long -> left + right
							
							else -> throw InterpreterException("Unsupported operands for + operator") at node.range
						}
					}
					
					"-" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is Long -> left - right
							left is Long && right is Double -> left - right
							left is Double && right is Double -> left - right
							left is Double && right is Long -> left - right
							
							else -> throw InterpreterException("Unsupported operands for - operator") at node.range
						}
					}
					
					"*" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is String -> right.repeat(left.toInt())
							left is String && right is Long -> left.repeat(right.toInt())
							
							left is Long && right is Long -> left * right
							left is Long && right is Double -> left * right
							left is Double && right is Double -> left * right
							left is Double && right is Long -> left * right
							
							else -> throw InterpreterException("Unsupported operands for * operator") at node.range
						}
					}
					
					"/" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is Long -> left / right
							left is Long && right is Double -> left / right
							left is Double && right is Double -> left / right
							left is Double && right is Long -> left / right
							
							else -> throw InterpreterException("Unsupported operands for / operator") at node.range
						}
					}
					
					"^" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is Long -> left.toDouble().pow(right.toDouble())
							left is Long && right is Double -> left.toDouble().pow(right)
							left is Double && right is Double -> left.pow(right)
							left is Double && right is Long -> left.pow(right.toDouble())
							
							else -> throw InterpreterException("Unsupported operands for ^ operator") at node.range
						}
					}
					
					"<" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is Long -> left < right
							left is Long && right is Double -> left < right
							left is Double && right is Double -> left < right
							left is Double && right is Long -> left < right
							
							else -> throw InterpreterException("Unsupported operands for < operator") at node.range
						}
					}
					
					">" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is Long -> left > right
							left is Long && right is Double -> left > right
							left is Double && right is Double -> left > right
							left is Double && right is Long -> left > right
							
							else -> throw InterpreterException("Unsupported operands for > operator") at node.range
						}
					}
					
					"<=" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is Long -> left <= right
							left is Long && right is Double -> left <= right
							left is Double && right is Double -> left <= right
							left is Double && right is Long -> left <= right
							
							else -> throw InterpreterException("Unsupported operands for <= operator") at node.range
						}
					}
					
					">=" -> {
						val left = interpret(leftNode)
						val right = interpret(rightNode)
						
						return when {
							left is Long && right is Long -> left >= right
							left is Long && right is Double -> left >= right
							left is Double && right is Double -> left >= right
							left is Double && right is Long -> left >= right
							
							else -> throw InterpreterException("Unsupported operands for >= operator") at node.range
						}
					}
					
					"||" -> {
						(interpret(leftNode) as? Boolean)?.then {
							return true
						} ?: throw InterpreterException("operands of || operator must be boolean") at leftNode.range
						
						(interpret(rightNode) as? Boolean)?.then {
							return true
						} ?: throw InterpreterException("operands of || operator must be boolean") at rightNode.range
						
						return false
					}
					
					"&&" -> {
						(interpret(leftNode) as? Boolean)?.then {
							(interpret(rightNode) as? Boolean)?.then {
								return true
							} ?: throw InterpreterException("operands of && operator must be boolean") at rightNode.range
						} ?: throw InterpreterException("operands of && operator must be boolean") at leftNode.range
						
						return false
					}
					
					"==" -> return interpret(leftNode) == interpret(rightNode)
					"!=" -> return interpret(leftNode) != interpret(rightNode)
					
					else -> throw InterpreterException("Unsupported Operator") at leftNode.range.end..rightNode.range.start
				}
			}
			
			is FunctionDeclarationNode -> {
				val function = FunctionVariable(node.name, node.params, node.body)
				if (context.addVariable(function)) return function
				else throw InterpreterException("function ${node.name} already exists")
			}
			
			is FunctionCallNode -> {
				val variable = context.findVariable(node.name)
					?: throw InterpreterException("No function named ${node.name}") at node.range
				
				return variable.invoke(
					Context(context, node.name).apply {
						addVariable(ValueVariable("__params__", node.params.map(this@Interpreter::interpret), true))
					}
				)
			}
			
			is IfStatementNode -> {
				(interpret(node.ifBranch.first) as? Boolean)?.then {
					node.ifBranch.second.isEmpty() then { return null }
					
					breakable {
						pushContext("if statement")
						val result = node.ifBranch.second.interpretAndReturnLast()
						popContext()
						return result
					} onBreak {
						popContext()
						return null
					}
				} ?: throw InterpreterException("if condition must be boolean") at node.ifBranch.first.range
				
				node.elifBranches?.forEach {
					(interpret(it.first) as? Boolean)?.then {
						it.second.isEmpty() then { return null }
						
						breakable {
							pushContext("elif statement")
							val result = it.second.interpretAndReturnLast()
							popContext()
							return result
						} onBreak {
							popContext()
							return null
						}
					} ?: throw InterpreterException("elif condition must be boolean") at it.first.range
				}
				
				node.elseBranch?.isNotEmpty()?.then {
					breakable {
						pushContext("else statement")
						val result = node.elseBranch.interpretAndReturnLast()
						popContext()
						return result
					} onBreak {
						popContext()
						return null
					}
				}
				
				return null
			}
			
			is RepeatLoopStatementNode -> {
				val count = (interpret(node.times) as? Long)
					?: throw InterpreterException("repeat count must be integer value") at node.times.range
				
				breakable {
					repeat(count) { i ->
						continuable {
							pushContext("[repeat:$i]")
							
							node.varName?.let {
								context.addVariable(ValueVariable(it, i, true))
							}
							node.body.interpret()
							
							popContext()
						} onContinue { popContext() }
					}
				} onBreak { popContext() }
				
				return null
			}
			
			is WhileLoopStatementNode -> {
				breakable {
					var loop = true
					while (loop) {
						continuable {
							val cond = interpret(node.condition) as? Boolean
								?: throw InterpreterException("condition of while loop must be boolean") at node.condition.range
							
							if (cond) {
								pushContext("while loop")
								node.body.interpret()
								popContext()
							} else loop = false
						} onContinue { popContext() }
					}
				} onBreak { popContext() }
				
				return null
			}
			
			is DoWhileLoopStatementNode -> {
				breakable {
					var loop = true
					do {
						continuable {
							val cond = interpret(node.condition) as? Boolean
								?: throw InterpreterException("condition of do-while loop must be boolean") at node.condition.range
							
							if (cond) {
								pushContext("do-while loop")
								node.body.nodes.interpret()
								popContext()
							} else loop = false
						} onContinue { popContext() }
					} while (loop)
				} onBreak { popContext() }
				
				return null
			}
			
			is ForLoopStatementNode -> {
				breakable {
					val iterable = interpret(node.iterable)?.let { if (it is Variable) it.getValue() else it }
					
					if (node.key == null) {
						when (iterable) {
							is List<*> -> iterable.forEach {
								continuable {
									pushContext("for loop")
									context.addVariable(ValueVariable(node.value, it, true))
									node.body.nodes.interpret()
									popContext()
								} onContinue { popContext() }
							}
							
							is String -> iterable.forEach {
								continuable {
									pushContext("for loop")
									context.addVariable(ValueVariable(node.value, it, true))
									node.body.nodes.interpret()
									popContext()
								} onContinue { popContext() }
							}
							
							else -> throw InterpreterException("only lists and strings are allowed in value based for loop") at node.iterable.range
						}
					} else {
						if (iterable is Map<*, *>) iterable.forEach { key, value ->
							continuable {
								pushContext("for loop")
								context.addVariable(ValueVariable(node.key, key, true))
								context.addVariable(ValueVariable(node.value, value, true))
								node.body.nodes.interpret()
								popContext()
							} onContinue { popContext() }
						} else throw InterpreterException("only maps are allowed in key-value based for loop") at node.iterable.range
					}
				} onBreak {
					popContext()
					return null
				}
				
				return null
			}
			
			is WhenExpressionNode -> {
				breakable {
					pushContext("when expression")
					
					node.cases.forEach { fullCase ->
						fullCase.condition.check(node.operand, node.comparator) then {
							val result = fullCase.body.nodes.interpretAndReturnLast()
							popContext()
							return result
						}
					}
					
					val result = node.default?.nodes?.interpretAndReturnLast()
					popContext()
					return result
				} onBreak {
					popContext()
					return null
				}
				
				return null
			}
			
			is NamedBlockNode -> {
				when (node.name) {
					"repeat" -> {
						breakable {
							while (true) {
								continuable {
									pushContext("infinite repeat block")
									node.body.nodes.interpret()
									popContext()
								} onContinue { popContext() }
							}
						} onBreak { popContext() }
						
						return null
					}
					
					"run" -> {
						pushContext("run block")
						val result = node.body.nodes.interpretAndReturnLast()
						popContext()
						
						return result
					}
					
					"list" -> return node.body.nodes.map(this::interpret)
					
					"map" -> {
						val map = mutableMapOf<String, Any?>()
						for (i in node.body.nodes.indices step 2) {
							val key = (interpret(node.body.nodes[i]) as? String)
								?: throw InterpreterException("map key must be string") at node.body.nodes[i].range
							val value = node.body.nodes.getOrNull(i + 1)?.let { interpret(it) }
							
							map[key] = value
						}
						
						return map
					}
					
					"func" -> return FunctionVariable("lambda${node.range}", listOf(), node.body.nodes)
					
					else -> throw InterpreterException("Named block with name '${node.name}' is not known") at node.range
				}
			}
			
			else -> throw InterpreterException("Unsupported node") at node.range
		}
	}
	
	private fun pushContext(name: String) {
		context = Context(context, name)
	}
	
	private fun popContext() {
		context.parent?.let { context = it }
	}
	
	private fun List<Node>.interpret() = forEach(this@Interpreter::interpret)
	
	private fun List<Node>.interpretAndReturnLast(): Any? {
		isEmpty() then { return null }
		
		for (i in 0 until lastIndex) interpret(get(i))
		return interpret(last())
	}
	
	private fun CaseOrGroupNode.check(
		operand: ParenthesizedOperationNode,
		defaultComparator: String?
	): Boolean {
		val cond = groups.let orResult@{ orGroup ->
			orGroup.forEach { andGroup ->
				val andResult = andGroup.cases.let andResult@{ cases ->
					cases.forEach { case ->
						val caseResult = interpret(
							BinaryOperationNode(
								operand,
								case.operator ?: defaultComparator ?: throw InterpreterException("no operator found to check case condition") at case.range,
								case.operand,
								case.range
							)
						) as Boolean
						if (!caseResult) return@andResult false
					}
					return@andResult true
				}
				if (andResult) return@orResult true
			}
			return@orResult false
		}
		return cond
	}
}