package mparsing

import parsing.tokenization.lsplit
import javax.xml.crypto.Data

open class ManualParser {
	open fun ParseExpression(tokens: Array<String>): ExprNode? {
		if(tokens.count() == 1) {
			val data = tryOptions<DataNode>(tokens, ::ParseLiteral, ::ParseIdentifier)
			if(data != null) return ExprNode(data)
		}
		if(tokens.count { it == "(" } != tokens.count { it == ")" }) return null//if parens are imbalanced, def not an expression
		val data = tryOptions<DataNode>(tokens, ::ParseBinOp, ::ParseUnOp, ::ParseTuple, ::ParseFunctionCall)
		if(data != null) return ExprNode(data)
		return null
	}

	open fun ParseLiteral(tokens: Array<String>): LiteralNode? {
		return tryOptions<LiteralNode>(tokens, ::ParseInt, ::ParseReal, ::ParseBool, ::ParseChar, ::ParseString)
	}

	open fun ParseIdentifier(tokens: Array<String>): IdNode? {
		if(tokens.count() == 1) {
			val r = "[A-z][A-z0-9]*".toRegex()
			if (r.matches(tokens[0])) return IdNode(tokens[0])
		}
		return null
	}

	open fun ParseReal(tokens: Array<String>): RealNode? {
		if(tokens.count() == 1) {
			tokens[0].toDoubleOrNull()?.let { return RealNode(it) }
		}
		return null
	}

	open fun ParseInt(tokens: Array<String>): IntNode? {
		if(tokens.count() == 1) {
			tokens[0].toIntOrNull()?.let { return IntNode(it) }
		}
		return null
	}

	open fun ParseBool(tokens: Array<String>): BoolNode? {
		if(tokens.count() == 1) {
			if(tokens[0] == "true") return BoolNode(true)
			if(tokens[0] == "false") return BoolNode(false)
		}
		return null
	}

	open fun ParseChar(tokens: Array<String>): CharNode? {
		if(tokens.count() == 1) {
			if(tokens[0].length == 3 && tokens[0][0] == '\'' && tokens[0][2] == '\'') return CharNode(tokens[0][1])
		}
		return null
	}

	open fun ParseString(tokens: Array<String>): StringNode? {
		if(tokens.count() == 1) {
			if(tokens[0].length > 1 && tokens[0][0] == '"' && tokens[0].last() == '"') return StringNode(tokens[0].substring(1 until tokens[0].lastIndex))
		}
		return null
	}

	open fun ParseTuple(tokens: Array<String>): TupleNode? {
		if(tokens.count() > 1 && tokens[0] == "(" && tokens.last() == ")") {
			val split = lsplit(tokens.sliceArray(1 until tokens.lastIndex), "(", ")", ",")
			val exprs = split.map { ParseExpression(it) }.toTypedArray()
			val nexprs = exprs.requireNoNulls().map { it as DataNode }.toTypedArray()
			if(exprs.count() == nexprs.count()) return TupleNode(nexprs)
		}
		return null
	}

	open fun ParseFunctionCall(tokens: Array<String>): FunctionCallNode? {
		if(tokens.count() > 2 && tokens[1] == "(" && tokens.last() == ")") {
			val tuple = ParseTuple(tokens.sliceArray(1..tokens.lastIndex))
			if(tuple != null) return FunctionCallNode(tokens[0], tuple)
		}
		return null
	}

	open fun ParseUnOp(tks: Array<String>): UnOpNode? {
		fun UnOpExpr(tokens: Array<String>, suffix: ExprNode): UnOpNode? {
			if(tokens.count() == 1) {
				if("!".toRegex().matches(tokens[0])) return UnOpNode(tokens[0], suffix)
			}
			return null
		}
		val suffix = ParseExpression(tks.sliceArray(1..tks.lastIndex))
		if (suffix != null) {
			return UnOpExpr(tks.sliceArray(0 until 1), suffix)
		}
		return null
	}

	open fun ParseBinOp(tks: Array<String>): BinOpNode? {
		val bins = arrayOf("+", "-", "*", "/", "**", "<", "<=", ">", ">=", "|", "||", "&", "&&", "==")
		val binIndices = tks.mapIndexed { index: Int, s: String -> if (bins.contains(s)) index else null  }.filterNotNull()
		for(index in binIndices) {
			val prefix = ParseExpression(tks.sliceArray(0 until index))
			val suffix = ParseExpression(tks.sliceArray(index+1..tks.lastIndex))
			if(prefix != null && suffix != null) return BinOpNode(tks[index], prefix, suffix)
		}
		return null
	}

	private inline fun<reified T> tryOptions(tokens: Array<String>, vararg options: (Array<String>) -> ASTNode?): T? {
		for (evaluator in options) {
			val result = evaluator(tokens)
			if(result != null) return result as T?
		}
		return null
	}
}

open class ASTNode
open class DataNode: ASTNode()//node that can be interpreted as data
open class LiteralNode: DataNode()//node that holds a literal
open class RealNode(val value: Double): LiteralNode()
open class IntNode(val value: Int): LiteralNode()
open class BoolNode(val value: Boolean): LiteralNode()
open class CharNode(val value: Char): LiteralNode()
open class StringNode(val value: String): LiteralNode()
open class IdNode(val identifier: String): DataNode()
open class TupleNode(val components: Array<DataNode>): DataNode()
open class FunctionCallNode(val name: String, val call: TupleNode): DataNode()
open class UnOpNode(val operator: String, val expr: ExprNode): DataNode()
open class BinOpNode(val operator: String, val prefix: ExprNode, val suffix: ExprNode): DataNode()
open class ExprNode(val result: DataNode): DataNode()