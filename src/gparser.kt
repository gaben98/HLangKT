package parsing

import kotlin.io.*
import java.io.*

fun parse(filename: String): GVM {
	val file = File(filename)
	val path = file.absolutePath
    val lines = file.readText().replace("[\n\r]".toRegex(), "").split(";").filter { it.isNotEmpty() }
	return GVM(mapOf(*lines.map { parseLine(grammarTokenize(it)) }.toTypedArray()))
}

private fun parseLine(tokens: Array<String>): Pair<String, SubInst> {
	val tag = tokens[0]
	assert(tokens[1] == ":=")
	val tail = tokens.sliceArray(2 until tokens.count())
	val last = Jump(Match())
	return tag to SubInst(parseExpression(tail, tag, last), last)
}

//work right to left, so each inst knows what the next inst is
fun parseExpression(tokens: Array<String>, tag: String, next: Inst): Inst {
	if(tokens.isEmpty()) return next
	if(tokens.count() == 1) return parseAtom(tokens[0], tag, next)
	if(tokens.last() == "?") return parseOption(tokens.sliceArray(0 until tokens.count() - 1), tag, next)
	if(tokens.last() == "*") return parseStar(tokens.sliceArray(0 until tokens.count() - 1), tag, next)
	if(tokens.last() == "+") return parsePlus(tokens.sliceArray(0 until tokens.count() - 1), tag, next)
	//if surrounded by parens, remove them
	val tks = if (tokens.first() == "(" && tokens.last() == ")") tokens.sliceArray(1 until tokens.count()) else tokens
	if(containsNeutral(tks, "|")) return splitAlternation(tks, tag, next)
	return splitConcatenation(tks, tag, next)
}

private fun parseOption(tokens: Array<String>, tag: String, next: Inst): Inst {
	val l1 = parseExpression(tokens, tag, next)
	return Split(l1, next)
}

private fun parseStar(tokens: Array<String>, tag: String, next: Inst): Inst {
	val l1 = Split(null, next)
	val l2 = parseExpression(tokens, tag, Jump(l1))
	l1.x = l2
	return l1
}

private fun parsePlus(tokens: Array<String>, tag: String, next: Inst): Inst {
	val l2 = Split(null, next)
	val l1 = parseExpression(tokens, tag, l2)
	l2.x = l1
	return l1
}

private fun splitConcatenation(tokens: Array<String>, tag: String, next: Inst): Inst {
	val e2 = getTrail(tokens)
	val e1 = tokens.sliceArray(0 until tokens.count()- e2.count())
	val l2 = parseExpression(e2, tag, next)
	if (e1.isEmpty()) return l2
	return splitConcatenation(e1, tag, l2)
}

//returns a pair of first, last token
private fun splitAlternation(tokens: Array<String>, tag: String, next: Inst): Inst {
	//splits tokens into a body and trailing expression
	/*fun iso(tks: Array<String>, lvl: Int, buffer: Array<String>): Pair<Array<String>, Array<String>> {
		if (tks.isEmpty()) return arrayOf<String>() to buffer
		val lst = tks.count()-1
		val last = tks.last()
		if (last == "(") return iso(tks.sliceArray(0 until lst), lvl - 1, arrayOf(last) + buffer)
		if (last == ")") return iso(tks.sliceArray(0 until lst), lvl + 1, arrayOf(last) + buffer)
		if (lvl == 0 && last == "|") return tks to buffer
		return iso(tks.sliceArray(0 until lst), lvl, arrayOf(last) + buffer)
	}*/

	fun splt(body: Array<String>, tail: Array<String>): Array<String> {
		//want to get all trailing tokens after the last pipe
		if(tail.count() == 1 && tail[0] == "|") return arrayOf()
		val ntail = getTrail(body)
		val nbody = body.sliceArray(0 until body.count()-ntail.count())
		return splt(nbody, ntail) + tail
	}

	if(tokens.isEmpty()) return next
	//val (e1, e2) = iso(tokens, 0, arrayOf())//array of token subsets separated by union symbols
	val tail = getTrail(tokens)
	if(tail.count() == tokens.count()) return parseExpression(tail, tag, next)
	val e2 = splt(tokens.sliceArray(0 until tokens.count()-tail.count()), tail)
	val nCount = tokens.count() - e2.count()
	val e1 = tokens.sliceArray(0 until nCount - if(tokens[nCount - 1] == "|") 1 else 0)

	if (e1.isEmpty()) return parseExpression(e2, tag, next)
	//starting from the right-most array of tokens, and the next param,
	//parse the array of tokens into an expr with the nxt param passed to that to become L2
	val l2 = parseExpression(e2, tag, next)
	//recurse and split option on the rest of the tokens, with jmp L3 as next param to become L1
	val l1 = splitAlternation(e1, tag, Jump(next))
	//return a split between L1 and L2
	return Split(l1, l2)
}

private fun parseAtom(token: String, tag: String, next: Inst): Inst {
	//an atom is either a token match or a call to a subexpression
	if(token[0] == '"') return Atom(next, ( '^' + token.substring(1 until token.length - 1) + '$').toRegex(), tag)
    return Reference(next, token)
}

private fun containsNeutral(tokens: Array<String>, token: String): Boolean {
	fun cn(toks: Array<String>, lvl: Int): Boolean {
		if(toks.isEmpty()) return false
		if(toks.first() == "(") return cn(toks.sliceArray(1 until toks.count()), lvl + 1)
		if(toks.first() == "(") return cn(toks.sliceArray(1 until toks.count()), lvl - 1)
		if(lvl == 0 && toks.first() == token) return true
		return cn(toks.sliceArray(1 until toks.count()), lvl)
	}
	return cn(tokens, 0)
}

private fun deparens(tokens: Array<String>): Array<String> {
	fun dp(toks: Array<String>, lvl: Int): Array<String> {
		if(lvl == 0 || toks.isEmpty()) return arrayOf()
		val f = toks.last()
		val n = toks.sliceArray(0 until toks.count() - 1)
		if (f == "(") return dp(n, lvl - 1) + "("
		if (f == ")") return dp(n, lvl + 1) + ")"
		return dp(n, lvl) + arrayOf(f)
	}
	if (tokens.last() == ")") return dp(tokens.sliceArray(0 until tokens.count() - 1), 1) + ")"
	return arrayOf(tokens.last())
}

private fun getTrail(tokens: Array<String>): Array<String> {
	if(tokens.isEmpty()) return arrayOf()
	val last = tokens.last()
	if (arrayOf("?", "+", "*").contains(last)) {
		val n = tokens.sliceArray(0 until tokens.count()-1)
		return deparens(n) + last
	}
	return arrayOf(last)
}

//dp(["asdf", "(", "(", "def", ")", "ghi"], [")"], 1)
// f = "ghi", n = ["asdf", "(", "(", "def", ")"]
//dp([], [], 1) + ["ghi", ")"]