package com.jnj.vaccinetracker.register.data.mapper

import com.jnj.vaccinetracker.common.data.database.mappers.toDomain
import com.jnj.vaccinetracker.common.data.models.api.response.AddressDto
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.domain.entities.AddressValueType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressMapper @Inject constructor(moshi: Moshi) {

    private val addressMapAdapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
    private val addressAdapter = moshi.adapter(AddressDto::class.java)

    private fun emptyAddress() = Address(
        address1 = null,
        address2 = null,
        cityVillage = null,
        stateProvince = null,
        country = null,
        countyDistrict = null,
        postalCode = null
    )

    /**
     * [addressMap] is address field to value
     */
    fun toDomain(addressMap: Map<AddressValueType, String>): Address {
        val json = addressMapAdapter.toJson(addressMap.map { (addressValueType, value) -> addressValueType.fieldName to value }.toMap())
        val dto = addressAdapter.fromJson(json)
        return dto?.toDomain() ?: emptyAddress()
    }
}