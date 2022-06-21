package com.jnj.vaccinetracker.common.data.models

/**
 * @author maartenvangiel
 * @version 1
 */
data class AddressTree(
    val data: String,
    @Transient val parent: AddressTree? = null
) {

    var children = listOf<AddressTree>()
        private set

    val isRoot: Boolean get() = parent == null

    val level: Int get() = if (isRoot) 0 else parent!!.level + 1

    fun addChild(childData: String): AddressTree {
        val node = AddressTree(childData, this)
        children = children + node
        return node
    }

    fun getOrCreateChild(childData: String): AddressTree {
        return children.find { it.data == childData } ?: addChild(childData)
    }

    fun getNode(childData: String): AddressTree? {
        return children.find { it.data == childData }
    }

    override fun toString(): String {
        return "AddressTree(level=$level, data=$data, children=[${children.joinToString(separator = ",\n\t")}])"
    }

}