package ir.mimlang.jmim.lang.std.filestream

import com.sun.jdi.Value
import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import org.json.JSONObject

val Converter = object : Variable {
    override val name: String
        get() = "Converter"

    override fun invokeMember(name: String, context: Context): Any? {
        val params = context.getParams()!!
        return when (name) {
            "jsonToMap" -> {
                if (params.isNotEmpty()) {
                    if (params[0] is String) {
                        return JSONObject(params[0] as String).toMap()
                    } else {
                        throw Exception("Input must be String")
                    }
                }else{
                    throw Exception()
                }
            }
            else -> throw Exception()
        }

    }
}