package parsing.mparsing

import com.sun.org.apache.xpath.internal.operations.Bool
import core.environment.HState
import core.environment.StackFrame
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
	override fun GetType(state: HState): IHType = HType.HReal
}
open class IntNode(val value: Int): LiteralNode() {
	override fun Execute(state: HState): Pair<HTInt, HState> = HTInt(value) to state
	override fun GetType(state: HState): IHType = HType.HInt
}
open class BoolNode(val value: Boolean): LiteralNode() {
	override fun Execute(state: HState): Pair<HTBool, HState> = HTBool(value) to state
	override fun GetType(state: HState): IHType = HType.HBool
}
open class CharNode(val value: Char): LiteralNode() {
	override fun Execute(state: HState): Pair<HTChar, HState> = HTChar(value) to state
	override fun GetType(state: HState): IHType = HType.HChar
}
open class StringNode(val value: String): LiteralNode() {
	override fun Execute(state: HState): Pair<HTString, HState> = HTString(value) to state
	override fun GetType(state: HState): IHType = HType.HString
}
open class UnitNode: TupleNode(arrayOf()) {
	override fun GetType(state: HState): TupleType = HType.HUnit
}
open class ArrayNode(val elements: Array<out DataNode>): LiteralNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val (dataArr: Array<HTypedData>, fstate: HState) = elements.fold(arrayOf<HTypedData>() to state) {(dtArr, state), dtNode -> val (result, nstate) = dtNode.Execute(state); dtArr + result to nstate }
		return HTStruct(GetType(state), HStruct(dataArr)) to fstate
	}
		override fun GetType(state: HState): IHType = ArrayType(GetCommonType(*(elements.map { it.GetType(state) }.requireNoNulls().toTypedArray())))
}


//advanced data types
open class IdNode(val identifier: String): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		if(identifier == "result") return state.stack.peek().result to state
		return state.Identifiers.getOrDefault(identifier, HTUnit()) to state
	}
	override fun GetType(state: HState): IHType? = state.Identifiers.getValue(identifier).type
}
open class TupleNode(val components: Array<DataNode>): DataNode() {
	override fun Execute(state: HState): Pair<HTStruct, HState> {
		val fold = components.fold(arrayOf<HTypedData>() to state) { prior, node -> val (dt, nst) = node.Execute(prior.second); prior.first + dt to nst }
		return HTStruct(GetType(fold.second), HStruct(fold.first)) to fold.second
	}
	override fun GetType(state: HState): TupleType = TupleType(components.map { it.GetType(state) }.requireNoNulls().toTypedArray())
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

open class AccessNode(val indexable: DataNode, val accessIndex: ExprNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val (indexResult, nstate) = accessIndex.Execute(state)
		val (indexData, fstate) = indexable.Execute(nstate)
		if(indexResult is HTInt) {
			val index = indexResult.intdata
			if(indexData is HTStruct) {
				return indexData.innerStruct.DataArr[index] to nstate
			} else if(indexData is HTString) {
				return HTChar(indexData.stringdata[index]) to nstate
			} else throw error("tried to access a non-indexed variable")
		}
		throw error("index did not evaluate to an integer")
	}

	override fun GetType(state: HState): IHType? {
		val indexableType = indexable.GetType(state)
		if(indexableType is ArrayType) return indexableType.elementType
		if(indexableType is TupleType) return indexableType.componentTypes[(accessIndex.Execute(state).first as HTInt).intdata]
		return null
	}
}

open class IfNode(val condition: ExprNode, val scope: DataNode, val elseClause: DataNode?): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val (condResult, nstate) = condition.Execute(state)
		if(condResult is HTBool) {
			if(condResult.booldata) {
				val (scopeResult, fstate) = scope.Execute(nstate)
				return scopeResult to fstate.withResult(scopeResult)
			} else {
				if(elseClause != null) return elseClause.Execute(nstate)
				return HTFailure() to nstate.withResult(HTFailure())
			}
		} else throw error("condition must return a boolean value")
	}
	override fun GetType(state: HState): IHType? = scope.GetType(state)
}

open class ElseNode(val scope: DataNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val runningVal = state.stack.peek().result
		return if(runningVal is HTFailure) scope.Execute(state) else runningVal to state//if not a failure, carry on as if this node was never reached
	}
	override fun GetType(state: HState): IHType? = scope.GetType(state)
}

open class WhileNode(val condition: ExprNode, val scope: DataNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		fun runScope(inState: HState): HState {
			val (scopeResult, afterState) = scope.Execute(inState)
			return afterState.withResult(scopeResult)
		}

		fun checkCondition(inState: HState): Pair<Boolean, HState> {
			val (condResult, condState) = condition.Execute(inState)
			if(condResult is HTBool) return condResult.booldata to condState
			throw error("condition did not evaluate to a boolean value")
		}

		fun runWhile(inState: HState, iteration: Int): Pair<HTypedData, HState> {
			val (cond, nstate) = checkCondition(inState)
			if(cond) {
				val scopeResultState = runScope(nstate)
				return runWhile(scopeResultState, iteration + 1)
			}
			if(iteration == 0) return HTFailure() to inState
			return inState.stack.peek().result to inState
		}

		return runWhile(state, 0)
	}

	override fun GetType(state: HState): IHType? = scope.GetType(state)
}

open class ExprNode(val result: DataNode): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> = result.Execute(state)
	override fun GetType(state: HState): IHType? = result.GetType(state)
}

open class ScopeNode(val steps: Array<out DataNode>): DataNode() {
	override fun Execute(state: HState): Pair<HTypedData, HState> {
		val scopestate = state.push()

		 val finalState = steps.fold(scopestate) { oldState, dataNode ->
			val (result, nstate) = dataNode.Execute(oldState)
			nstate.withResult(result)
		}

		return finalState.stack.peek().result to finalState.pop()
	}
	override fun GetType(state: HState): IHType? = steps[steps.lastIndex].GetType(state)
}