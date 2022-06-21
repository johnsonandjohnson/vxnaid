package com.jnj.vaccinetracker.common.data.database.entities.base

interface AddressEntityBase : AddressBase {
    val address1: String?
    val address2: String?
    val cityVillage: String?
    val stateProvince: String?
    val country: String?
    val countyDistrict: String?
    val postalCode: String?
}