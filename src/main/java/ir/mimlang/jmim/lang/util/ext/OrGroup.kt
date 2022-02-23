package ir.mimlang.jmim.lang.util.ext

class OrGroup<T>(
	lhs: T,
	rhs: T
) {
	val operands: MutableList<T> = mutableListOf()
	
	init {
		operands.add(lhs)
		operands.add(rhs)
	}
	
	infix fun or(rhs: T): OrGroup<T> = this.apply { operands.add(rhs) }
}

infix fun <T> T.or(other: T): OrGroup<T> = OrGroup(this, other)
infix fun <T> T.equals(orGroup: OrGroup<T>): Boolean {
	orGroup.operands.forEach { if (this == it) return true }
	return false
}
