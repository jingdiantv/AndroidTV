package com.kt.apps.core.storage

import androidx.annotation.VisibleForTesting

@VisibleForTesting
class KeyValueStorageForTesting : IKeyValueStorage {
    override fun <T> get(key: String, clazz: Class<T>): T {
        return clazz.newInstance()
    }

    override fun <T, U> get(key: String, clazz: Class<T>, clazz2: Class<U>): Map<T, U> {
       return mapOf()
    }

    override fun <T: Any> save(key: String, value: T) {
        println(key)
    }

    override fun <T, U> save(key: String, value: Map<T, U>) {
        println(key)
    }

    override fun <T> save(key: String, value: List<T>) {
        println(key)
    }

    override fun <T> getList(key: String, clazz: Class<T>): List<T> {
        println(key)
        return listOf()
    }
}