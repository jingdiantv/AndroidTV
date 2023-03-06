package com.kt.apps.core.base.player

data class LinkStream(
    val m3u8Link: String,
    val referer: String,
    val streamId: String
) {
    var token: String? = null
    var host: String? = null

    override fun equals(other: Any?): Boolean {
        if (other is LinkStream && other.streamId.equals(streamId, ignoreCase = true)) {
            return true
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = m3u8Link.hashCode()
        result = 31 * result + referer.hashCode()
        result = 31 * result + streamId.hashCode()
        result = 31 * result + (token?.hashCode() ?: 0)
        result = 31 * result + (host?.hashCode() ?: 0)
        return result
    }
}