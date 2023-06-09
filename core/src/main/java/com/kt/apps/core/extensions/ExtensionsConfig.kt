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

    override fun toString(): String {
        return "{" +
                "sourceName: $sourceName,\n" +
                "sourceUrl: $sourceUrl,\n" +
                "type: $type\n" +
                "}"
    }

    override fun equals(other: Any?): Boolean {
        if (other is ExtensionsConfig) {
            return sourceName == other.sourceName && sourceUrl == other.sourceName
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = sourceName.hashCode()
        result = 31 * result + sourceUrl.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

class ExtensionsConfigWithLoadedListChannel(
    sourceName: String,
    sourceUrl: String
) : ExtensionsConfig(sourceName, sourceUrl) {
    @Relation(parentColumn = "sourceUrl", entityColumn = "extensionSourceId")
    var extensionsChannelList: List<ExtensionsChannel>? = null
}
