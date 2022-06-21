package com.jnj.vaccinetracker.common.validators

import com.jnj.vaccinetracker.common.helpers.logWarn
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import javax.inject.Inject

class PhoneValidator @Inject constructor(private val phoneNumberUtil: PhoneNumberUtil) {

    companion object {
        /**
         * only numbers and white spaces
         */
        private val phoneRegex = "[0-9\\s]*".toRegex()
    }

    fun validate(fullPhoneNumber: String): Boolean {
        if (!phoneRegex.matches(fullPhoneNumber)) {
            logWarn("$fullPhoneNumber not matching with $phoneRegex")
            return false
        }
        val phoneWithPlusPrefix = "+$fullPhoneNumber"
        val phoneNumber = try {
            phoneNumberUtil.parse(phoneWithPlusPrefix, null)
        } catch (e: Exception) {
            logWarn("Phone number invalid: ${e.message}")
            return false
        }
        return phoneNumberUtil.isValidNumber(phoneNumber)
    }
}