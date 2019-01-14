package parsing

import kotlin.text.Regex

sealed class Inst
open class Atom(var next: Inst?, val expr: Regex, val tag: String?): Inst()
open class Match: Inst()
open class Jump(var next: Inst?): Inst()
open class Split(var x: Inst?, var y: Inst?): Inst()
open class Reference(var next: Inst?, val tag: String): Inst()
open class SubInst(val start: Inst, val last: Jump): Inst()