package core.typesystem

import java.lang.Exception

interface IHType {
	fun Is(other: IHType): Boolean
}

open class HType(private var children: Array<HType>, private val parent: HType?, name: String, val defVal: Any?): IHType {
	open fun Define(child: String, defaultVal: Any? = null): HType {
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

	companion object {
		val HAny = HType(arrayOf(), null, "any", null)

		var typeMap: MutableMap<String, HType> = mutableMapOf("any" to HAny)

		val HInt = HAny.Define("int", 0)
		val HReal = HAny.Define("real", 0.0)
		val HChar = HAny.Define("char", '0' - '0')
		val HString = HAny.Define("string", "")
		val HBool = HAny.Define("bool", false)
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
}

open class SignatureType(val inputs: TupleType, val returnType: IHType = TupleType(arrayOf())): IHType {
	override fun Is(other: IHType): Boolean {
		if(other is SignatureType) {
			return inputs.Is(other.inputs) && returnType.Is(other.returnType)
		}
		if(other === HType.HAny) return true
		return false
	}


}