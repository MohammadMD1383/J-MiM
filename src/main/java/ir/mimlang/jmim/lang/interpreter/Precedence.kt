package ir.mimlang.jmim.lang.interpreter

val String.precedence
	get() = when (this) {
		"^" -> 7
		
		"*",
		"/",
		"%" -> 6
		
		"+",
		"-" -> 5
		
		"&",
		"|" -> 4
		
		"<<",
		">>",
		">>>" -> 3
		
		"==",
		"!=",
		">",
		">=",
		"<",
		"<=" -> 2
		
		"&&",
		"||" -> 1
		
		"=",
		"&=",
		"|=",
		"+=",
		"-=",
		"*=",
		"/=",
		"%=",
		"^=",
		"<<=",
		">>=",
		"~=" -> 0
		
		else -> throw Exception("precedence of operator $this is not defined")
	}