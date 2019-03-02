package mparsing

import org.junit.Test
import java.util.*
import kotlin.test.*
import parsing.tokenization.*

class ManParseTest {
    @Test
    fun testManParse () {
		val parser = ManualParser()
		val tokens = hlangTokenize("(my, \"friend\", 63, f(3, 5))")
		val result = parser.ParseExpression(tokens)
		assertEquals(result, null)
	}
}