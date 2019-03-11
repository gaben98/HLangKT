package core.typesystem

import core.environment.requireAllPrimitive

abstract class HData(var htype: IHType)
abstract class HPrimitive(htype: IHType, private var data: Any): HData(htype) {
	open fun<T> GetData(): T = data as T
}

class HStruct(htype: IHType, var data: Array<HData>): HData(htype) {
	override fun toString(): String = data.toString()
}

class HPBool(var data: Boolean): HPrimitive(HType.HBool, data) {
	override fun toString(): String = data.toString()
}
class HPReal(var data: Double): HPrimitive(HType.HReal, data) {
	override fun toString(): String = data.toString()
}
class HPInt(var data: Int): HPrimitive(HType.HInt, data) {
	override fun toString(): String = data.toString()
}
class HPChar(var data: Char): HPrimitive(HType.HChar, data) {
	override fun toString(): String = data.toString()
}
class HPString(var data: String): HPrimitive(HType.HString, data) {
	override fun toString(): String = data.toString()
}
class HPUnit: HPrimitive(HType.HUnit, Unit) {
	override fun toString(): String = "Unit"
}


abstract class HCallable(val signature: SignatureType): HData(signature) {
	open fun call(inputs: Array<HData>): HData {
		return when(this) {
			is HFunction -> runtime(inputs)
			is HNative -> runtime(inputs.requireAllPrimitive())
			else -> HPUnit()
		}
	}
}

class HFunction(signature: SignatureType, val runtime: (Array<HData>) -> HData): HCallable(signature)
class HNative(signature: SignatureType, val runtime: (Array<HPrimitive>) -> HData): HCallable(signature)