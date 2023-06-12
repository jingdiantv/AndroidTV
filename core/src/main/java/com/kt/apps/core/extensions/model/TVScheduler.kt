package com.kt.apps.core.extensions.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class TVScheduler @JvmOverloads constructor(
    var date: String = "",
    var sourceInfoName: String = "",
    var generatorInfoName: String = "",
    var generatorInfoUrl: String = "",
    var extensionsConfigId: String = "",
    @PrimaryKey
    var epgUrl: String = ""
) {

    class Channel @JvmOverloads constructor(
        var id: String = "",
        var displayName: String = "",
        var displayNumber: String = "",
        var icon: String = "",
    ) {
        override fun toString(): String {
            return "{" +
                    "channelId: $id,\n" +
                    "displayNumber: $displayNumber,\n" +
                    "displayName: $displayName,\n" +
                    "icon: $icon,\n" +
                    "}"
        }
    }

    @Entity(primaryKeys = ["channel", "title", "start"])
    class Programme @JvmOverloads constructor(
        var channel: String = "",
        var channelNumber: String = "",
        var start: String = "",
        var stop: String = "",
        var title: String = "",
        var description: String = "",
        var extensionsConfigId: String = "",
        var extensionEpgUrl: String = ""
    ) {
        override fun toString(): String {
            return "{" +
                    "channel: $channel,\n" +
                    "channelNumber: $channelNumber,\n" +
                    "start: $start,\n" +
                    "stop: $stop,\n" +
                    "title: $title,\n" +
                    "description: $description,\n" +
                    "}"
        }
    }

    override fun toString(): String {
        return "{" +
                "date: $date,\n" +
                "sourceInfoName: $sourceInfoName,\n" +
                "generatorInfoName: $generatorInfoName,\n" +
                "sourceInfoUrl: $generatorInfoUrl,\n" +
                "listTV: $extensionsConfigId,\n" +
                "}"
    }
}