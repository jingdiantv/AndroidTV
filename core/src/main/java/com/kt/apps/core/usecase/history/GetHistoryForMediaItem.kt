package com.kt.apps.core.usecase.history

import com.kt.apps.core.base.rxjava.MaybeUseCase
import com.kt.apps.core.repository.IMediaHistoryRepository
import com.kt.apps.core.storage.local.dto.HistoryMediaItemDTO
import io.reactivex.rxjava3.core.Maybe
import javax.inject.Inject

class GetHistoryForMediaItem @Inject constructor(
    private val _repository: IMediaHistoryRepository
) : MaybeUseCase<HistoryMediaItemDTO>() {
    override fun prepareExecute(params: Map<String, Any>): Maybe<HistoryMediaItemDTO> {
        val itemId = params[EXTRA_ITEM_ID] as? String ?: return Maybe.empty()
        val linkPlay = params[EXTRA_LINK_PLAY] as? String ?: return Maybe.empty()
        return _repository.getHistoryForItem(itemId, linkPlay)
            .filter {
                it.currentPosition < it.contentDuration
            }
    }

    operator fun invoke(
        itemId: String,
        linkPlay: String
    ) = execute(mapOf(
        EXTRA_ITEM_ID to itemId,
        EXTRA_LINK_PLAY to linkPlay
    ))

    companion object {
        private const val EXTRA_ITEM_ID = "extra:item_id"
        private const val EXTRA_LINK_PLAY = "extra:link_play"
    }

}