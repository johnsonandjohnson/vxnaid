package com.jnj.vaccinetracker.common.data.encryption

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePasswordGenerator @Inject constructor() {

    private val secureRandom by lazy {
        SecureRandom()
    }

    fun generateSecurePassword(maxLength: Int, upperCase: Boolean, lowerCase: Boolean, numbers: Boolean, specialCharacters: Boolean): String {
        val upperCaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowerCaseChars = "abcdefghijklmnopqrstuvwxyz"
        val numberChars = "0123456789"
        val specialChars = "!@#$%^&*()_-+=<>?/{}~|"
        var allowedChars = ""
        val rn = secureRandom
        val sb = StringBuilder(maxLength)

        //this will fulfill the requirements of atleast one character of a type.
        if (upperCase) {
            allowedChars += upperCaseChars
            sb.append(upperCaseChars[rn.nextInt(upperCaseChars.length - 1)])
        }
        if (lowerCase) {
            allowedChars += lowerCaseChars
            sb.append(lowerCaseChars[rn.nextInt(lowerCaseChars.length - 1)])
        }
        if (numbers) {
            allowedChars += numberChars
            sb.append(numberChars[rn.nextInt(numberChars.length - 1)])
        }
        if (specialCharacters) {
            allowedChars += specialChars
            sb.append(specialChars[rn.nextInt(specialChars.length - 1)])
        }

        //fill the allowed length from different chars now.
        for (i in sb.length until maxLength) {
            sb.append(allowedChars[rn.nextInt(allowedChars.length)])
        }
        return sb.toString()
    }
}