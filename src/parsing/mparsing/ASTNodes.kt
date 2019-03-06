package parsing.mparsing

import core.environment.HState
import core.typesystem.*
import javax.xml.crypto.Data

//top-level abstract classes
abstract class ASTNode {
	abstract fun Execute(state: HState): Pair<HData, HState>
}
abstract class DataNode: ASTNode() {
	abstract fun GetType(state: HState): IHType?
}
abstract class LiteralNode: DataNode()

//basic data types
open class RealNode(val value: Double): LiteralNode() {
	override fun Execute(state: HState): Pair<HPReal, HState> = HPReal(value) to state
	override fun GetType(state: HState): IHType? = HType.HReal
}
open class IntNode(val value: Int): LiteralNode() {
	override fun Execute(state: HState): Pair<HPInt, HState> = HPInt(value) to state
	override fun GetType(state: HState): IHType? = HType.HInt
}
open class BoolNode(val value: Boolean): LiteralNode() {
	override fun Execute(state: HState): Pair<HPBool, HState> = HPBool(value) to state
	override fun GetType(state: HState): IHType? = HType.HBool
}
open class CharNode(val value: Char): LiteralNode() {
	override fun Execute(state: HState): Pair<HPChar, HState> = HPChar(value) to state
	override fun GetType(state: HState): IHType? = HType.HChar
}
open class StringNode(val value: String): LiteralNode() {
	override fun Execute(state: HState): Pair<HPString, HState> = HPString(value) to state
	override fun GetType(state: HState): IHType? = HType.HString
}
open class UnitNode: TupleNode(arrayOf()) {
	override fun GetType(state: HState): IHType? = HType.HUnit
}

//advanced data types
open class IdNode(val identifier: String): DataNode() {
	override fun Execute(state: HState): Pair<HData, HState> = state.Identifiers.getOrDefault(identifier, HPUnit()) to state
	override fun GetType(state: HState): IHType? = state.Identifiers[identifier]?.htype
}
open class TupleNode(val components: Array<DataNode>): DataNode() {
	override fun Execute(state: HState): Pair<HStruct, HState> {
		val fold = components.fold(arrayOf<HData>() to state) { prior, node -> val (dt, nst) = node.Execute(prior.second); prior.first + dt to nst }
		return HStruct(GetType(fold.second)!!, fold.first) to fold.second
	}
	override fun GetType(state: HState): IHType? = TupleType(components.map { it.GetType(state) }.requireNoNulls().toTypedArray())
}

//functional nodes
open class FunctionCallNode(val name: String, val call: TupleNode): DataNode() {
	override fun Execute(state: HState): Pair<HData, HState> {
		val (struct: HStruct, nstate: HState) = call.Execute(state)
		return state.Functions.getValue(name).call(struct.data) to nstate
	}
	override fun GetType(state: HState): IHType? = state.Functions.getValue(name).signature
}

open class UnOpNode(val operator: String, val expr: ExprNode): DataNode() {
	override fun Execute(state: HState): Pair<HData, HState> {
		val (result: HData, nstate: HState) = expr.Execute(state)
		return state.Functions.getValue(operator).call(arrayOf(result)) to nstate
	}
	override fun GetType(state: HState): IHType? = state.Functions.getValue(operator).signature
}
open class BinOpNode(val operator: String, val prefix: ExprNode, val suffix: ExprNode): DataNode() {
	override fun Execute(state: HState): Pair<HData, HState> {
		val (lresult: HData, nst1: HState) = prefix.Execute(state)
		val (rresult: HData, nst2: HState) = suffix.Execute(nst1)
		return state.Functions.getValue(operator).call(arrayOf(lresult, rresult)) to nst2
	}
	override fun GetType(state: HState): IHType? = state.Functions.getValue(operator).signature
}
open class ExprNode(val result: DataNode): DataNode() {
	override fun Execute(state: HState): Pair<HData, HState> = result.Execute(state)
	override fun GetType(state: HState): IHType? = result.GetType(state)
}