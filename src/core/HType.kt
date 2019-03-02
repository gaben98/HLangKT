package core

interface IHType {
	fun Is(other: IHType): Boolean
}

class HType (private var children: Array<HType>, private val parent: HType?, private var checkval: Int, val name: String, defVal: Any?): IHType {
	override fun Is(other: IHType): Boolean = when(other) {
			is HType -> other.checkval % this.checkval == 0
			else -> false
		}
	fun Define(child: String): HType? {
		if (child in htypes) return null

		fun bubble(node: HType): HType? {

			return null
		}

		var nType = HType(arrayOf(), this, curPrime, child, null)
		when(children.count()) {
			0 -> {
				nType.checkval = this.checkval

			}
			1 -> {

			}
			else -> {

			}
		}
		return nType
	}
}

var htypes: Map<String, HType> = mapOf()
var curPrime = 2
var HObj = HType(arrayOf(), null, 2, "object", null)