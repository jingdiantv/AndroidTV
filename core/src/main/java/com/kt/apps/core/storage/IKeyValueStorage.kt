package com.kt.apps.core.storage

interface IKeyValueStorage {
    fun <T> get(key: String, clazz: Class<T>): T
    fun <T : Any> save(key: String, value: T)
    fun <T, U> save(key: String, value: Map<T, U>)
    fun <T, U> get(key: String, clazz: Class<T>, clazz2: Class<U>): Map<T, U>

    fun <T> save(key: String, value: List<T>)
    fun <T> getList(key: String, clazz: Class<T>): List<T>
}