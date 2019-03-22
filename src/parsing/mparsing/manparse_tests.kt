package parsing.mparsing

import core.environment.*
import core.typesystem.*
import org.junit.Test
import kotlin.test.*
import parsing.tokenization.*

class ManParseTest {
    @Test
    fun testManParse () {
		val parser = ManualParser()
		val tokens1 = hlangTokenize("var x = (5, \"a string\")")
		//val tokens2 = hlangTokenize("x = x + 4 + y")
		val initialState = HState.Init()
		val l1 = parser.ParseExpression(tokens1)
		//val l2 = parser.ParseExpression(tokens2)
		val (data1, state1) = l1!!.Execute(initialState)
		//val (data2, state2) = l2!!.Execute(state1)
		print(data1)
		//print(data2)
	}
}