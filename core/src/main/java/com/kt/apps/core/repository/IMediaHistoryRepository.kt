package com.kt.apps.core.repository

import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import io.reactivex.rxjava3.core.Single

interface IMediaHistoryRepository {
    fun saveHistoryItem(item: HistoryMediaItemDTO)
    fun getHistoryForItem(itemId: String): Single<HistoryMediaItemDTO>
    fun getHistoryForItem(itemId: String, linkPlay: String): Single<HistoryMediaItemDTO>
    fun getListHistory(): Single<List<HistoryMediaItemDTO>>
}