package core.typesystem

import java.lang.Exception

interface IHType {
	fun Is(other: IHType): Boolean
	fun Default(): HData
}

open class HType(private var children: Array<HType>, private val parent: HType?, name: String, val defVal: HData): IHType {
	open fun Define(child: String, defaultVal: HData): HType {
		if(child in typeMap) throw Exception("type has already been defined")
		val ch = HType(arrayOf(), this, child, defaultVal)
		typeMap[child] = ch
		return ch
	}

	override fun Is(other: IHType): Boolean {
		if(other is HType) {
			if (parent != null) {
				if (parent === other) return true
				return parent.Is(other)
			}
			return this === other
		}
		return false
	}

	override fun Default(): HData {
		return defVal
	}

	companion object {
		val HAny = HType(arrayOf(), null, "any", HPUnit())

		var typeMap: MutableMap<String, HType> = mutableMapOf("any" to HAny)

		val HInt = HAny.Define("int", HPInt(0))
		val HReal = HAny.Define("real", HPReal(0.0))
		val HChar = HAny.Define("char", HPChar('0'))
		val HString = HAny.Define("string", HPString(""))
		val HBool = HAny.Define("bool", HPBool(false))
		val HUnit = TupleType(arrayOf())
	}
}

open class TupleType(val componentTypes: Array<out IHType>): IHType {
	override fun Is(other: IHType): Boolean {
		if(other is TupleType && componentTypes.count() == other.componentTypes.count()) {
			return componentTypes.zip(other.componentTypes).all { (a, b) -> a.Is(b) }
		}
		return false
	}

	override fun Default(): HData = HPUnit()
}

open class SignatureType(val inputs: TupleType, val returnType: IHType = TupleType(arrayOf())): IHType {
	override fun Is(other: IHType): Boolean {
		if(other is SignatureType) {
			return inputs.Is(other.inputs) && returnType.Is(other.returnType)
		}
		if(other === HType.HAny) return true
		return false
	}

	override fun Default(): HData = HFunction(this) { HPUnit() }
}