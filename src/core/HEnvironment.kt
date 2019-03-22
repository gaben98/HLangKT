package core.environment

import core.typesystem.*
import java.util.*
import kotlin.math.sign

open class HState(val Identifiers: Map<String, HTypedData>, val Functions: Map<String, Array<out HCallable>>, val stack: Stack<StackFrame>) {
	companion object {
		fun Init(): HState {
			val ids = mapOf<String, HTypedData>()
			val bangfn = MakeNative("!", HType.HBool, output = HType.HBool) { input: Array<HPrimitive>, state -> HTBool(!input[0].getValue<Boolean>()) to state }
			val plusfn = MakeNative("+", HType.HInt, HType.HInt, output = HType.HInt, func = WrapBinaryInt { a, b -> a + b })
			val multfn = MakeNative("*", HType.HInt, HType.HInt, output = HType.HInt, func = WrapBinaryInt { a, b -> a * b })
			val powfn  = MakeNative("^", HType.HInt, HType.HInt, output = HType.HInt, func = WrapBinaryInt { base, pow -> (1..pow).map { base }.reduce { acc, i -> acc * i } })
			val strconc = MakeNative("+", HType.HString, HType.HString, output = HType.HString, func = WrapBinaryString { a, b -> a + b })
			val fns = mapOf("!" to arrayOf(bangfn), "+" to arrayOf(plusfn, strconc), "*" to arrayOf(multfn), "^" to arrayOf(powfn))
			return HState(ids, fns, Stack(arrayOf()))
		}

		private fun MakeNative(name: String, vararg inputs: IHType, output: IHType = HType.HUnit, func: (Array<HPrimitive>, HState) -> Pair<HTypedData, HState>): HNative {
			val signature = SignatureType(TupleType(inputs), output)
			return HNative(name, signature, func)
		}

		private fun WrapBinaryInt(func: (Int, Int) -> Int): (Array<HPrimitive>, HState) -> Pair<HTypedData, HState> {
			return { inputArr, state -> HTInt(func(inputArr[0].getValue(), inputArr[1].getValue())) to state }
		}

		private fun WrapBinaryString(func: (String, String) -> String): (Array<HPrimitive>, HState) -> Pair<HTypedData, HState> {
			return { inputArr, state -> HTString(func(inputArr[0].getValue(), inputArr[1].getValue())) to state }
		}
	}
	open fun with(ids: Map<String, HTypedData> = Identifiers, fns: Map<String, Array<out HCallable>> = Functions, stck: Stack<StackFrame> = stack): HState = HState(ids, fns, stck)

	open fun GetFunctionMatch(fn: String, inputs: TupleType): HCallable {
		if(fn !in Functions) throw error("no such function exists")
		val fns = Functions.getValue(fn).filter { inputs.Is(it.signature.inputs) }
		if(fns.count() > 0) return fns[0]//TODO: in the future I need to pick the closest match
		throw error("arguments do not match the function definition")
	}
}

fun Array<HData>.requireAllPrimitive(): Array<HPrimitive> = this.map { if(it is HPrimitive) it else null }.requireNoNulls().toTypedArray()


data class StackFrame(var result: HTypedData)

//immutable stack
open class Stack<K>(val arr: Array<K>) {
	open fun push(elem: K): Stack<K> {
		return Stack(arr + elem)
	}

	open fun pop(): Pair<K, Stack<K>> {
		val nArr = arr.sliceArray(0 until arr.lastIndex)
		return peek() to Stack(nArr)
	}

	open fun peek(): K {
		return arr[arr.lastIndex]
	}
}