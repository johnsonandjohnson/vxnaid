package com.jnj.vaccinetracker.common.data.database.models.common

import com.jnj.vaccinetracker.common.data.database.entities.base.AddressBase
import com.jnj.vaccinetracker.common.data.database.entities.base.AddressEntityBase

data class RoomAddressModel(
    override val address1: String?,
    override val address2: String?,
    override val cityVillage: String?,
    override val stateProvince: String?,
    override val country: String?,
    override val countyDistrict: String?,
    override val postalCode: String?,
) : AddressBase, AddressEntityBase
