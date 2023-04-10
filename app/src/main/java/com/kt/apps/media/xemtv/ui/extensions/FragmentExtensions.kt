package com.kt.apps.media.xemtv.ui.extensions

import android.content.Intent
import android.os.Parcelable
import android.view.View
import androidx.core.os.bundleOf
import androidx.leanback.widget.*
import androidx.lifecycle.ViewModelProvider
import com.kt.apps.core.base.BaseRowSupportFragment
import com.kt.apps.core.base.adapter.leanback.applyLoading
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ParserExtensionsSource
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.RoomDataBase
import com.kt.apps.core.tv.model.TVChannelGroup
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.R
import com.kt.apps.media.xemtv.presenter.DashboardTVChannelPresenter
import com.kt.apps.media.xemtv.ui.playback.PlaybackActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class FragmentExtensions : BaseRowSupportFragment() {

    @Inject
    lateinit var roomDataBase: RoomDataBase

    @Inject
    lateinit var parserExtensionsSource: ParserExtensionsSource

    private val disposable by lazy {
        CompositeDisposable()
    }

    private val mRowsAdapter: ArrayObjectAdapter by lazy {
        ArrayObjectAdapter(ListRowPresenter().apply {
            shadowEnabled = false
        })
    }

    private val extensionsID by lazy {
        requireArguments().getString(EXTRA_EXTENSIONS_ID)!!
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ExtensionsViewModel::class.java]
    }

    private var tvList: List<ExtensionsChannel>? = null

    override fun initView(rootView: View) {
        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
                startActivity(
                    Intent(
                        requireContext(),
                        PlaybackActivity::class.java
                    ).apply {
                        putExtra(
                            PlaybackActivity.EXTRA_PLAYBACK_TYPE,
                            PlaybackActivity.Type.EXTENSION as Parcelable
                        )
                        putExtra(
                            PlaybackActivity.EXTRA_ITEM_TO_PLAY,
                            item as ExtensionsChannel
                        )
                        putExtra(
                            PlaybackActivity.EXTRA_EXTENSIONS_ID,
                            extensionsID
                        )
                    }
                )
            }
    }

    override fun initAction(rootView: View) {
        adapter = mRowsAdapter
        Logger.e(this, message = extensionsID)
        mRowsAdapter.applyLoading(R.layout.item_tv_loading_presenter)

        disposable.add(
            roomDataBase.extensionsConfig()
                .getExtensionById(extensionsID)
                .subscribeOn(Schedulers.io())
                .flatMap {
                    parserExtensionsSource.parseFromRemoteRx(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ tvList ->
                    this@FragmentExtensions.tvList = tvList
                    val channelWithCategory = tvList.groupBy {
                        it.tvGroup
                    }
                    extensionsViewModel.appendExtensionsCache(extensionsID, tvList)
                    mRowsAdapter.clear()
                    val childPresenter = DashboardTVChannelPresenter()
                    for ((group, channelList) in channelWithCategory) {
                        val headerItem = try {
                            val gr = TVChannelGroup.valueOf(group)
                            HeaderItem(gr.value)
                        } catch (e: Exception) {
                            HeaderItem(group)
                        }
                        val adapter = ArrayObjectAdapter(childPresenter)
                        for (channel in channelList) {
                            adapter.add(channel)
                        }
                        mRowsAdapter.add(ListRow(headerItem, adapter))
                    }
                }, {
                    if (!this.isHidden && !this.isDetached) {
                        try {
                            showErrorDialog(content = it.message)
                        } catch (_: Exception) {
                        }
                    }
                    Logger.e(this, exception = it)
                })
        )
    }

    companion object {
        private const val EXTRA_EXTENSIONS_ID = "extra:extensions_id"
        fun newInstance(id: String) = FragmentExtensions().apply {
            this.arguments = bundleOf(
                EXTRA_EXTENSIONS_ID to id
            )
        }
    }
}