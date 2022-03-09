package ir.mimlang.jmim.lang.std

import ir.mimlang.jmim.lang.ctx.Context
import ir.mimlang.jmim.lang.ctx.Variable
import ir.mimlang.jmim.lang.util.ext.then
import org.w3c.dom.ranges.Range

class ValueVariable(
        override val name: String,
        private var value: Any? = null,
        private val isConst: Boolean = false
) : Variable {
    override fun getValue(): Any? = value
    override fun setValue(value: Any?) {
        isConst then { constThrow() }

        this.value = value
    }

    override fun decrement(returnBeforeJob: Boolean): Any? {
        isConst then { constThrow() }

        val initialValue = value

        value = when (value) {
            is Long -> (value as Long).dec()
            is Double -> (value as Double).dec()
            else -> throw UnsupportedOperationException("operand of -- operator must be number")
        }

        return if (returnBeforeJob) initialValue else value
    }

    override fun increment(returnBeforeJob: Boolean): Any? {
        isConst then { constThrow() }

        val initialValue = value

        value = when (value) {
            is Long -> (value as Long).inc()
            is Double -> (value as Double).inc()
            else -> throw UnsupportedOperationException("operand of ++ operator must be number")
        }

        return if (returnBeforeJob) initialValue else value
    }

    override fun getProperty(name: String): Any {
        when (name) {
            "size" -> {
                return (value as? Collection<*>)?.size
                        ?: throw UnsupportedOperationException("property 'size' exists only for lists")
            }

            else -> throw IllegalArgumentException("property $name doesn't exist on ${this.name}")
        }
    }

    override fun invoke(context: Context): Any? = (value as? FunctionVariable)?.invoke(context) ?: super.invoke(context)
    override fun invokeMember(name: String, context: Context): Any? {
        return when (name) {
            "invoke" -> invoke(context)

            "remove" -> {
                val getParam = context.getParams()!!
                when (value) {
                    is List<*> -> (value as MutableList<Any?>).apply { remove((getParam[0])) }
                    is String -> value = (value as String).replace(getParam[0] as String, "")
                    is Map<*, *> -> (value as MutableMap<Any?, Any?>).apply { remove(getParam[0]) }
                    else -> throw UnsupportedOperationException()
                }
            }

            "removeAt" -> {
                val getParam = context.getParams()!!
                when (value) {
                    is List<*> -> (value as MutableList<Any?>).apply { removeAt((getParam[0] as Long).toInt()) }
                    is String -> value = (value as String).removeRange(IntRange((getParam[0] as Long).toInt(), (getParam[0] as Long).toInt()))
                    else -> throw UnsupportedOperationException()
                }
            }

            "add" -> {
                val getParam = context.getParams()!!
                when (value) {
                    is List<*> -> (value as MutableList<Any?>).apply { add(getParam[0]) }
                    is String -> value = (value as String) + getParam[0]
                    is Map<*, *> -> (value as MutableMap<Any?, Any?>).apply { put(getParam[0] as String, getParam[1] as String) }
                    else -> throw UnsupportedOperationException()
                }
            }


            "insert" -> {
                val getParam = context.getParams()!!
                if (getParam.count() != 2) {
                    throw Exception("Must Input two arguments");
                }
                if (getParam[0] !is Long) {
                    throw IllegalArgumentException()
                }
                when (value) {
                    is List<*> -> (value as MutableList<Any?>).apply { add((getParam[0] as Long).toInt(), getParam[1]) }
                    is String -> value = StringBuilder(value as String).apply { insert((getParam[0] as Long).toInt(), getParam[1] as String) }
                    else -> throw UnsupportedOperationException()
                }
            }

            "insertCopy" -> {
                val getParam = context.getParams()!!

                if (getParam.count() != 2) {
                    throw Exception("Must Input two arguments");
                }
                if (getParam[0] !is Long) {
                    throw IllegalArgumentException()
                }

                when (value) {
                    is List<*> -> (value as List<Any?>).toMutableList().apply { add((getParam[0] as Long).toInt(), getParam[1]) }
                    is String -> StringBuilder(value as String).insert((getParam[0] as Long).toInt(), getParam[1] as String)
                    else -> throw UnsupportedOperationException()
                }
            }


            "get" -> {
                val getParam = context.getParams()!!.single() // todo make better

                when (value) {
                    is List<*> -> (value as List<*>)[(getParam as Long).toInt()]
                    is Map<*, *> -> (value as Map<*, *>)[getParam as String]
                    is String -> (value as String)[(getParam as Long).toInt()]

                    else -> throw UnsupportedOperationException("property 'size' exists only for lists")
                }
            }

            else -> throw IllegalArgumentException("method $name doesn't exist on ${this.name}")
        }
    }

    private fun constThrow(): Nothing = throw UnsupportedOperationException("cannot change the value of constant $name")
}