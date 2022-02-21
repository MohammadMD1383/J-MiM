package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context

class StdContext(name: String) : Context(null, name) {
	init {
		variables.add(StdStream)
		variables.add(True)
		variables.add(False)
	}
}