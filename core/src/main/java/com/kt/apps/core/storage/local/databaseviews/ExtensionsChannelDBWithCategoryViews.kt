package com.kt.apps.core.storage.local.databaseviews

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey
import com.kt.apps.core.extensions.ExtensionsChannel

@DatabaseView(
    "SELECT configSourceUrl, name as categoryName, tvChannelName, " +
            "logoChannel, tvStreamLink, sourceFrom " +
            "FROM ExtensionChannelCategory AS Category " +
            "INNER JOIN ExtensionChannelDatabaseViews ON Category.name = tvGroup"
)
class ExtensionsChannelDBWithCategoryViews(
    val configSourceUrl: String,
    val categoryName: String,
    val tvChannelName: String,
    val logoChannel: String,
    val tvStreamLink: String,
    val sourceFrom: String,
) {
}


@Fts4(
    contentEntity = ExtensionsChannel::class,
    order = FtsOptions.Order.ASC,
    matchInfo = FtsOptions.MatchInfo.FTS4,
    tokenizer = "unicode61",
)
@Entity
class ExtensionsChannelFts4(
    @PrimaryKey(
        autoGenerate = true
    )
    @ColumnInfo(name = "rowid")
    val index: Int,
    var tvGroup: String,
    val logoChannel: String,
    val tvChannelName: String,
    val tvStreamLink: String,
    val sourceFrom: String,
    val channelId: String,
    val channelPreviewProviderId: Long = -1,
    val isHls: Boolean,
    val catchupSource: String = "",
    val userAgent: String = "",
    val referer: String = "",
    val props: Map<String, String>? = null,
    val extensionSourceId: String
)