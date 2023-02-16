package com.kt.apps.core.storage

import android.content.SharedPreferences
import com.google.gson.Gson
import javax.inject.Inject

open class KeyValueStorageImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : IKeyValueStorage {

    override fun <T> get(key: String, clazz: Class<T>): T {
        return when (clazz) {
            String::class.java -> {
                sharedPreferences.getString(key, "") as T
            }
            Boolean::class.java -> {
                sharedPreferences.getBoolean(key, false) as T
            }
            Int::class.java -> {
                sharedPreferences.getInt(key, -1) as T
            }
            Float::class.java -> sharedPreferences.getFloat(key, -1f) as T

            Long::class.java -> sharedPreferences.getLong(key, -1) as T
            else -> {
                val strValue = sharedPreferences.getString(key, "")
                Gson().fromJson(strValue, clazz)
            }
        }
    }

    override fun <T> save(key: String, value: T, clazz: Class<T>) {
        when (clazz) {
            String::class.java -> sharedPreferences.edit().putString(key, value as String).apply()

            Boolean::class.java -> sharedPreferences.edit().putBoolean(key, value as Boolean)
                .apply()

            Int::class.java -> sharedPreferences.edit().putInt(key, value as Int).apply()

            Float::class.java -> sharedPreferences.edit().putFloat(key, value as Float).apply()

            Long::class.java -> sharedPreferences.edit().putLong(key, value as Long).apply()

            else -> {
                val strValue = Gson().toJson(value)
                sharedPreferences.edit().putString(key, strValue).apply()
            }
        }
    }
}