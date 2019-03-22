package parsing.mparsing

import core.environment.HState
import core.typesystem.*
import javax.xml.crypto.Data

//top-level abstract classes
abstract class ASTNode {
	abstract fun Execute(state: HState): Pair<HTypedData, HState>
}
abstract class DataNode: ASTNode() {
	abstract fun GetType(state: HState): IHType?
}
abstract class LiteralNode: DataNode()

//basic data types
open class RealNode(val value: Double): LiteralNode() {
	override fun Execute(state: HState): Pair<HTReal, HState> = HTReal(value) to state
	override fun GetType(state: HState): IHType? = HType.HReal
}
open class IntNode(val value: Int): LiteralNode() {
	override fun Execute(state: HState): Pair<HTInt, HState> = HTInt(value) to state
	override fun GetType(state: HState): IHType? = HType.HInt
}
open class BoolNode(val value: Boolean): LiteralNode() {
	override fun Execute(state: HState): Pair<HTBool, HState> = HTBool(value) to state
	override fun GetType(state: HState): IHType? = HType.HBool
}
open class CharNode(val value: Char): LiteralNode() {
	override fun Execute(state: HState): Pair<HTChar, HState> = HTChar(value) to state
	override fun GetType(state: HState): IHType? = HType.HChar
}
open class StringNode(val value: String): LiteralNode() {
	override fun Execute(state: HState): Pair<HTString, HState> = HTString(value) to state
	override fun GetType(state: HState): IHType? = HType.HString
}
open class UnitNode: TupleNode(arrayOf()) {
	override fun GetType(state: HState): TupleType? = HType.HUnit
}

//advanced data types
open class IdNode(val identifier: String): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> = state.Identifiers.getOrDefault(identifier, HTUnit()) to state
	override fun GetType(state: HState): IHType? = state.Identifiers.getValue(identifier).type
}
open class TupleNode(val components: Array<DataNode>): DataNode() {
	override fun Execute(state: HState): Pair<HTStruct, HState> {
		val fold = components.fold(arrayOf<HTypedData>() to state) { prior, node -> val (dt, nst) = node.Execute(prior.second); prior.first + dt to nst }
		return HTStruct(GetType(fold.second)!!, HStruct(fold.first)) to fold.second
	}
	override fun GetType(state: HState): TupleType? = TupleType(components.map { it.GetType(state) }.requireNoNulls().toTypedArray())
}
open class VarDeclNode(val identifier: String, val type: String, val initialValue: DataNode?): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		if(initialValue != null) {
			val (internalData, nstate) = initialValue.Execute(state)
			return internalData to nstate.with(nstate.Identifiers + (identifier to internalData))
		} else {
			val etype = GetType(state)
			val defaultVal: HTypedData = if(etype != null) HTypedData(etype, etype.Default()) else HTUnit()
 			return defaultVal to state.with(state.Identifiers + (identifier to defaultVal))
		}
	}

	override fun GetType(state: HState): IHType? {
		if(type == "var" && initialValue != null) return initialValue.GetType(state)
		return HType.typeMap[type]
	}
}


//functional nodes
open class FunctionCallNode(val name: String, val call: TupleNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val (struct: HTStruct, nstate: HState) = call.Execute(state)
		val fnMatch = nstate.GetFunctionMatch(name, struct.type as TupleType)
		return fnMatch.call(struct.innerStruct.DataArr, nstate)
	}
	override fun GetType(state: HState): IHType? = state.GetFunctionMatch(name, call.GetType(state)!!).signature
}

open class UnOpNode(val operator: String, val expr: ExprNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val (result: HTypedData, nstate: HState) = expr.Execute(state)
		val fnMatch = nstate.GetFunctionMatch(operator, TupleType(arrayOf(result.type)))
		return fnMatch.call(arrayOf(result), nstate)
	}
	override fun GetType(state: HState): IHType? = state.GetFunctionMatch(operator, TupleType(arrayOf(expr.GetType(state)!!))).signature
}
open class BinOpNode(val operator: String, val prefix: ExprNode, val suffix: ExprNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val (lresult: HTypedData, nst1: HState) = prefix.Execute(state)
		val (rresult: HTypedData, nst2: HState) = suffix.Execute(nst1)
		val fnMatch = nst2.GetFunctionMatch(operator, TupleType(arrayOf(lresult.type, rresult.type)))
		return fnMatch.call(arrayOf(lresult, rresult), nst2)
	}
	override fun GetType(state: HState): IHType? = state.GetFunctionMatch(operator, TupleType(arrayOf(prefix.GetType(state)!!, suffix.GetType(state)!!))).signature
}

open class AssignmentNode(val id: String, val valueNode: ExprNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val (result, nstate) = valueNode.Execute(state)
		return result to nstate.with(nstate.Identifiers + (id to result))
	}

	override fun GetType(state: HState): IHType? = state.Identifiers.getValue(id).type
}

open class ExprNode(val result: DataNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> = result.Execute(state)
	override fun GetType(state: HState): IHType? = result.GetType(state)
}