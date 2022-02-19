package ir.mimlang.jmim.util

import ir.mimlang.jmim.lang.node.*

class VisualNode(private val node: Node) {
	val string: String
		get() = when (node) {
			is NumberNode -> node.value.toString()
			is IdentifierNode -> node.name
			is BinaryOperationNode -> node.operator
			else -> node.toString()
		}
	
	val left: VisualNode?
		get() = when (node) {
			is BinaryOperationNode -> node.lhs.visual
			else -> null
		}
	
	val right: VisualNode?
		get() = when (node) {
			is BinaryOperationNode -> node.rhs.visual
			else -> null
		}
}

val Node.visual: VisualNode get() = VisualNode(this)

fun printTree(root: VisualNode?) {
	val lines: MutableList<List<String?>> = ArrayList()
	var level: MutableList<VisualNode?> = ArrayList()
	var next: MutableList<VisualNode?> = ArrayList()
	level.add(root)
	var nn = 1
	var widest = 0
	while (nn != 0) {
		val line: MutableList<String?> = ArrayList()
		nn = 0
		for (n in level) {
			if (n == null) {
				line.add(null)
				next.add(null)
				next.add(null)
			} else {
				val aa: String = n.string
				line.add(aa)
				if (aa.length > widest) widest = aa.length
				next.add(n.left)
				next.add(n.right)
				if (n.left != null) nn++
				if (n.right != null) nn++
			}
		}
		if (widest % 2 == 1) widest++
		lines.add(line)
		val tmp: MutableList<VisualNode?> = level
		level = next
		next = tmp
		next.clear()
	}
	var perpiece = lines[lines.size - 1].size * (widest + 4)
	for (i in lines.indices) {
		val line = lines[i]
		val hpw = Math.floor((perpiece / 2f).toDouble()).toInt() - 1
		if (i > 0) {
			for (j in line.indices) {
				
				// split node
				var c = ' '
				if (j % 2 == 1) {
					if (line[j - 1] != null) {
						c = if (line[j] != null) '┴' else '┘'
					} else {
						if (j < line.size && line[j] != null) c = '└'
					}
				}
				print(c)
				
				// lines and spaces
				if (line[j] == null) {
					for (k in 0 until perpiece - 1) {
						print(" ")
					}
				} else {
					for (k in 0 until hpw) {
						print(if (j % 2 == 0) " " else "─")
					}
					print(if (j % 2 == 0) "┌" else "┐")
					for (k in 0 until hpw) {
						print(if (j % 2 == 0) "─" else " ")
					}
				}
			}
			println()
		}
		
		// print line of numbers
		for (j in line.indices) {
			var f = line[j]
			if (f == null) f = ""
			val gap1 = Math.ceil((perpiece / 2f - f.length / 2f).toDouble()).toInt()
			val gap2 = Math.floor((perpiece / 2f - f.length / 2f).toDouble()).toInt()
			
			// a number
			for (k in 0 until gap1) {
				print(" ")
			}
			print(f)
			for (k in 0 until gap2) {
				print(" ")
			}
		}
		println()
		perpiece /= 2
	}
}