package ir.mimlang.jmim.util.ext

infix fun String.line(l: Int) = split('\n')[l - 1]
infix fun String.lines(range: IntRange) = split('\n').subList(range.first - 1, range.last).joinToString("\n")

operator fun String.times(i: Int) = repeat(i - 1)
