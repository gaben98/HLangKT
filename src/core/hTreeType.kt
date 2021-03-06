package core.typesystem

interface IHType {
	fun Is(other: IHType): Boolean
	fun Default(): HData
	fun GetParent(): IHType?
}

open class HType(var children: Array<HType>, private val parent: HType?, val name: String, val defVal: HData): IHType {
	open fun Define(child: String, defVal: HData): HType {
		if(child in typeMap) throw error("type has already been defined")
		val ch = HType(arrayOf(), this, child, defVal)
		typeMap[child] = ch
		return ch
	}

	override fun Is(other: IHType): Boolean {
		if(other is HType) {
			if(this === other) return true
			if (parent != null) {
				if (parent === other) return true
				return parent.Is(other)
			}
		}
		return false
	}

	override fun Default(): HData {
		return defVal
	}

	override fun GetParent(): IHType?= parent

	override fun toString(): String = name

	companion object {
		val HAny = HType(arrayOf(), null, "any", HPUnit())

		var typeMap: MutableMap<String, HType> = mutableMapOf("any" to HAny)

		//type definitions of primitives
		val HInt = HAny.Define("int", HPInt(0))
		val HReal = HAny.Define("real", HPReal(0.0))
		val HChar = HAny.Define("char", HPChar('0'))
		val HString = HAny.Define("string", HPString(""))
		val HBool = HAny.Define("bool", HPBool(false))
		val HFailure = HAny.Define("failure", HPUnit())
		val HUnit = TupleType(arrayOf())
	}
}

open class TupleType(val componentTypes: Array<out IHType>): IHType {
	override fun Is(other: IHType): Boolean {
		if(other === HType.HAny) return true
		if(other is TupleType && componentTypes.count() == other.componentTypes.count()) {
			return componentTypes.zip(other.componentTypes).all { (a, b) -> a.Is(b) }
		}
		return false
	}

	override fun Default(): HData = HPUnit()

	override fun GetParent(): IHType? = HType.HAny

	override fun toString(): String = "(" + componentTypes.joinToString(", ") + ")"
}

open class SignatureType(val inputs: TupleType, val returnType: IHType = HType.HUnit): IHType {
	override fun Is(other: IHType): Boolean {
		if(other === HType.HAny) return true
		if(other is SignatureType) {
			return inputs.Is(other.inputs) && returnType.Is(other.returnType)
		}
		if(other === HType.HAny) return true
		return false
	}

	override fun Default(): HData = HFunction("Unit Function", this) { _, state -> HTUnit() to state }

	override fun GetParent(): IHType? = HType.HAny

	override fun toString(): String = "$inputs -> $returnType"
}

open class ArrayType(val elementType: IHType): IHType {
	override fun Is(other: IHType): Boolean {
		if(other is ArrayType) return other.elementType === this.elementType
		if(other === HType.HAny) return true
		return false
	}

	override fun Default(): HData = HPUnit()

	override fun GetParent(): IHType? = HType.HAny

}

fun GetCommonType(vararg types: IHType): IHType {
	fun gct(type1: IHType, type2: IHType): IHType {
		if(type1.Is(type2)) return type1
		val parent = type1.GetParent()
		if(parent != null) return gct(parent, type2)
		return HType.HAny
	}
	return types.reduce(::gct)
}