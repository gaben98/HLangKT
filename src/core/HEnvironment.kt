package core.environment

import core.typesystem.*
import javax.print.attribute.HashDocAttributeSet
import kotlin.math.sign

open class HState(val Identifiers: Map<String, HData>, val Functions: Map<String, HCallable>/*, val TypeMap: Map<String, HType>*/) {
	companion object {
		fun Init(): HState {
			val ids = mapOf<String, HData>()
			val bangfn = MakeNative(HType.HBool, output = HType.HBool) { input: Array<HPrimitive> -> if(input[0] is HPBool) HPBool(!input[0].GetData<Boolean>()) else HPBool(false) }
			val plusfn = MakeNative(HType.HInt, HType.HInt, output = HType.HInt, func = WrapBinaryInt { a, b -> a + b })
			val timesfn = MakeNative(HType.HInt, HType.HInt, output = HType.HInt, func = WrapBinaryInt { a, b -> a * b })
			val powerfn = MakeNative(HType.HInt, HType.HInt, output = HType.HInt, func = WrapBinaryInt { base, pow -> (1..pow).map { base }.reduce { acc, i -> acc * i } })
			val fns = mapOf("!" to bangfn, "+" to plusfn, "*" to timesfn, "^" to powerfn)
			return HState(ids, fns)
		}

		private fun MakeNative(vararg inputs: IHType, output: IHType = HType.HUnit, func: (Array<HPrimitive>) -> HData): HNative {
			val signature = SignatureType(TupleType(inputs), output)
			return HNative(signature, func)
		}

		private fun WrapBinaryInt(func: (Int, Int) -> Int): (Array<HPrimitive>) -> HData {
			return { if(it[0] is HPInt && it[1] is HPInt) HPInt(func(it[0].GetData(), it[1].GetData())) else HPInt(0) }
		}
	}
}


fun Translate(data: HData): Any {
	return when(data) {
		is HStruct -> {
			data.data.map { Translate(it) }
		}
		is HPBool -> data.data
		is HPReal -> data.data
		is HPInt -> data.data
		is HPChar -> data.data
		is HPString -> data.data
		else -> Unit
	}
}



fun Array<HData>.requireAllPrimitive(): Array<HPrimitive> = this.map { if(it is HPrimitive) it else null }.requireNoNulls().toTypedArray()

/*
fun<T: HData> MakeFunction(vararg inputs: IHType, output: IHType = HType.HUnit, func: (T) -> HData): HFunction {
	val8 tplInputs = TupleType(inputs)
	val signature = SignatureType(tplInputs, output)
	return HFunction(signature, func)

	val tfunc: (HData) -> HData = fun(input: HData): HData {
		when(input) {
			is HStruct -> return HFunction(signature, func)

		}
		val inputValues = (input.data as Array<*>).map { it as HData }
		val params = inputs.zip(inputValues).map { HData(it.component1(), it.component2()) }.toTypedArray()
		return func(params)
	}
	return HFunction(signature, tfunc)
}*/


/*
private fun<I, O> MakeNativeHFunc (signature: SignatureType, func: (I) -> O): HFunction {
	//convert input HData into native data
	//process to get output result
	//convert output result to HData

	val rt: (HData) -> HData = { MakeNativeHData(func(MakeHDataNative(it))) }
	return HFunction(signature, rt)
}

private fun<T> ToNative(data: HData): T {
	return when(data.htype) {
		is TupleType -> {
			if(data.data is Array<*>) {
				return data.data.map { ToNative(it) }
			}
		}
		else -> data.data
	} as T
}

private fun<T> MakeNativeHData (value: T): HData {
	val t: IHType = when(value) {
		is Int -> HType.HInt
		is Double -> HType.HReal
		is Float -> HType.HReal
		is Boolean -> HType.HBool
		is Char -> HType.HChar
		is String -> HType.HString
		is Unit -> TupleType(arrayOf())
		else -> HType.HAny
	}!!
	return HData(t, value)
}*/