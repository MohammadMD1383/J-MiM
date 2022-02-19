package ir.mimlang.jmim.lang.node

val Number.node: NumberNode get() = NumberNode(this)
val String.node: StringNode get() = StringNode(this)
val String.idNode: IdentifierNode get() = IdentifierNode(this)
