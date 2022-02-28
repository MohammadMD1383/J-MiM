package ir.mimlang.jmim.lang.interpreter

import ir.mimlang.jmim.lang.ctx.Context
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
			
			is MemberAccessNode -> {
				val variable = context.findVariable(node.name)
					?: throw InterpreterException("no variable named ${node.name}") at node.range
				
				return if (node.params != null) {
					variable.invokeMember(
						Context(context, "${node.name}${node.member}").apply {
							addVariable(ValueVariable("__params__", node.params.map(this@Interpreter::interpret), true))
						}
					)
				} else variable.getProperty(node.member)
			}
			
			is VariableDeclarationNode -> {
				val value = node.value?.let(::interpret)
				val variable = ValueVariable(node.name, value, node.isConst)
				
				if (context.addVariable(variable)) return value
				else throw InterpreterException("variable ${node.name} already exists") at node.range
			}
			
			is UnaryOperationNode -> {
				when (node.operator) {
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
							
							is MemberAccessNode -> {
								val variable = context.findVariable(leftNode.name)
									?: throw InterpreterException("no variable named ${leftNode.name}") at leftNode.range
								
								leftNode.params then { throw InterpreterException("cannot assign to member invocation") at node.range }
								
								val value = interpret(rightNode)
								return value.also { variable.setProperty(leftNode.member, it) }
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
						node.ifBranch.second.toMutableList().apply {
							val lastNode = removeLast()
							interpret()
							val result = interpret(lastNode)
							popContext()
							return result
						}
					} onBreak { popContext() }
				} ?: throw InterpreterException("if condition must be boolean") at node.ifBranch.first.range
				
				node.elifBranches?.forEach {
					(interpret(it.first) as? Boolean)?.then {
						it.second.isEmpty() then { return null }
						
						breakable {
							pushContext("elif statement")
							it.second.toMutableList().apply {
								val lastNode = removeLast()
								interpret()
								val result = interpret(lastNode)
								popContext()
								return result
							}
						} onBreak { popContext() }
					} ?: throw InterpreterException("elif condition must be boolean") at it.first.range
				}
				
				node.elseBranch?.isNotEmpty()?.then {
					breakable {
						pushContext("else statement")
						node.elseBranch.toMutableList().apply {
							val lastNode = removeLast()
							interpret()
							val result = interpret(lastNode)
							popContext()
							return result
						}
					} onBreak { popContext() }
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
								?: throw InterpreterException("condition of while loop must be boolean value") at node.condition.range
							
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
			
			is NamedBlockNode -> {
				when (node.name) {
					"repeat" -> {
						breakable {
							while (true) {
								continuable {
									pushContext("infinite repeat block")
									node.body.interpret()
									popContext()
								} onContinue { popContext() }
							}
						} onBreak { popContext() }
						
						return null
					}
					
					"run" -> {
						pushContext("run block")
						val result = node.body.interpretAndReturnLast()
						popContext()
						
						return result
					}
					
					"list" -> return node.body.map(this::interpret)
					
					"lambda" -> return FunctionVariable("lambda${node.range}", listOf(), node.body)
					
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
	
	@Suppress("NOTHING_TO_INLINE")
	private inline fun List<Node>.interpret() = forEach(this@Interpreter::interpret)
	
	private fun List<Node>.interpretAndReturnLast(): Any? { // todo make use of this all over interpreter
		isEmpty() then { return null }
		
		toMutableList().run {
			val lastNode = removeLast()
			interpret()
			return interpret(lastNode)
		}
	}
}