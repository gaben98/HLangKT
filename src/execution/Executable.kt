package execution

import core.environment.HState
import core.typesystem.HData
import core.typesystem.HPUnit
import parsing.mparsing.ManualParser
import parsing.tokenization.hlangTokenize

fun Execute(code: String): HData {
	val state = HState.Init()
	val tokens = hlangTokenize(code)
	val parser = ManualParser()
	val parsed = parser.ParseExpression(tokens)
	return if(parsed != null) return parsed.Execute(state).first
	else HPUnit()
}