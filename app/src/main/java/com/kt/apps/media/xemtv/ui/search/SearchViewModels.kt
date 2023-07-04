package com.kt.apps.media.xemtv.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kt.apps.core.base.BaseViewModel
import com.kt.apps.core.base.DataState
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.model.TVChannel.Companion.mapToTVChannel
import com.kt.apps.core.tv.model.TVChannelLinkStream
import com.kt.apps.core.usecase.search.SearchForText
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class SearchViewModels @Inject constructor(
    private val roomDataBase: RoomDataBase,
    private val searchForText: SearchForText
) : BaseViewModel() {

    private val _searchQueryLiveData by lazy {
        MutableLiveData<DataState<Map<String, List<SearchForText.SearchResult>>>>()
    }

    val searchQueryLiveData: LiveData<DataState<Map<String, List<SearchForText.SearchResult>>>>
        get() = _searchQueryLiveData

    var searchTask: Disposable? = null
    fun querySearch(query: String?, filter: String? = null, page: Int = 0) {
        query ?: return
        compositeDisposable.clear()
        _searchQueryLiveData.postValue(DataState.Loading())
        searchTask?.let {
            it.dispose()
            compositeDisposable.remove(it)
        }
        searchTask = searchForText(query, filter, limit = 1500, offset = page * 1500)
            .subscribe({
                _searchQueryLiveData.postValue(DataState.Success(it))
            }, {
                _searchQueryLiveData.postValue(DataState.Error(it))
            })
        add(searchTask!!)
    }

    private val _selectedItemLiveData by lazy {
        MutableLiveData<DataState<Any>>()
    }

    val selectedItemLiveData: LiveData<DataState<Any>>
        get() = _selectedItemLiveData

    fun getResultForItem(
        searchItem: SearchForText.SearchResult
    ) {
        when (searchItem) {
            is SearchForText.SearchResult.TV -> {
                _selectedItemLiveData.postValue(DataState.Loading())
                add(
                    roomDataBase.tvChannelDao()
                        .getChannelWithUrl(searchItem.data.channelId)
                        .map {
                            TVChannelLinkStream(
                                it.mapToTVChannel(),
                                it.urls.map { it.url }
                            )
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribe({
                            _selectedItemLiveData.postValue(DataState.Success(it))
                        }, {
                            Logger.e(this@SearchViewModels, "${it.message}", it)
                            _selectedItemLiveData.postValue(DataState.Error(it))
                        })
                )
            }

            is SearchForText.SearchResult.ExtensionsChannelWithCategory -> {
                _selectedItemLiveData.postValue(DataState.Loading())
                add(
                    roomDataBase.extensionsChannelDao()
                        .getConfigAndChannelByStreamLink(searchItem.data.tvStreamLink)
                        .subscribeOn(Schedulers.io())
                        .subscribe({
                            _selectedItemLiveData.postValue(DataState.Success(it))
                        }, {
                            Logger.e(this@SearchViewModels, "${it.message}", it)
                            _selectedItemLiveData.postValue(DataState.Error(it))
                        })
                )
            }
        }
    }
}