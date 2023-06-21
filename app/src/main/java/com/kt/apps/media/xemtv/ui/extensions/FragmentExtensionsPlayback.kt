package com.kt.apps.media.xemtv.ui.extensions

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.kt.apps.core.base.BasePlaybackFragment
import com.kt.apps.core.base.DataState
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.logging.logStreamingTV
import com.kt.apps.core.utils.expandUrl
import com.kt.apps.core.utils.isShortLink
import com.kt.apps.core.utils.showErrorDialog
import com.kt.apps.media.xemtv.presenter.TVChannelPresenterSelector
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

class FragmentExtensionsPlayback : BasePlaybackFragment() {
    override val numOfRowColumns: Int
        get() = 5

    private var itemToPlay: ExtensionsChannel? = null
    private val listCurrentItem by lazy {
        mutableListOf<ExtensionsChannel>()
    }
    private val retryTimes by lazy {
        mutableMapOf<String, Int>()
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val extensionsViewModel by lazy {
        ViewModelProvider(requireActivity(), factory)[ExtensionsViewModel::class.java]
    }

    private val extension: ExtensionsConfig by lazy {
        requireArguments().getParcelable(EXTRA_EXTENSION_ID)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemToPlay = requireArguments()[EXTRA_TV_CHANNEL] as ExtensionsChannel?
    }

    override fun onHandlePlayerError(error: PlaybackException) {
        super.onHandlePlayerError(error)
    }

    override fun onPlayerPlaybackStateChanged(playbackState: Int) {
        super.onPlayerPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_READY -> {
                itemToPlay?.let {
                    actionLogger.logStreamingTV(
                        it.tvChannelName,
                        "channelLink" to it.tvStreamLink
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemToPlay?.let {
            Logger.d(this, message = "$it")
            playVideo(it)
        }
        Logger.e(this@FragmentExtensionsPlayback, message = "id = $extension")
        extensionsViewModel.loadChannelForConfig(extension.sourceUrl).observe(viewLifecycleOwner){
            if (it is DataState.Success) {
                listCurrentItem.addAll(it.data)
                setupRowAdapter(listCurrentItem, TVChannelPresenterSelector(requireActivity()))
                onItemClickedListener = OnItemViewClickedListener { _, item, rowViewHolder, row ->
                    itemToPlay?.let {
                        retryTimes[it.channelId] = 0
                    }
                    itemToPlay = item as? ExtensionsChannel
                    itemToPlay?.let { it1 -> playVideo(it1) }
                }
            }
        }

        extensionsViewModel.programmeForChannelLiveData.observe(viewLifecycleOwner) {
            if (it is DataState.Success) {
                if (itemToPlay?.channelId == it.data.channel) {
                    itemToPlay?.currentProgramme = it.data
                    itemToPlay?.let { it1 -> showInfo(it1) }
                }
            }
        }


    }

    override fun onError(errorCode: Int, errorMessage: CharSequence?) {
        super.onError(errorCode, errorMessage)
        Logger.e(this, message = "errorCode: $errorCode, errorMessage: $errorMessage")
        val retriedTimes = retryTimes[itemToPlay!!.channelId] ?: 0
        val useMainSource = if (itemToPlay != null) {
            if (itemToPlay?.catchupSource.isNullOrBlank()) {
                true
            } else {
                retriedTimes < 2
            }
        } else {
            return
        }

        when {
            retriedTimes > MAX_RETRY_TIME -> {
                showErrorDialog(
                    content = "Kênh ${itemToPlay?.tvChannelName ?: "TV"} " +
                            "hiện tại đang lỗi hoặc chưa hỗ trợ nội dung miễn phí: " +
                            "$errorCode $errorMessage"
                )
                retryTimes[itemToPlay!!.channelId] = 0
            }

            useMainSource -> {
                itemToPlay?.let {
                    playVideo(it, false)
                }
                retryTimes[itemToPlay!!.channelId] = retriedTimes + 1
            }

            !itemToPlay?.catchupSource.isNullOrBlank() -> {
                itemToPlay?.let {
                    playVideo(it, true)
                }
                retryTimes[itemToPlay!!.channelId] = retriedTimes + 1
            }
        }
    }

    private var lastExpandUrlTask: Disposable? = null
    private val disposable by lazy {
        CompositeDisposable()
    }
    private fun playVideo(
        tvChannel: ExtensionsChannel,
        useCatchup: Boolean = false
    ) {
        extensionsViewModel.loadProgramForChannel(tvChannel, extension.type)
        lastExpandUrlTask?.let { disposable.remove(it) }
        disposable.clear()
        val linkToPlay = if (!useCatchup) {
            tvChannel.tvStreamLink
        } else {
            tvChannel.catchupSource
        }

        Logger.d(
            this,
            "PlayVideo",
            "$tvChannel,\t" +
                    "useCatchup: $useCatchup," +
                    "isHls: ${
                        linkToPlay.contains("m3u8") ||
                                linkToPlay.isShortLink()
                    }" +
                    ""
        )

        Logger.d(
            this,
            "LinkPlayVideo",
            linkToPlay
        )

        showInfo(tvChannel)

        if (linkToPlay.isShortLink()) {
            lastExpandUrlTask = Observable.fromCallable {
                linkToPlay.expandUrl()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ realUrl ->
                    if (isDetached) return@subscribe
                    playWhenReady(tvChannel, realUrl)
                }, {
                    if (isDetached) return@subscribe
                    playWhenReady(tvChannel, linkToPlay)
                })
            disposable.add(lastExpandUrlTask!!)

        } else if (linkToPlay.contains(".m3u8")) {
            playWhenReady(tvChannel, linkToPlay)
        } else {
            disposable.add(Observable.fromCallable {
                linkToPlay.expandUrl()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ realUrl ->
                    if (isDetached) return@subscribe
                    playWhenReady(tvChannel, realUrl)
                }, {
                    if (isDetached) return@subscribe
                    playWhenReady(tvChannel, linkToPlay)
                }))

        }
    }

    private fun FragmentExtensionsPlayback.playWhenReady(
        tvChannel: ExtensionsChannel,
        linkToPlay: String
    ) {
        playVideo(
            tvChannel.currentProgramme?.title?.takeIf {
                it.trim().isNotBlank()
            }?.trim() ?: tvChannel.tvChannelName,
            tvChannel.currentProgramme?.description?.takeIf {
                it.isNotBlank()
            }?.trim(),
            referer = tvChannel.referer,
            linkStream = listOf(linkToPlay),
            isLive = extension.type == ExtensionsConfig.Type.FOOTBALL,
            isHls = linkToPlay.contains("m3u8"),
            headers = tvChannel.props
        )
    }

    private fun showInfo(tvChannel: ExtensionsChannel) {
        Logger.d(this@FragmentExtensionsPlayback,"ChannelInfo", message = "$tvChannel")
        prepare(
            tvChannel.currentProgramme?.title?.takeIf {
                it.trim().isNotBlank()
            }?.trim() ?: tvChannel.tvChannelName,
            tvChannel.currentProgramme?.description?.takeIf {
                it.isNotBlank()
            }?.trim(),
            false
        )
    }

    override fun onDestroyView() {
        disposable.clear()
        super.onDestroyView()
    }

    companion object {
        private const val EXTRA_EXTENSION_ID = "extra:extension_id"
        private const val EXTRA_TV_CHANNEL = "extra:tv_channel"
        private const val EXTRA_TV_CHANNEL_LIST = "extra:tv_channel_list"
        fun newInstance(
            tvChannel: ExtensionsChannel,
            extension: ExtensionsConfig
        ) = FragmentExtensionsPlayback().apply {
            arguments = bundleOf(
                EXTRA_TV_CHANNEL to tvChannel,
                EXTRA_EXTENSION_ID to extension

            )
        }
    }
}