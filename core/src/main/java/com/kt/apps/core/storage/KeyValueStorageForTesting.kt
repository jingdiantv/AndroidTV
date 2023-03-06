package com.kt.apps.core.storage

import androidx.annotation.VisibleForTesting

@VisibleForTesting
class KeyValueStorageForTesting : IKeyValueStorage {
    override fun <T> get(key: String, clazz: Class<T>): T {
        TODO("Not yet implemented")
    }

    override fun <T, U> get(key: String, clazz: Class<T>, clazz2: Class<U>): Map<T, U> {
        TODO("Not yet implemented")
    }

    override fun <T> save(key: String, value: T, clazz: Class<T>) {
        TODO("Not yet implemented")
    }

    override fun <T, U> save(key: String, value: Map<T, U>) {
        TODO("Not yet implemented")
    }

    override fun <T> save(key: String, value: List<T>) {
        TODO("Not yet implemented")
    }

    override fun <T> getList(key: String, clazz: Class<T>): List<T> {
        TODO("Not yet implemented")
    }
}