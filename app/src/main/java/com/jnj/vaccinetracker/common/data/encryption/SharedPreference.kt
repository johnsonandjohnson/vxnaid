package com.jnj.vaccinetracker.common.data.encryption

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jnj.vaccinetracker.common.domain.entities.Manufacturer


class SharedPreference (context:Context)
{

    val PREFERENCE_NAME="Vxnaid"
    val preference=context.getSharedPreferences(PREFERENCE_NAME,Context.MODE_PRIVATE)
    var prefsEditor = preference.edit()
    val MANUFACTURER="manufacturer"

    fun saveManufracterList(manufacturer: List<Manufacturer>)
    {
        val gson = Gson()
        val json:String=gson.toJson(manufacturer)
        prefsEditor.putString(MANUFACTURER, json.toString())
        prefsEditor.apply()

    }

    fun getManufracterList():List<Manufacturer>
    {
        val gson = Gson()
        val jsonText: String = preference.getString(MANUFACTURER,"").toString()
        val myType=object :TypeToken<List<Manufacturer>>(){}.type
        val logs=gson.fromJson<List<Manufacturer>>(jsonText,myType)
        return logs
    }
}