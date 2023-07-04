package com.kt.apps.core.storage.local.databaseviews

import androidx.room.DatabaseView
import androidx.room.PrimaryKey

@DatabaseView(
    "SELECT * FROM ExtensionsChannel ORDER BY CASE WHEN LENGTH(tvChannelName) < 2 THEN tvChannelName WHEN tvChannelName LIKE 'A%' OR tvChannelName LIKE 'Á%' OR tvChannelName LIKE 'À%' OR tvChannelName LIKE 'Ả%' OR tvChannelName LIKE 'Ã%' OR tvChannelName LIKE 'Ạ%' THEN 'A'||substr(tvChannelName, 1, 3) WHEN tvChannelName LIKE 'Â%' OR tvChannelName LIKE 'Ấ%' OR tvChannelName LIKE 'Ầ%' OR tvChannelName LIKE 'Ẩ%' OR tvChannelName LIKE 'Ẫ%' OR tvChannelName LIKE 'Ậ%' THEN 'Azz' WHEN tvChannelName LIKE 'Ă%' OR tvChannelName LIKE 'Ắ%' OR tvChannelName LIKE 'Ằ%' OR tvChannelName LIKE 'Ẳ%' OR tvChannelName LIKE 'Ẵ%' OR tvChannelName LIKE 'Ặ%' THEN 'Az'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Đ%' THEN 'Dz'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Ê%' OR tvChannelName LIKE 'Ế%' OR tvChannelName LIKE 'Ề%' OR tvChannelName LIKE 'Ể%' OR tvChannelName LIKE 'Ễ%' OR tvChannelName LIKE 'Ệ%' THEN 'Ez'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Ô%' OR tvChannelName LIKE 'Ố%' OR tvChannelName LIKE 'Ồ%' OR tvChannelName LIKE 'Ổ%' OR tvChannelName LIKE 'Ỗ%' OR tvChannelName LIKE 'Ộ%' THEN 'Oz'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Ơ%' OR tvChannelName LIKE 'Ớ%' OR tvChannelName LIKE 'Ờ%' OR tvChannelName LIKE 'Ở%' OR tvChannelName LIKE 'Ỡ%' OR tvChannelName LIKE 'Ợ%' THEN 'Ozz' WHEN tvChannelName LIKE 'Ư%' OR tvChannelName LIKE 'Ứ%' OR tvChannelName LIKE 'Ừ%' OR tvChannelName LIKE 'Ử%' OR tvChannelName LIKE 'Ữ%' OR tvChannelName LIKE 'Ự%' THEN 'Uz'||substr(tvChannelName, 1 , 2) ELSE substr(tvChannelName, 0, 3) END"
)
class ExtensionChannelDatabaseViews(
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
    val extensionSourceId: String,
    @PrimaryKey(autoGenerate = true)
    val index: Int
) {
}