package com.kt.apps.autoupdate.model

import com.google.android.datatransport.cct.StringMerger

class UpdateInfo(
    val version: Int,
    val priority: Int,
    val updateMethod: List<UpdateMethod>
) {
    class UpdateMethod(
        val type: String,
        val link: String
    )
}