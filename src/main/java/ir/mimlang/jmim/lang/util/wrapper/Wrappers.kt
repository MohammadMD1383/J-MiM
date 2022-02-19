package ir.mimlang.jmim.lang.util.wrapper

@JvmInline
value class Line(val value: Int) {
	operator fun inc(): Line = Line(value + 1)
}

@JvmInline
value class Column(val value: Int) {
	operator fun inc(): Column = Column(value + 1)
}

val Int.line: Line get() = Line(this)
val Int.column: Column get() = Column(this)
