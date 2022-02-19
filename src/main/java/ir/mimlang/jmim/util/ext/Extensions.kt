package ir.mimlang.jmim.util.ext

infix fun String.line(l: Int) = split('\n')[l - 1]
operator fun String.times(i: Int) = repeat(i - 1)
