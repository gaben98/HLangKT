package parsing.aparsing

import org.junit.Test
import java.util.*
import kotlin.test.*
import parsing.tokenization.*

class test {
    @Test
    fun testVMInst () {
        val l4 = Match()
		val j = Jump(l4)
		val l0 = Atom(expr = "^a$".toRegex(), tag = "A", next = j)
        val l1 = Split(l0, j)
        val l2 = Atom(expr = "^b$".toRegex(), tag = "B", next = j)
        val l3 = Split(l2, j)
        l0.next = l1
        l1.y = l2
        l2.next = l3
		val s = SubInst(l0, j)
        var vm = GVM(mapOf("Program" to s))
        val arr = vm.execute(arrayOf("a", "a", "b"))
        assertEquals<Array<Pair<String, String?>>?>(arrayOf("a" to "A", "a" to "A", "b" to "B"), arr)
    }
    @Test
    fun testTokenization () {
        val tokens = genericTokenize("Ty  pe := Ide\\(ntifier \"h'ah\\\"a lol\"", ":=", eDelims =  " 	", toggles =  "\"'")
        assertEquals(arrayOf(), tokens)
    }
	@Test
	fun testGParse() {
		val vm = parse("src\\hlang.ebnf")
		//val d = deparens(arrayOf("asdf", "(", "(", "def", ")", "ghi", ")"))
		//val p = parseExpression(grammarTokenize("Id | Ham"), "Expr", Match())
		println(Arrays.toString(vm.execute(arrayOf("5", "*", "x"))))
	}
}