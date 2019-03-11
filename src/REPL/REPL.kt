package REPL

import java.io.File
import kotlin.io.*
import execution.*

fun main(args: Array<String>) {
	//print()
	val code = File(args[0])
	print(Execute(code.readText()))
}