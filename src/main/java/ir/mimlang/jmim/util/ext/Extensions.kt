package ir.mimlang.jmim.util.ext

fun Int.toCharOrNull(): Char? = if (this != -1) this.toChar() else null

operator fun Regex.contains(char: Char?): Boolean = matches(char.toString())

fun Char.builder(): StringBuilder = StringBuilder(this.toString())

infix fun <T> T.equals(other: T): Boolean = this == other


