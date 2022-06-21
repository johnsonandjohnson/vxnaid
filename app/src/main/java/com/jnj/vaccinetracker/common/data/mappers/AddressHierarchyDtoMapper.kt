package com.jnj.vaccinetracker.common.data.mappers

import com.jnj.vaccinetracker.common.data.models.api.response.AddressHierarchyDto
import com.jnj.vaccinetracker.common.domain.entities.AddressHierarchy
import com.jnj.vaccinetracker.common.domain.entities.AddressValue
import com.jnj.vaccinetracker.common.domain.usecases.GetAddressMasterDataOrderUseCase
import javax.inject.Inject

class AddressHierarchyDtoMapper @Inject constructor(private val getAddressMasterDataOrderUseCase: GetAddressMasterDataOrderUseCase) {
    companion object {
        private const val SEP = "|"
    }

    private fun String.toNullableCell(): String? = takeIf { it.isNotEmpty() && it != "null" }

    suspend fun toDomain(addressHierarchyDto: AddressHierarchyDto): AddressHierarchy {

        return addressHierarchyDto.map { fields ->
            fields.replace("\uFEFF", "").split(SEP)
        }.groupBy { values -> values[0] } // first value is always country
            .mapValues { (_, rows) -> rows.map { cells -> cells.map { it.toNullableCell() } } } // make cells nullable
            .mapValues { (country, rows) ->
                val addressMasterDataOrder = getAddressMasterDataOrderUseCase.getAddressMasterDataOrder(country, isUseDefaultAsAlternative = false, onlyDropDowns = true)
                val longestRow = rows.maxByOrNull { it.filterNotNull().size } ?: error("rows.maxByOrNull must not be null for country $country")
                val longestRowNotNull = longestRow.filterNotNull()
                if (longestRowNotNull.size != addressMasterDataOrder.size) {
                    // the row with most non null values must have the same amount of non null values as config address hierarchy fields
                    error("longest address master data row $longestRowNotNull mismatch to config address hierarchy $addressMasterDataOrder")
                }
                // the longest row will be our model for the mapping
                val addressValueMapping = longestRow.mapIndexed { columnIndex, s -> columnIndex to s }
                    .filter { (_, s) -> s != null }
                    .map { (columnIndex, _) -> columnIndex }
                    .mapIndexed { nonNullIndex, columnIndex -> columnIndex to addressMasterDataOrder[nonNullIndex] }.toMap()

                rows.map { cells ->
                    cells.mapIndexedNotNull { columnIndex, cell ->
                        if (cell != null) {
                            val addressValueType = addressValueMapping[columnIndex]
                                ?: error("columnIndex $columnIndex mapping not known, non null mapping keys ${addressValueMapping.keys}, cells $cells")
                            AddressValue(cell, addressValueType)
                        } else {
                            null
                        }
                    }
                }
            }.let { AddressHierarchy(it) }
    }

}