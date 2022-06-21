package com.jnj.vaccinetracker.common.data.models

import com.jnj.vaccinetracker.common.helpers.logError
import okhttp3.Cookie
import okhttp3.internal.and
import java.io.*


class SerializableCookie : Serializable {

    @Transient
    private var cookie: Cookie? = null

    private companion object {
        private const val serialVersionUID = 3247863248961238946L
        private const val INVALID_EXPIRY = -1L
    }

    fun encode(cookie: Cookie): String? {
        this.cookie = cookie
        val byteArrayOutputStream = ByteArrayOutputStream()
        var objectOutputStream: ObjectOutputStream? = null
        try {
            objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject(this)
        } catch (e: IOException) {
            logError("IOException in encodeCookie", e)
            return null
        } finally {
            if (objectOutputStream != null) {
                try {
                    // Closing a ByteArrayOutputStream has no effect, it can be used later (and is used in the return statement)
                    objectOutputStream.close()
                } catch (e: IOException) {
                    logError("Stream not closed in encodeCookie", e)
                }
            }
        }
        return byteArrayToHexString(byteArrayOutputStream.toByteArray())
    }

    fun decode(encodedCookie: String): Cookie? {
        val bytes = hexStringToByteArray(encodedCookie)
        val byteArrayInputStream = ByteArrayInputStream(
            bytes
        )
        var cookie: Cookie? = null
        var objectInputStream: ObjectInputStream? = null
        try {
            objectInputStream = ObjectInputStream(byteArrayInputStream)
            cookie = (objectInputStream.readObject() as SerializableCookie).cookie
        } catch (e: IOException) {
            logError("IOException in decodeCookie", e)
        } catch (e: ClassNotFoundException) {
            logError("ClassNotFoundException in decodeCookie", e)
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close()
                } catch (e: IOException) {
                    logError("Stream not closed in decodeCookie", e)
                }
            }
        }
        return cookie
    }

    /**
     * Converts byte array to hex value string
     *
     * @param bytes byte array to be converted
     * @return string containing hex values
     */
    private fun byteArrayToHexString(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (element in bytes) {
            val v: Int = element and 0xff
            if (v < 16) {
                sb.append('0')
            }
            sb.append(Integer.toHexString(v))
        }
        return sb.toString()
    }

    /**
     * Converts hex values from strings to byte array
     *
     * @param hexString string of hex-encoded values
     * @return decoded byte array
     */
    private fun hexStringToByteArray(hexString: String): ByteArray? {
        val len = hexString.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) + Character
                .digit(hexString[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeObject(cookie?.name)
        out.writeObject(cookie?.value)
        if(cookie?.expiresAt != null){
            out.writeLong(cookie!!.expiresAt)
        } else {
            out.writeLong(INVALID_EXPIRY)
        }
        out.writeObject(cookie?.domain)
        out.writeObject(cookie?.path)
        cookie?.let { out.writeBoolean(it.secure) }
        cookie?.let { out.writeBoolean(it.httpOnly) }
        cookie?.let { out.writeBoolean(it.hostOnly) }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(inputStream: ObjectInputStream) {
        val builder = Cookie.Builder()

        builder.name(inputStream.readObject() as String)
        builder.value(inputStream.readObject() as String)

        val expiresAt = inputStream.readLong()
        if (expiresAt != INVALID_EXPIRY) builder.expiresAt(expiresAt)

        val domain = inputStream.readObject() as String
        builder.domain(domain)

        builder.path(inputStream.readObject() as String)

        if (inputStream.readBoolean()) builder.secure()
        if (inputStream.readBoolean()) builder.httpOnly()
        if (inputStream.readBoolean()) builder.hostOnlyDomain(domain)

        cookie = builder.build()
    }
}