package parsing.tokenization

//iDelims: included delims
//eDelims: excluded delims
fun genericTokenize(text: String, vararg iDelims: String, eDelims: String, toggles: String): Array<String> {
    fun pcont(txt: String, buffer: String, tgl: Char?, escaping: Boolean): Array<String> {
		if (txt.length == 1) {
			if(iDelims.any { txt.startsWith(it) }) return if (buffer.isNotEmpty()) arrayOf(buffer, txt) else arrayOf(txt)
			if(!eDelims.contains(txt)) return arrayOf(buffer + txt)
		}
        val cur = txt[0]
		val next = txt.substring(1)
		if(cur == '\\') {
			return if (escaping) arrayOf("\\") else arrayOf<String>() + pcont(next, if (escaping) "" else buffer, tgl, !escaping)
		} else if (escaping) {
			// \. in quotes should still be \.
			//
			return pcont(next, buffer + "\\" + cur, tgl, false)
		} else {
			//if tgl is null, I'm not in a toggle group.  If it is a character, that character if non-escaped can close the toggle group
			if (tgl == null) {//not in a toggle, handle normal cases
				val buf = if (buffer.isNotEmpty()) arrayOf(buffer) else arrayOf()
				for (d in iDelims) {
					if (txt.startsWith(d)) {
						return buf + arrayOf(d) + pcont(txt.substring(d.length), "", tgl, false)
					}
				}
				//if(iDelims.any { txt.startsWith(it) }) return buf + arrayOf(cur.toString()) + pcont(next, "", null, escaping)
				if(eDelims.contains(cur)) return buf + pcont(next, "", null, escaping)
				if(toggles.contains(cur)) return buf + pcont(next, cur.toString(), cur, escaping)
				return pcont(next, buffer + cur, null, escaping)
			} else {//in a toggle, either toggle closes or a new char is added to the buffer
				return if(cur == tgl) arrayOf(buffer + cur) + pcont(next, "", null, escaping)
				else pcont(next, buffer + cur, tgl, escaping)
			}
		}
    }
    return pcont(text, "", null, false)
}

fun grammarTokenize(text: String): Array<String> = genericTokenize(text, *("()[]+*|?;".map { it.toString() }.toTypedArray() + arrayOf(":=")), eDelims = " 	", toggles =  "\"")

fun hlangTokenize(text: String): Array<String> = genericTokenize(text, *"()[]<>+*|?!;,".map { it.toString() }.toTypedArray(), eDelims = " 	", toggles = "\"")

//splits tokens but only splits at the base level
fun lsplit(tokens: Array<String>, opener: String, closer: String, splitters: Array<String>): Array<Array<String>> {
	fun lsp(tks: Array<String>, lvl: Int, buffer: Array<String>): Array<Array<String>> {
		if(tks.isEmpty()) return arrayOf(buffer)
		val tail = tks.sliceArray(1..tks.lastIndex)
		return when(tks[0]) {
			opener -> lsp(tail, lvl+1, buffer + if(lvl == 0) arrayOf(opener) else arrayOf())
			closer -> lsp(tail, lvl-1, buffer + if(lvl == 1) arrayOf(closer) else arrayOf())
			in splitters -> {
				if(lvl == 0) arrayOf(buffer) + lsp(tail, lvl, arrayOf())
				else lsp(tail, lvl, buffer + tks[0])
			}
			else -> lsp(tail, lvl, buffer + tks[0])
		}
	}
	return lsp(tokens, 0, arrayOf())
}

//selects the most tokens at the base level until that level is closed.  Thus,
//lselect(["a", "(", "B", ")", ")"], "(", ")") returns ["a", "(", "B", ")"]
fun lselect(tokens: Array<String>, opener: String, closer: String): Array<String> {
	fun lslct(tks: Array<String>, lvl: Int, buffer: Array<String>): Array<String> {
		if(tks.isEmpty() || lvl < 0) return buffer
		val tail = tks.sliceArray(1..tks.lastIndex)
		return when(tks[0]) {
			opener -> lslct(tail, lvl+1, buffer + opener)
			closer -> lslct(tail, lvl-1, if(lvl > 0) buffer + closer else buffer)
			else -> lslct(tail, lvl, buffer + tks[0])
		}
	}

	return lslct(tokens, 0, arrayOf())
}