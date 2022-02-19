package ir.mimlang.jmim.lang.node

val Number.node: NumberNode get() = NumberNode(this)
val String.node: IdentifierNode get() = IdentifierNode(this)
