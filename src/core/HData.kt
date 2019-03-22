package core.typesystem

import core.environment.HState
import core.environment.requireAllPrimitive

abstract class HData(var Data: Any?) {
	override fun toString(): String = Data.toString()
}
open class HTypedData(var type: IHType, var data: HData) {
	override fun toString(): String = "$type: $data"
}
abstract class HPrimitive(data: Any?): HData(data) {
	fun<D> getValue(): D = Data as D
	override fun toString(): String = "$Data"
}


class HStruct(var DataArr: Array<HTypedData>): HData(DataArr) {
	override fun toString(): String = "(" + DataArr.joinToString(", ") + ")"
}
class HTStruct(type: IHType, val innerStruct: HStruct): HTypedData(type, innerStruct)

//primitive data atoms, without a type
class HPBool(val booldata: Boolean): HPrimitive(booldata)
class HPReal(data: Double): HPrimitive(data)
class HPInt(val intdata: Int): HPrimitive(intdata)
class HPChar(data: Char): HPrimitive(data)
class HPString(data: String): HPrimitive(data)
class HPUnit: HPrimitive(Unit)

//typed versions of data primitives
class HTBool(data: Boolean): HTypedData(HType.HBool, HPBool(data))
class HTReal(data: Double): HTypedData(HType.HReal, HPReal(data))
class HTInt(data: Int): HTypedData(HType.HInt, HPInt(data))
class HTChar(data: Char): HTypedData(HType.HChar, HPChar(data))
class HTString(data: String): HTypedData(HType.HString, HPString(data)) {
	override fun toString(): String = "\"$data\""
}
class HTUnit: HTypedData(HType.HUnit, HPUnit())


abstract class HCallable(val name: String, val signature: SignatureType): HData(signature) {
	open fun call(inputs: Array<HTypedData>, state: HState): Pair<HTypedData, HState> {
		return when(this) {
			is HFunction -> runtime(inputs, state)
			is HNative -> runtime(inputs.map{ it.data }.toTypedArray().requireAllPrimitive(), state)
			else -> HTUnit() to state
		}
	}

	override fun toString(): String = "$name: $signature"
}

class HFunction(name: String, signature: SignatureType, val runtime: (Array<HTypedData>, HState) -> Pair<HTypedData, HState>): HCallable(name, signature)
class HNative(name: String, signature: SignatureType, val runtime: (Array<HPrimitive>, HState) -> Pair<HTypedData, HState>): HCallable(name, signature)

