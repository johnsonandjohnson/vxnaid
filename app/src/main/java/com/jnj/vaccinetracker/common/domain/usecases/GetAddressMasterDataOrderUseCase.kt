package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.domain.entities.AddressField
import com.jnj.vaccinetracker.common.domain.entities.AddressValueType
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import javax.inject.Inject

class GetAddressMasterDataOrderUseCase @Inject constructor(private val getConfigurationUseCase: GetConfigurationUseCase) {

    /**
     * if [isUseDefaultAsAlternative] is true then we will return the default master data order if [country] is null or config doesn't have address fields for [country]
     * if [onlyDropDowns] is true then only dropdown fields will be returned
     */
    suspend fun getAddressMasterDataOrder(country: String?, isUseDefaultAsAlternative: Boolean, onlyDropDowns: Boolean): List<AddressValueType> {
        if (country == null) {
            if (isUseDefaultAsAlternative)
                return AddressValueType.defaultOrder()
            else
                error("getAddressMasterDataOrder with null country while null country is not allowed")
        }
        val config = getConfigurationUseCase.getMasterData()
        var addressFields :List<AddressField>? =null

            config.addressFields.entries.forEach {
                if (it.key.equals(country, true)) {
                    addressFields = it.value
                }
            }

            addressFields ?: run {
            if (isUseDefaultAsAlternative) {
                return AddressValueType.defaultOrder()
            } else
                error("couldn't find address fields in config for country $country")

        }

        //country comes first always
        return (listOf(AddressValueType.COUNTRY) + addressFields!!
            .run {
                if (onlyDropDowns)
                    filter { it.inputType == AddressField.InputType.DROPDOWN }
                else
                    this
            }
            .map { it.field }).distinct()
    }
}