package ir.mimlang.jmim.lang.std.filestream

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable

val Converter = object : Variable {
    override val name: String
        get() = "Converter"

    override fun invokeMember(name: String, context: Context): Any? {
        val params =  context.getParams()!!
        if (params.isNotEmpty()){
            if(params[0] is String){
                val json = params[0] as String
                return json
            }else{
                throw Exception("Input must be String")
            }
        }else{
            throw Exception("This method doesn't exist")
        }
    }
}