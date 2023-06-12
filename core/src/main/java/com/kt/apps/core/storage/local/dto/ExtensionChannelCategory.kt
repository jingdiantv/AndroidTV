package com.kt.apps.core.storage.local.dto

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig

@Entity(
    primaryKeys = ["configSourceUrl", "name"]
)
data class ExtensionChannelCategory(
    val configSourceUrl: String,
    val name: String
)

data class ExtensionsCategoryWithListChannel(
    @Embedded
    val category: ExtensionChannelCategory,
    @Relation(
        parentColumn = "name",
        entityColumn = "tvGroup",
    )
    val listChannel: List<ExtensionsChannel>
)

data class ExtensionsConfigWithListCategory(
    @Embedded
    val config: ExtensionsConfig,
    @Relation(
        parentColumn = "sourceUrl",
        entityColumn = "configSourceUrl"
    )
    val listCategory: List<ExtensionChannelCategory>
)