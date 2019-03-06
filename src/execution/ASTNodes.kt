package execution

import core.typesystem.*
import java.net.ContentHandler

abstract class ASTNode(private val evaluator: () -> Result) {
	fun eval(): Result = evaluator()
}

data class Result(val data: HData)
/*
class StrNode(content: String): ASTNode({ Result(HData(htypes["string"], content)) })
class ChrNode(content: Char): ASTNode({ Result(HData(htypes["char"], content)) })
class IntNode(content: Int): ASTNode({ Result(HData(htypes["int"], content))})
class RealNode(content: Float): ASTNode({ Result(HData(htypes["real"], content))})
*/
class LiteralNode(child: ASTNode): ASTNode(child::eval)



//class Expr(val children: Array<ASTNode>, option: Int): ASTNode("Expr", option) {
	/*override fun Eval(): Result {
		return when (option) {
			in 0..2 -> children[0].Eval()
			3 -> //apply unary operater to child 1, treat it as a method call
			else -> this
		}
	}*/
//}