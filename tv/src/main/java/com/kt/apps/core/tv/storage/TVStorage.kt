package com.kt.apps.core.tv.storage

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kt.apps.core.storage.KeyValueStorageImpl
import com.kt.apps.core.tv.model.TVChannel
import com.kt.apps.core.tv.model.TVDataSourceFrom
import javax.inject.Inject
import javax.inject.Provider

class TVStorage @Inject constructor(
    private val sharedPreferences: Provider<SharedPreferences>
) : KeyValueStorageImpl(sharedPreferences.get()) {

    fun getTvByGroup(group: String) = get(group, String::class.java).let {
        Gson().fromJson(
            it,
            TypeToken.getParameterized(
                List::class.java,
                TVChannel::class.java
            ).type
        )
    } ?: listOf<TVChannel>()

    fun saveTVByGroup(group: String, value: List<TVChannel>) {
        sharedPreferences.get().edit().putString(group, Gson().toJson(value)).apply()
    }

    fun getVersionRefreshed(name: String): Long {
        return sharedPreferences.get().getLong("${name}_refresh_version", 1)
    }

    fun saveRefreshInVersion(name: String, long: Long) {
        sharedPreferences.get().edit()
            .putLong("${name}_refresh_version", long)
            .apply()
    }

    fun cacheCookie(sourceFrom: TVDataSourceFrom): Map<String, String> {
        val type = TypeToken.getParameterized(
            Map::class.java,
            String::class.java,
            String::class.java
        ).type
        val gson = Gson()
        val oldCookie: String = sharedPreferences.get().getString(
            "${sourceFrom.name}_cookies", null
        ) ?: return mapOf()
        return gson.fromJson(oldCookie, type)
    }

}