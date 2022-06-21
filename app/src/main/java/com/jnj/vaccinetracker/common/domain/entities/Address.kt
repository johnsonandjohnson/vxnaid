package com.jnj.vaccinetracker.common.domain.entities

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Address(
    val address1: String?,
    val address2: String?,
    val cityVillage: String?,
    val stateProvince: String?,
    val country: String?,
    val countyDistrict: String?,
    val postalCode: String?,
) {
    private fun AddressValueType.toValue(): String? = when (this) {
        AddressValueType.COUNTRY -> country
        AddressValueType.STATE_PROVINCE -> stateProvince
        AddressValueType.COUNTY_DISTRICT -> countyDistrict
        AddressValueType.CITY_VILLAGE -> cityVillage
        AddressValueType.POSTAL_CODE -> postalCode
        AddressValueType.ADDRESS_1 -> address1
        AddressValueType.ADDRESS_2 -> address2
    }

    fun isEmpty() = listOfNotNull(address1, address2, cityVillage, stateProvince, country, countyDistrict, postalCode).isEmpty()

    fun toStringList(fields: List<AddressValueType>): List<String> {
        return fields.mapNotNull { it.toValue() }
    }
}
