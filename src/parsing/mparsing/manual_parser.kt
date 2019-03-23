package parsing.mparsing

import com.sun.org.apache.xpath.internal.operations.Bool
import core.typesystem.HType
import parsing.tokenization.lselect
import parsing.tokenization.lsplit
import javax.xml.crypto.Data

open class ManualParser {
	open fun ParseScope(tokens: Array<String>): ScopeNode? {
		if(tokens.first() == "{" && tokens.last() == "}") {
			if(tokens.count() == 2) return ScopeNode(arrayOf())
			val internalSplit = lsplit(tokens.sliceArray(1 until tokens.lastIndex), "{", "}", arrayOf(";", "\n"))
			return ScopeNode(internalSplit.map { ParseExpression(it) }.toTypedArray().requireNoNulls())
		}
		return null
	}

	open fun ParseExpression(tokens: Array<String>): ExprNode? {
		if(tokens.count() == 1) {
			val data = tryOptions<DataNode>(tokens, ::ParseLiteral, ::ParseIdentifier)
			if(data != null) return ExprNode(data)
		}
		if(tokens.count { it == "(" } != tokens.count { it == ")" }) return null//if parens are imbalanced, def not an expression
		/*if(tokens.last() == "]") {
			val accessNode = ParseAccess(tokens)
			if(accessNode != null) return accessNode
		}*/
		val data = tryOptions<DataNode>(tokens, ::ParseWhile, ::ParseIf, ::ParseElse, ::ParseVariableDeclaration, ::ParseVariableAssignment, ::ParseAccess, ::ParseLiteral, ::ParseBinOp, ::ParseUnOp, ::ParseTuple, ::ParseGroup, ::ParseFunctionCall)
		if(data is ExprNode) return data
		if(data != null) return ExprNode(data)
		return null
	}

	fun ParseExprOrScope(tokens: Array<String>): DataNode? = if(tokens.first() == "{") ParseScope(tokens) else ParseExpression(tokens)

	open fun ParseIf(tokens: Array<String>): IfNode? {
		if(tokens.count() > 4 && tokens.first() == "if" && tokens[1] == "(") {
			val conditionToks = lselect(tokens.sliceArray(2..tokens.lastIndex), "(", ")")
			val condNode = ParseExpression(conditionToks)
			if(condNode != null) {
				val tailToks = tokens.sliceArray(3 + conditionToks.count()..tokens.lastIndex)
				if(tailToks.contains("else")) {
					val exprToks = tailToks.sliceArray(0 until tailToks.indexOf("else"))
					val elseToks = tailToks.sliceArray(tailToks.indexOf("else") + 1..tailToks.lastIndex)
					val exprNode = ParseExpression(exprToks)
					val elseNode = ParseExpression(elseToks)
					if(exprNode != null && elseNode != null) return IfNode(condNode, exprNode, elseNode)
				}
				val tailNode = ParseExprOrScope(tailToks)
				if(tailNode != null) return IfNode(condNode, tailNode, null)
			}
		}
		return null
	}

	open fun ParseElse(tokens: Array<String>): ElseNode? {
		if(tokens.count() > 1 && tokens.first() == "else") {
			val tailToks = tokens.sliceArray(1..tokens.lastIndex)
			val tailNode = ParseExprOrScope(tailToks)
			if(tailNode != null) return ElseNode(tailNode)
		}
		return null
	}

	open fun ParseWhile(tokens: Array<String>): WhileNode? {
		if(tokens.count() > 4 && tokens.first() == "while" && tokens[1] == "(") {
			val condToks = lselect(tokens.sliceArray(2..tokens.lastIndex), "(", ")")
			val condNode = ParseExpression(condToks)
			if(condNode != null) {
				val tailToks = tokens.sliceArray(3+condToks.count()..tokens.lastIndex)
				val scopeNode = ParseExprOrScope(tailToks)
				if(scopeNode != null) return WhileNode(condNode, scopeNode)
			}
		}
		return null
	}

	open fun ParseAccess(tokens: Array<String>): ExprNode? {
		if(tokens.count() == 0 || tokens.last() != "]") return null
		val bracketIndex = tokens.indexOf("[")
		if(bracketIndex == -1) return null
		val elementToks = tokens.sliceArray(0 until bracketIndex)
		val accessToks = tokens.sliceArray(bracketIndex..tokens.lastIndex)
		val indexExpr = ParseExpression(accessToks.sliceArray(1 until accessToks.lastIndex))
		if(indexExpr != null) {
			if (elementToks.count() == 1) return ExprNode(AccessNode(IdNode(elementToks[0]), indexExpr))
			else {
				val dataNode = ParseExpression(elementToks)
				if(dataNode != null) return ExprNode(AccessNode(dataNode, indexExpr))
			}
		}
		return null
	}

