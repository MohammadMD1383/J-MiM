package ir.mimlang.jmim.lang.interpreter

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.node.*
import ir.mimlang.jmim.lang.std.FunctionVariable
import ir.mimlang.jmim.lang.std.ValueVariable
import ir.mimlang.jmim.lang.util.ext.repeat
import ir.mimlang.jmim.lang.util.ext.then
import kotlin.math.pow

class Interpreter(private var context: Context) {
	fun interpret(node: Node): Any? {
		when (node) {
			is StatementNode -> return interpret(node.node)
			is ParenthesizedOperationNode -> return interpret(node.node)
			
			is StringNode -> return node.string // todo: template strings
			is NumberNode -> return node.value
			
			is IdentifierNode -> return context.findVariable(node.name)?.getValue()
				?: throw InterpreterException("no variable named ${node.name}") at node.range
			
			is MemberAccessNode -> return context.findVariable(node.name)?.getProperty(node.member)
				?: throw InterpreterException("No variable named ${node.name}") at node.range
			
			is VariableDeclarationNode -> {
				val value = node.value?.let(::interpret)
				val variable = ValueVariable(node.name, value)
				
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
					val range = leftNode.range.start..rightNode.range.end
					return interpret(
						BinaryOperationNode(
							ParenthesizedOperationNode(
								BinaryOperationNode(
									leftNode,
									node.operator,
									rightNode,
									range
								),
								range
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
				return context.findVariable(node.name)?.invoke(
					Context(context, node.name).apply {
						addVariable(ValueVariable("__params__", node.params.map(this@Interpreter::interpret)))
					}
				) ?: throw InterpreterException("No function named ${node.name}") at node.range
			}
			
			is IfStatementNode -> {
				pushContext("if statement")
				
				(interpret(node.ifBranch.first) as? Boolean)?.then {
					if (node.ifBranch.second.isEmpty()) {
						popContext()
						return null
					}
					
					node.ifBranch.second.toMutableList().apply {
						val lastNode = removeLast()
						forEach(this@Interpreter::interpret)
						val result = interpret(lastNode)
						popContext()
						return result
					}
				} ?: run {
					popContext()
					throw InterpreterException("if condition must be boolean") at node.ifBranch.first.range
				}
				
				node.elifBranches?.forEach {
					(interpret(it.first) as? Boolean)?.then {
						if (it.second.isEmpty()) {
							popContext()
							return null
						}
						
						it.second.toMutableList().apply {
							val lastNode = removeLast()
							forEach(this@Interpreter::interpret)
							val result = interpret(lastNode)
							popContext()
							return result
						}
					} ?: run {
						popContext()
						throw InterpreterException("elif condition must be boolean") at it.first.range
					}
				}
				
				node.elseBranch?.isNotEmpty()?.then {
					node.elseBranch.toMutableList().apply {
						val lastNode = removeLast()
						forEach(this@Interpreter::interpret)
						val result = interpret(lastNode)
						popContext()
						return result
					}
				}
				
				popContext()
				return null
			}
			
			is RepeatLoopStatement -> {
				val count = (interpret(node.times) as? Long)
					?: throw InterpreterException("repeat count must be integer value") at node.times.range
				
				count repeat { i ->
					pushContext("[repeat:$i]")
					
					node.varName?.let {
						context.addVariable(ValueVariable(it, i))
					}
					
					node.body.forEach(this::interpret)
					
					popContext()
				}
				
				return null
			}
			
			else -> throw Exception("Unsupported node at ${node.range}")
		}
	}
	
	private fun pushContext(name: String) {
		context = Context(context, name)
	}
	
	private fun popContext() {
		context.parent?.let { context = it }
	}
}