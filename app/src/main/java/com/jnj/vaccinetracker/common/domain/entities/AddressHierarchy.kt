package com.jnj.vaccinetracker.common.domain.entities

import com.squareup.moshi.Json

/**
 * country to addressFields
 */
data class AddressHierarchy(val countryAddressMap: Map<String, List<List<AddressValue>>>)

data class AddressValue(val value: String, val type: AddressValueType)

enum class AddressValueType(val fieldName: String) {
    @field:Json(name = "country")
    COUNTRY("country"),

    @field:Json(name = "stateProvince")
    STATE_PROVINCE("stateProvince"),

    @field:Json(name = "countyDistrict")
    COUNTY_DISTRICT("countyDistrict"),

    @field:Json(name = "cityVillage")
    CITY_VILLAGE("cityVillage"),

    @field:Json(name = "postalCode")
    POSTAL_CODE("postalCode"),

    @field:Json(name = "address1")
    ADDRESS_1("address1"),

    @field:Json(name = "address2")
    ADDRESS_2("address2");


    companion object {
        fun defaultOrder() = values().toList()
    }

}