	open fun ParseLiteral(tokens: Array<String>): LiteralNode? {
		return tryOptions<LiteralNode>(tokens, ::ParseInt, ::ParseReal, ::ParseBool, ::ParseChar, ::ParseString, ::ParseArray)
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
			if(tokens[0].length == 3 && tokens[0][0] == '\'' && tokens[0][2] == '\'') return CharNode(
				tokens[0][1]
			)
		}
		return null
	}

	open fun ParseString(tokens: Array<String>): StringNode? {
		if(tokens.count() == 1) {
			if(tokens[0].length > 1 && tokens[0][0] == '"' && tokens[0].last() == '"') return StringNode(
				tokens[0].substring(1 until tokens[0].lastIndex)
			)
		}
		return null
	}

	open fun ParseArray(tokens: Array<String>): ArrayNode? {
		if(tokens.count() >= 2 && tokens.first() == "<" && tokens.last() == ">") {
			val elements = lsplit(tokens.sliceArray(1 until tokens.lastIndex), "<", ">", arrayOf(","))
			val elemNodes = elements.map { ParseExpression(it) }
			val noNullNodes = elemNodes.requireNoNulls().toTypedArray()
			if(elemNodes.count() == elemNodes.count()) return ArrayNode(noNullNodes)
		}
		return null
	}

	open fun ParseTuple(tokens: Array<String>): TupleNode? {
		if(tokens.count() == 2 && tokens[0] == "(" && tokens[1] == ")") return UnitNode()
		if(tokens.count() > 1 && tokens[0] == "(" && tokens.last() == ")") {
			val split = lsplit(tokens.sliceArray(1 until tokens.lastIndex), "(", ")", arrayOf(","))
			if(split.count() == 1) return null
			val exprs = split.map { ParseExpression(it) }.toTypedArray()
			val nexprs = exprs.requireNoNulls().map { it as DataNode }.toTypedArray()
			if(exprs.count() == nexprs.count()) return TupleNode(nexprs)
		}
		return null
	}

	open fun ParseGroup(tokens: Array<String>): ExprNode? {
		if(tokens.count() > 1 && tokens[0] == "(" && tokens.last() == ")") {
			val innerToks = tokens.sliceArray(1 until tokens.lastIndex)
			return ParseExpression(innerToks)
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

	open fun ParseVariableDeclaration(tokens: Array<String>): VarDeclNode? {
		if(tokens.count() > 1) {
			val type = tokens[0]
			val id = tokens[1]
			if(!IsValidID(id) || !IsValidID(type)) return null
			if(id == "result") throw error("result is a reserved keyword and cannot be used as a variable identifier")
			if(type == "var" && tokens.count() == 2) return null//var requires some expression for initial value

			if(tokens.count() > 2 && tokens[2] == "=") {
				val result = ParseExpression(tokens.sliceArray(3..tokens.lastIndex))
				if(result != null) return VarDeclNode(id, type, result)
				return null
			}

			if(tokens.count() == 2) return VarDeclNode(id, type, null)
		}
		return null
	}

	open fun ParseVariableAssignment(tokens: Array<String>): AssignmentNode? {
		if(tokens.count() > 2) {
			if(tokens[1] != "=") return null
			if(!IsValidID(tokens[0])) return null
			val expr = ParseExpression(tokens.sliceArray(2..tokens.lastIndex)) ?: return null
			return AssignmentNode(tokens[0], expr)
		}
		return null
	}

	private fun IsValidID(token: String): Boolean = "[A-z_][A-z0-9_]*".toRegex().matches(token)

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
		val bins = arrayOf("^", "*", "/", "+", "-", "<", "<=", ">", ">=", "&", "&&", "|", "||", "==")
		for(bin in bins.reversed()) {
			val binIndices = tks.mapIndexed { index: Int, s: String -> if (s == bin && index != 0 && index != tks.lastIndex) index else null }.filterNotNull()
			for(index in binIndices) {
				val prefix = ParseExpression(tks.sliceArray(0 until index))
				val suffix = ParseExpression(tks.sliceArray(index+1..tks.lastIndex))
				if(prefix != null && suffix != null) return BinOpNode(tks[index], prefix, suffix)
			}
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