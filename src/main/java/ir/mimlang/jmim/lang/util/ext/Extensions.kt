@file:Suppress("NOTHING_TO_INLINE")

package ir.mimlang.jmim.lang.util.ext

import ir.mimlang.jmim.lang.std.Break
import ir.mimlang.jmim.lang.std.Continue

fun Int.toCharOrNull(): Char? = if (this != -1) this.toChar() else null

operator fun Regex.contains(char: Char?): Boolean = matches(char.toString())

fun Char.builder(): StringBuilder = StringBuilder(this.toString())

inline infix fun <T> T.equals(other: T): Boolean = this == other

inline fun breakable(autoHandle: Boolean = false, block: () -> Unit): Break? {
	return try {
		block()
		null
	} catch (e: Break) {
		if (autoHandle && e.weight-- > 0) throw e
		else e
	}
}

inline infix fun Break?.onBreak(block: (Break) -> Unit) {
	if (this != null) {
		block(this)
		if (weight-- > 0) throw this
	}
}

inline fun continuable(autoHandle: Boolean = false, block: () -> Unit): Continue? {
	return try {
		block()
		null
	} catch (e: Continue) {
		if (autoHandle && e.weight-- > 0) throw e
		else e
	}
}

inline infix fun Continue?.onContinue(block: (Continue) -> Unit) {
	if (this != null) {
		block(this)
		if (weight-- > 0) throw this
	}
}

inline infix fun <T> T?.andAlso(block: () -> Unit): T? = also { block() }

inline infix fun Boolean.then(block: () -> Unit): Boolean = also { if (it) block() }
inline infix fun Boolean.otherwise(block: () -> Unit): Boolean = also { if (!it) block() }

inline infix fun <T> T?.then(block: () -> Unit): T? = also { if (it != null) block() }
inline infix fun <T> T?.otherwise(block: () -> Unit): T? = also { if (it == null) block() }

inline infix fun String.prepend(str: String) = str + this

inline fun repeat(count: Long, block: (Long) -> Unit) {
	for (i in 1..count) block(i)
}
