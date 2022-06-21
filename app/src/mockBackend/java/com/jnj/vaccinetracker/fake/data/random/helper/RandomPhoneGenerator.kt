package com.jnj.vaccinetracker.fake.data.random.helper

import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import javax.inject.Inject

class RandomPhoneGenerator @Inject constructor(private val phoneNumberUtil: PhoneNumberUtil) {

    companion object {
        private val regex = "[^0-9]".toRegex()
        fun String.digitsOnly() = replace(regex, "")
    }

    private fun generateValidPhone(): String = with(phoneNumberUtil) {
        val randomRegionCode = supportedRegions.random()
        val phone = getExampleNumber(randomRegionCode)
        format(phone, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL).digitsOnly()
    }

    fun generatePhone(percentNull: Int = 1): String? {
        val shouldBeNull = (1..100).random() <= percentNull
        return if (shouldBeNull) null else generateValidPhone()
    }
}