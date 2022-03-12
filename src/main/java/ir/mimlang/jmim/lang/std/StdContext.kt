package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context

open class StdContext(name: String) : Context(null, name) {
	init {
		variables.add(StdStream)
		variables.add(Null)
		variables.add(True)
		variables.add(False)
		variables.add(Exit)
		variables.add(Unpack)
		variables.add(BreakStatement)
		variables.add(ContinueStatement)
		variables.add(IntValueOf)
		variables.add(StringValueOf)
		variables.add(Random)
	}
}