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
		val tokens1 = hlangTokenize("var x = <5, 7, 3>")
		val tokens2 = hlangTokenize("int y = x[0]")
		val tokens3 = hlangTokenize("while(y < 7) { y = y + 1 }")
		val initialState = HState.Init()
		val l1 = parser.ParseExpression(tokens1)
		val l2 = parser.ParseExpression(tokens2)
		val l3 = parser.ParseExpression(tokens3)
		val (data1, state1) = l1!!.Execute(initialState)
		val (data2, state2) = l2!!.Execute(state1)
		val (data3, state3) = l3!!.Execute(state2)
		//val selection = parser.ParseIf(hlangTokenize("if(x + (y - 5) <= 5) 5"))
		print(data1)
		print(data2)
		print(data3)
	}
}