package com.jnj.vaccinetracker.common.data.helpers

import com.squareup.moshi.Moshi
import javax.inject.Inject

class Json @Inject constructor(val moshi: Moshi) {

    fun <T : Any> stringify(obj: T, type: Class<out T> = obj::class.java): String {
        return moshi.adapter<T>(type).toJson(obj)!!
    }

    inline fun <reified T> parse(json: String): T {
        return moshi.adapter(T::class.java).fromJson(json)!!
    }
}