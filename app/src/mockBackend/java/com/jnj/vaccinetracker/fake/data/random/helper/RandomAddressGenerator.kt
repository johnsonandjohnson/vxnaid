package com.jnj.vaccinetracker.fake.data.random.helper

import com.jnj.vaccinetracker.common.data.models.api.response.AddressDto
import com.jnj.vaccinetracker.fake.data.network.MockAssetReader
import com.jnj.vaccinetracker.register.data.mapper.AddressMapper
import javax.inject.Inject

class RandomAddressGenerator @Inject constructor(
    private val mockAssetReader: MockAssetReader,
    private val addressMapper: AddressMapper,
) {
    companion object {
        private const val COUNTRY = "India"
    }

    suspend fun generateAddress(): AddressDto {
        val country = COUNTRY
        val addressHierarchy = mockAssetReader.readAddressHierarchyDomain()
        val addressSet = addressHierarchy.countryAddressMap[country]!!
        val addressValues = addressSet.random()
        val map = addressValues
            .map { value ->
                val label = value.type
                label to value.value
            }.toMap()
        val address = addressMapper.toDomain(map)
        val number = (0..100).random().toString()
        val street = generateSequence { ('A'..'Z').random() }.take((10..20).random()).joinToString("")
        return address.copy(address1 = street, address2 = number)
    }
}