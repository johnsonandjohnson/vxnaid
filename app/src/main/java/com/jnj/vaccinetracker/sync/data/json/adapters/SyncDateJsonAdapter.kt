package com.jnj.vaccinetracker.sync.data.json.adapters

import com.jnj.vaccinetracker.sync.data.models.SyncDate
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

class SyncDateJsonAdapter : JsonAdapter<SyncDate>() {

    override fun fromJson(reader: JsonReader): SyncDate? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        val time = reader.nextLong()
        return SyncDate(time)
    }

    override fun toJson(writer: JsonWriter, value: SyncDate?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.time)
        }
    }
}