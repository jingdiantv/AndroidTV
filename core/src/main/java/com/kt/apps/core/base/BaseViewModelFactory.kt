package com.kt.apps.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

class BaseViewModelFactory @Inject constructor(
    private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        var creator = creators[modelClass]
        if (creator == null) {
            for ((key, value) in creators) {
                key.isAssignableFrom(modelClass)
                creator = value
                break
            }
        }
        return try {
            creator?.let {
                it.get() as T
            } ?: throw IllegalStateException("No view model factory found: $modelClass")
        } catch (e: Exception) {
            throw e
        }
    }
}