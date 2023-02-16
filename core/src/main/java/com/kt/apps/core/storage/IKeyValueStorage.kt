package com.kt.apps.core.storage

interface IKeyValueStorage {
    fun <T> get(key: String, clazz: Class<T>): T
    fun <T> save(key: String, value: T, clazz: Class<T>)
}