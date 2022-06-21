package com.jnj.vaccinetracker.common.data.mappers

import com.jnj.vaccinetracker.common.domain.entities.AddressValue
import com.jnj.vaccinetracker.common.domain.entities.AddressValueType
import com.jnj.vaccinetracker.common.domain.usecases.GetAddressMasterDataOrderUseCase
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk

class AddressHierarchyDtoMapperTest : FunSpec({
    val getMasterDataOrder: GetAddressMasterDataOrderUseCase = mockk()
    val mapper = AddressHierarchyDtoMapper(getMasterDataOrder)
    val masterDataOrderMap = mutableMapOf<String, List<AddressValueType>>()
    coEvery { getMasterDataOrder.getAddressMasterDataOrder(any(), isUseDefaultAsAlternative = false, onlyDropDowns = true) } coAnswers {
        val country = it.invocation.args[0] as String
        masterDataOrderMap[country] ?: error("country $country not available")
    }
    context("given 3 expected address value types") {
        val country = "Belgium"
        masterDataOrderMap[country] = listOf(AddressValueType.COUNTRY, AddressValueType.CITY_VILLAGE, AddressValueType.POSTAL_CODE)
        test("and 3 address master data columns then return 3 address values") {
            // Arrange
            val addressHierarchyDto = listOf("$country|Antwerpen|2000", "$country|Wommelgem|2160")
            // Act
            val domain = mapper.toDomain(addressHierarchyDto)
            val values = domain.countryAddressMap[country]
            //Assert
            values.shouldNotBeNull()
            values[0][2] shouldBe AddressValue("2000", AddressValueType.POSTAL_CODE)
            values[1][1] shouldBe AddressValue("Wommelgem", AddressValueType.CITY_VILLAGE)
        }

        test("and 3 address master data columns with one missing cell then throw exception") {
            // Arrange
            val addressHierarchyDto = listOf("$country|Antwerpen", "$country|Wommelgem|2160")
            // Act
            try {
                mapper.toDomain(addressHierarchyDto)
                fail("exception expected")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        test("and 3 address master data columns with one null cell then don't throw exception") {
            // Arrange
            val addressHierarchyDto = listOf("$country|Antwerpen|null", "$country|Wommelgem|2160")
            // Act
            mapper.toDomain(addressHierarchyDto)
        }

        test("and 3 address master data columns with one empty cell then don't throw exception") {
            // Arrange
            val addressHierarchyDto = listOf("$country|Antwerpen|", "$country|Wommelgem|2160")
            // Act
            mapper.toDomain(addressHierarchyDto)
        }

        test("and 3 address master data columns with one null cell in middle then return correct mapping") {
            // Arrange
            val addressHierarchyDto = listOf("$country|Antwerpen|2000", "$country|null|2160")
            // Act
            val domain = mapper.toDomain(addressHierarchyDto)
            val values = domain.countryAddressMap[country]
            //Assert
            values.shouldNotBeNull()
            values[0][1] shouldBe AddressValue("Antwerpen", AddressValueType.CITY_VILLAGE)
            values[1][1] shouldBe AddressValue("2160", AddressValueType.POSTAL_CODE)
        }


        test("and 3 address master data columns with one missing column then throw exception") {
            // Arrange
            val addressHierarchyDto = listOf("$country|Antwerpen", "$country|Wommelgem")
            // Act
            try {
                mapper.toDomain(addressHierarchyDto)
                fail("exception expected")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
})