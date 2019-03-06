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
		val tokens = hlangTokenize("2 ^ (2 * 2 + 1)")
		val initialState = HState.Init()
		val parsed = parser.ParseExpression(tokens)
		val result = parsed?.Execute(initialState)
		print(result)
	}
}