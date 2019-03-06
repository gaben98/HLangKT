package parsing.aparsing

import kotlin.collections.*

open class GVM(private val expressions: Map<String, SubInst>) {
	open fun execute(tokens: Array<String>): Array<Pair<String, String?>>? = execute(tokens, "Program")
    open fun execute(tokens: Array<String>, startLexeme: String): Array<Pair<String, String?>>? {
		c.enqueue(expressions[startLexeme]!!.start to arrayOf())
		for(token in tokens) {
            for((inst, arr) in c) {
                when (inst) {
                    is Atom -> if (inst.expr.matches(token)) n.enqueue(inst.next to arr.plusElement(inst.tag))
                    is Match -> return tokens.zip(arr).toTypedArray()
                    is Jump -> c.enqueue(inst.next to arr)
                    is Split -> {
                        c.enqueue(inst.x to arr)
                        c.enqueue(inst.y to arr)
                    }
					is Reference -> {
						val nInst = expressions[inst.tag]
						nInst!!.last.next = inst.next
						c.enqueue(nInst.start to arr)
					}
				}
            }
			swap()
        }
        for((inst, arr) in c) {
            when (inst) {
				is Atom -> return null
				is Match -> return tokens.zip(arr).toTypedArray()
				is Jump -> c.enqueue(inst.next to arr)
				is Split -> {
					c.enqueue(inst.x to arr)
					c.enqueue(inst.y to arr)
				}
				is Reference -> {
					val nInst = expressions[inst.tag]
					nInst!!.last.next = inst.next
					c.enqueue(nInst.start to arr)
				}
            }
        }
        return null
    }

    companion object State {
        var c = Queue<Pair<Inst?, Array<String?>>>()
        var n = Queue<Pair<Inst?, Array<String?>>>()
        fun swap () {
            for(i in n) c.enqueue(i)
        }
    }
}

open class Queue<T> {
    private var items: MutableList<T> = mutableListOf()
    open fun enqueue(elem: T) = items.add(elem)
    open fun dequeue(): T = items.removeAt(0)
    open fun peek(): T = items[0]
    open fun count(): Int = items.size
    open fun isEmpty(): Boolean = items.isEmpty()
    operator fun iterator(): Iterator<T> = object : Iterator<T> {
        override fun hasNext(): Boolean = !isEmpty()
        override fun next(): T = dequeue()
    }
}