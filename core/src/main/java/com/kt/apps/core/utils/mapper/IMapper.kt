package com.kt.apps.core.utils.mapper

interface IMapper<FROM, TO> {
    fun mapTo(from: FROM): TO
}