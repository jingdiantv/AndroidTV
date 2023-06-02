package com.kt.apps.core.extensions

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
open class ExtensionsConfig @JvmOverloads constructor(
    var sourceName: String,
    @PrimaryKey
    val sourceUrl: String,
    var type: Type = Type.TV_CHANNEL
) : Parcelable {

    enum class Type {
        TV_CHANNEL, FOOTBALL
    }

    companion object {
        val test by lazy {
            ExtensionsConfig(
                "Test",
                "https://raw.githubusercontent.com/phuhdtv/vietngatv/master/vietngatv.m3u"
            )
        }
        val test2 by lazy {
            ExtensionsConfig(
                "Test K+",
                "https://s.id/nhamng"
            )
        }
    }
}

class ExtensionsConfigWithLoadedListChannel(
    sourceName: String,
    sourceUrl: String
) : ExtensionsConfig(sourceName, sourceUrl) {
    @Relation(parentColumn = "sourceUrl", entityColumn = "extensionSourceId")
    var extensionsChannelList: List<ExtensionsChannel>? = null
}
