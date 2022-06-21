package com.jnj.vaccinetracker.common.helpers

import javax.inject.Inject

class FullPhoneFormatter @Inject constructor() {

    fun toFullPhoneNumberOrNull(phone: String, countryCode: String): String? {
        if (phone.isEmpty() || countryCode.isEmpty())
            return null
        return "$countryCode$phone".replace(" ", "")
    }
}