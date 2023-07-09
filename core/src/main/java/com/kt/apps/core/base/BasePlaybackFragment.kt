package com.kt.apps.core.base

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.kt.apps.core.base.leanback.media.PlaybackTransportControlGlue
import com.kt.apps.core.base.leanback.media.SurfaceHolderGlueHost
import com.kt.apps.core.base.leanback.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.util.Util
import com.kt.apps.core.R
import com.kt.apps.core.base.leanback.ProgressBarManager
import com.kt.apps.core.base.leanback.media.LeanbackPlayerAdapter
import com.kt.apps.core.base.player.AbstractExoPlayerManager
import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.logging.IActionLogger
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.utils.*
import com.kt.skeleton.makeGone
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import java.util.Formatter
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.max


private const val MIN_SEEK_DURATION = 30 * 1000

abstract class BasePlaybackFragment : PlaybackSupportFragment(),
    HasAndroidInjector, IMediaKeycodeHandler {
    private val progressManager by lazy {
        ProgressBarManager()
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var actionLogger: IActionLogger

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<LeanbackPlayerAdapter>
    protected var mAdapter: ObjectAdapter? = null
    protected var mGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    private var mGridPresenter: VerticalGridPresenter? = null
    private var mVideoSurface: SurfaceView? = null
    private var mMediaPlaybackCallback: SurfaceHolder.Callback? = null
    private var mState = SURFACE_NOT_CREATED
    private var mSelectedPosition = -1
    private var mPlayingPosition = -1
    private var mGridViewPickHeight = 0f
    private var mGridViewOverlays: FrameLayout? = null
    private var mPlayPauseIcon: ImageButton? = null
    private var mPlaybackOverlaysContainerView: View? = null
    private var mBackgroundView: View? = null
    private var mPlaybackInfoContainerView: LinearLayout? = null
    private var mPlaybackTitleView: TextView? = null
    private var mPlaybackInfoView: TextView? = null
    private var mPlaybackInfoLiveView: TextView? = null
    private var mBrowseDummyView: FrameLayout? = null
    private var progressBar: SeekBar? = null
    private var progressBarContainer: ViewGroup? = null
    private var contentPositionView: TextView? = null
    private var contentDurationView: TextView? = null
    protected var onItemClickedListener: OnItemViewClickedListener? = null
    private val mChildLaidOutListener = OnChildLaidOutListener { _, _, _, _ ->
    }
    private val mGlueHost by lazy {
        BasePlaybackSupportFragmentGlueHost(this@BasePlaybackFragment)
    }
    abstract val numOfRowColumns: Int
    private var durationSet = false
    private val mPlayerListener by lazy {
        object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                super.onTimelineChanged(timeline, reason)
            }

            override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
                super.onSeekBackIncrementChanged(seekBackIncrementMs)
            }

            override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
                super.onSeekForwardIncrementChanged(seekForwardIncrementMs)
            }

            override fun onMetadata(metadata: Metadata) {
                super.onMetadata(metadata)
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                onHandlePlayerError(error)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    mPlayPauseIcon?.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.round_pause_24
                        )
                    )
                } else {
                    mPlayPauseIcon?.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.round_play_arrow_24
                        )
                    )
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                onPlayerPlaybackStateChanged(playbackState)
                if (playbackState == ExoPlayer.STATE_READY) {
                    progressManager.hide()
                    progressBar?.isActivated = true
                    changeNextFocus()
                    val player = exoPlayerManager.exoPlayer ?: return
                    durationSet = true
                    updateProgress(player)
                    exoPlayerManager.exoPlayer?.setSeekParameters(SeekParameters(10_000L, 10_000L))
                }
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    if (progressManager.isShowing) {
                        progressManager.hide()
                    }
                }
            }
        }
    }

    val mainLooper by lazy {
        Handler(Looper.getMainLooper())
    }

    private val timerTask by lazy {
        object : TimerTask() {
            override fun run() {
                mainLooper.post {
                    if (exoPlayerManager.exoPlayer?.isPlaying == true
                        && exoPlayerManager.exoPlayer?.playbackState != Player.STATE_ENDED
                    ) {
                        val player = exoPlayerManager.exoPlayer ?: return@post
                        updateProgress(player)
                    }
                }
            }

        }
    }

    private var timer: Timer? = null

    private fun updateTimeline(player: Player) {
        updateProgress(player)
    }

    protected val formatBuilder = StringBuilder()
    protected val formatter = Formatter(formatBuilder, Locale.getDefault())
    private var contentPosition = 0L

    private fun updateProgress(player: Player) {
        val realDurationMillis: Long = player.duration
        if (progressBarContainer?.isVisible == false) {
            return
        }
        if (progressBarContainer?.isVisible == true &&
            realDurationMillis < 10_000
        ) {
            progressBarContainer?.gone()
            return
        }
        progressBar?.max = realDurationMillis.toInt()
        contentPosition = player.contentPosition
        progressBar?.setSecondaryProgress(player.bufferedPosition.toInt())
        player.contentBufferedPosition
        progressBar?.progress = player.contentPosition.toInt()
        contentDurationView?.text = " ${Util.getStringForTime(formatBuilder, formatter, player.contentDuration)}"
        contentPositionView?.text = "${Util.getStringForTime(formatBuilder, formatter, player.contentPosition)} /"
//        exoPlayerManager.exoPlayer?.setSeekParameters(SeekParameters(10_000L, 10_000L))
    }

    open fun onPlayerPlaybackStateChanged(playbackState: Int) {

    }
    open fun onHandlePlayerError(error: PlaybackException) {
        Logger.e(this, tag = "onHandlePlayerError", exception = error)
    }

    private var mAutoHideTimeout: Long = 0

    private val mHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                Logger.d(this@BasePlaybackFragment, "HandlerUI", "${msg.what}")
            }
        }
    }
    private val autoHideOverlayRunnable by lazy {
        Runnable {
            isAnimationHideGridMenuShowVideoInfoRunning.set(false)
            mBrowseDummyView?.fadeOut {
                mGridViewOverlays?.translationY = mGridViewPickHeight
//                mBrowseDummyView?.setBackgroundColor(mLightOverlaysColor)
                mSelectedPosition = 0
                mGridViewHolder?.gridView?.setSelectedPositionSmooth(0)
                mPlayPauseIcon?.clearFocus()
                mGridViewHolder?.gridView?.clearFocus()
            }
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        mVideoSurface = LayoutInflater.from(context)
            .inflate(R.layout.core_layout_surfaces, root, false) as SurfaceView
        root.addView(mVideoSurface, 0)
        root.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Logger.d(this@BasePlaybackFragment, tag = "RootView", message = "Height: $mGridViewPickHeight")
                mGridViewPickHeight = root.height - DEFAULT_OVERLAY_PICK_HEIGHT
                mGridViewOverlays?.translationY = mGridViewPickHeight
                root.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        mVideoSurface!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mMediaPlaybackCallback?.surfaceCreated(holder)
                mState = SURFACE_CREATED
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                mMediaPlaybackCallback?.surfaceChanged(holder, format, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mMediaPlaybackCallback?.surfaceDestroyed(holder)
                mState = SURFACE_NOT_CREATED
            }
        })
        progressManager.initialDelay = 500
        progressManager.setRootView(root)
        backgroundType = BG_LIGHT
        val gridView = LayoutInflater.from(context)
            .inflate(R.layout.playback_vertical_grid_overlay, container, false)
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mPlaybackOverlaysContainerView = gridView
        gridView.layoutParams = layoutParams
        mGridViewOverlays = gridView.findViewById(R.id.browse_grid_dock)
        setupVerticalGridView(gridView)
        root.addView(gridView)
        mBackgroundView = root.findViewById(androidx.leanback.R.id.playback_fragment_background)
        mPlaybackTitleView = root.findViewById(R.id.playback_title)
        mPlaybackInfoView = root.findViewById(R.id.playback_info)
        mPlaybackInfoLiveView = root.findViewById(R.id.playback_live)
        mPlaybackInfoContainerView = root.findViewById(R.id.info_container)
        mBrowseDummyView = root.findViewById(R.id.browse_dummy)
        mPlayPauseIcon = root.findViewById(R.id.ic_play_pause)
        progressBar = root.findViewById(R.id.video_progress_bar)
        progressBarContainer = root.findViewById(R.id.progress_bar_container)
        contentPositionView = root.findViewById(R.id.content_position)
        contentDurationView = root.findViewById(R.id.content_duration)
        hideControlsOverlay(false)
        val controlBackground = root.findViewById<View>(androidx.leanback.R.id.playback_controls_dock)
        controlBackground.makeGone()
        mPlayPauseIcon?.setOnClickListener {
            onPlayPauseIconClicked()
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exoPlayerManager.prepare()
        exoPlayerManager.playerAdapter?.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)
        mTransportControlGlue = PlaybackTransportControlGlue(activity, exoPlayerManager.playerAdapter)
        mTransportControlGlue.host = mGlueHost
        mTransportControlGlue.isSeekEnabled = false

    }

    override fun onStart() {
        super.onStart()
        hideControlsOverlay(false)
        val controlBackground = view?.findViewById<View>(androidx.leanback.R.id.playback_controls_dock)
        controlBackground?.makeGone()
        if (timer == null) {
            timer = Timer()
            try {
                timer?.schedule(timerTask, 1_000, 1_000)
            } catch (_: Exception) {
                timer = null
            }
        }
    }


    private fun onPlayPauseIconClicked() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        try {
            if (true == exoPlayerManager.playerAdapter?.isPlaying) {
                exoPlayerManager.playerAdapter?.pause()
            } else {
                exoPlayerManager.playerAdapter?.play()
                mHandler.postDelayed(autoHideOverlayRunnable, 5000)
            }
        } catch (e: Exception) {
            Logger.e(this, exception = e)
        }
    }

    private fun setupVerticalGridView(gridView: View) {
        mGridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false).apply {
            shadowEnabled = false
        }
        mGridPresenter!!.numberOfColumns = numOfRowColumns
        mGridViewHolder = mGridPresenter!!.onCreateViewHolder(mGridViewOverlays)
        mGridViewOverlays?.addView(mGridViewHolder!!.view)
        mGridPresenter?.setOnItemViewSelectedListener { _, item, _, _ ->
        }
        mGridPresenter?.setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            (mAdapter as ArrayObjectAdapter).indexOf(item)
                .takeIf {
                    it > -1
                }?.let {
                    mSelectedPosition = it
                }
            onItemClickedListener?.onItemClicked(itemViewHolder, item, rowViewHolder, row)

        }
        mGridViewHolder!!.gridView.setOnChildLaidOutListener(mChildLaidOutListener)
        mGridPresenter!!.onBindViewHolder(mGridViewHolder, mAdapter)
        gridView.makeGone()
    }

    protected fun <T> setupRowAdapter(
        objectList: Map<String, List<T>>,
        presenterSelector: PresenterSelector
    ) {
        mAdapter = ArrayObjectAdapter(ListRowPresenter())
        for ((key, value) in objectList) {
            (mAdapter as ArrayObjectAdapter)
                .add(ListRow(HeaderItem(key), ArrayObjectAdapter(presenterSelector).apply {
                    this.addAll(0, value)
                }))
        }
        updateAdapter()
    }

    protected fun <T> setupRowAdapter(
        objectList: List<T>,
        presenterSelector: PresenterSelector,
        vararg related: List<T>
    ) {
        mPlayingPosition = mSelectedPosition
        Logger.d(this, message = "setupRowAdapter: $mSelectedPosition")
        val cardPresenterSelector: PresenterSelector = presenterSelector
        mAdapter = ArrayObjectAdapter(cardPresenterSelector)
        (mAdapter as ArrayObjectAdapter).addAll(0, objectList)
        if (related.isNotEmpty()) {
            for (i in related.indices) {
                (mAdapter as ArrayObjectAdapter).addAll(0, related[i])
            }
        }
        updateAdapter()
    }

    fun updateAdapter() {
        if (mGridViewHolder != null) {
            mGridPresenter!!.onBindViewHolder(mGridViewHolder, mAdapter)
            setSelectedPosition(max(0, mSelectedPosition))
        }
    }

    override fun setSelectedPosition(position: Int) {
        mSelectedPosition = position
        mGridViewHolder?.gridView?.setSelectedPositionSmooth(position)
    }

    private fun changeNextFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            progressBar?.focusable = if (progressBarContainer?.visibility == View.VISIBLE) {
                View.FOCUSABLE
            } else {
                View.NOT_FOCUSABLE
            }
        }

        mGridViewOverlays?.nextFocusUpId = if (progressBarContainer?.visibility == View.VISIBLE) {
            R.id.progress_bar_container
        } else {
            R.id.ic_play_pause
        }

        mPlayPauseIcon?.nextFocusDownId = if (progressBarContainer?.visibility == View.VISIBLE) {
            R.id.progress_bar_container
        } else {
            R.id.browse_grid_dock
        }


    }

    fun prepare(
        title: String,
        subTitle: String?,
        isLive: Boolean,
        showProgressManager: Boolean = true
    ) {
        if (showProgressManager) {
            progressManager.show()
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        setVideoInfo(title, subTitle, isLive)

        fadeInOverlay(false)
        if (isLive) {
            progressBarContainer?.gone()
        } else {
            progressBarContainer?.visible()
        }
    }

    fun fadeInOverlay(autoHide: Boolean = true) {
        mPlaybackOverlaysContainerView?.fadeIn()
        mPlaybackInfoContainerView?.fadeIn()
        mPlayPauseIcon?.fadeIn()
        if (mGridViewPickHeight > 0) {
            mGridViewOverlays?.translationY = mGridViewPickHeight
        }
//        mBrowseDummyView?.setBackgroundColor(mLightOverlaysColor)
        mPlayPauseIcon?.requestFocus()
        if (autoHide) {
            mHandler.removeCallbacks(autoHideOverlayRunnable)
            mHandler.postDelayed(autoHideOverlayRunnable, 5000)
        }
    }

    fun removeAutoHideCallback() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
    }

    fun playVideo(
        linkStreams: List<LinkStream>,
        playItemMetaData: Map<String, String>,
        headers: Map<String, String>?,
        isHls: Boolean,
        isLive: Boolean,
        listener: Player.Listener?,
        hideGridView: Boolean
    ) {
        progressManager.hide()
        mGlueHost.setSurfaceHolderCallback(null)
        exoPlayerManager.playVideo(
            linkStreams = linkStreams,
            isHls = isHls,
            itemMetaData = playItemMetaData,
            playerListener = listener ?: mPlayerListener,
            headers = headers
        )
        mTransportControlGlue = PlaybackTransportControlGlue(activity, exoPlayerManager.playerAdapter)
        mTransportControlGlue.host = mGlueHost
        mTransportControlGlue.title = playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_TITLE]
        mTransportControlGlue.subtitle = playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_DESCRIPTION]
        mTransportControlGlue.isSeekEnabled = true
        mTransportControlGlue.playWhenPrepared()
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
        if (hideGridView) {
            setVideoInfo(
                playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_TITLE],
                playItemMetaData[AbstractExoPlayerManager.EXTRA_MEDIA_DESCRIPTION],
                isLive
            )
            fadeInOverlay(false)
            setSelectedPosition(0)
        }
        if (isLive) {
            progressBarContainer?.gone()
        } else {
            progressBarContainer?.visible()
        }
        changeNextFocus()
        progressBar?.isActivated = false
        progressBar?.isFocusable = false
    }

    fun getBackgroundView(): View? {
        return mBackgroundView
    }

    fun hideProgressBar() {
        view?.findViewById<ProgressBar>(androidx.leanback.R.id.playback_progress)
            ?.makeGone()
    }


    override fun androidInjector(): AndroidInjector<Any> {
        return injector
    }

    /**
     * Adds [SurfaceHolder.Callback] to [android.view.SurfaceView].
     */
    fun setSurfaceHolderCallback(callback: SurfaceHolder.Callback?) {
        mMediaPlaybackCallback = callback
        if (callback != null) {
            if (mState == SURFACE_CREATED) {
                mMediaPlaybackCallback?.surfaceCreated(mVideoSurface!!.holder)
            }
        }
    }

    private fun setVideoInfo(title: String?, description: String?, isLive: Boolean = false) {
        if (!mPlaybackTitleView?.text.toString().equals(title, ignoreCase = true)) {
            mPlaybackTitleView?.text = title
        }
        if (!mPlaybackInfoView?.text.toString().equals(description, ignoreCase = true)) {
            mPlaybackInfoView?.text = description?.trim()
        }
        if (mPlaybackTitleView?.isSelected != true) {
            mPlaybackTitleView?.isSelected = true
        }
        if (description == null) {
            mPlaybackInfoView?.gone()
        } else {
            mPlaybackInfoView?.visible()
        }
        if (isLive) {
            mPlaybackInfoLiveView?.visible()
        } else {
            mPlaybackInfoLiveView?.gone()
        }
        mPlaybackInfoContainerView?.fadeIn()
        mPlaybackOverlaysContainerView?.visibility = View.VISIBLE
        if (mGridViewPickHeight > 0) {
            mSelectedPosition = 0
            mGridViewHolder?.gridView?.setSelectedPositionSmooth(0)
            mGridViewHolder?.gridView?.clearFocus()
            mPlayPauseIcon?.requestFocus()
            mGridViewOverlays?.translateY(mGridViewPickHeight) {
//                mBrowseDummyView?.setBackgroundColor(mLightOverlaysColor)
            }
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        val screenWidth = requireView().width
        val screenHeight = requireView().height
        val p = mVideoSurface!!.layoutParams
        if (screenWidth * height > width * screenHeight) {
            p.height = screenHeight
            p.width = screenHeight * width / height
        } else {
            p.width = screenWidth
            p.height = screenWidth * height / width
        }
        mVideoSurface!!.layoutParams = p
    }

    /**
     * Returns the surface view.
     */
    fun getSurfaceView(): SurfaceView? {
        return mVideoSurface
    }

    fun canBackToMain(): Boolean {
        return mPlaybackOverlaysContainerView?.visibility != View.VISIBLE
    }

    fun hideOverlay() {
        autoHideOverlayRunnable.run()
    }

    override fun onDestroyView() {
        mVideoSurface = null
        mState = SURFACE_NOT_CREATED
        super.onDestroyView()
    }

    override fun onDpadCenter() {
        showGridMenu()
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
    }

    fun isMenuShowed(): Boolean {
        return mPlaybackOverlaysContainerView?.visibility == View.VISIBLE
    }

    private fun showGridMenu(): Boolean {
        if (mPlaybackInfoContainerView == null) {
            Logger.d(
                this, "DpadCenter", "{" +
                        "mPlaybackInfoContainerView null" +
                        "}"
            )
            return true
        }
        val visible = mPlaybackOverlaysContainerView?.visibility == View.VISIBLE
        Logger.d(
            this, "DpadCenter", "{" +
                    "mPlaybackOverlaysContainerView visibility: ${mPlaybackOverlaysContainerView?.visibility}" +
                    "mPlaybackOverlaysContainerView alpha: ${mPlaybackOverlaysContainerView?.alpha}" +
                    "}"
        )
        if (!visible || mPlaybackOverlaysContainerView!!.alpha < 1f) {
            fadeInOverlay(false)
            setSelectedPosition(0)
        }
        return false
    }

    override fun onDpadDown() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)

        if ((mPlayPauseIcon?.isFocused == true) && (progressBarContainer?.visibility == View.VISIBLE)
            && progressBar?.isActivated == true
        ) {
            progressBar?.requestFocus()
        } else if (isMenuShowed()) {
            Logger.d(this, message = "y = ${mGridViewOverlays?.translationY} - $mGridViewPickHeight")
            mPlaybackInfoContainerView?.fadeOut {
                isAnimationHideGridMenuShowVideoInfoRunning.set(false)
            }
            mPlayPauseIcon?.fadeOut()
            if (mGridViewOverlays!!.translationY == mGridViewPickHeight) {
                mGridViewOverlays?.translateY(0f) {
                    Logger.d(this, message = "translateY = ${mGridViewOverlays?.translationY} - $mGridViewPickHeight")
                    mHandler.removeCallbacks(autoHideOverlayRunnable)
                    mGridViewOverlays?.visible()
                    mGridViewHolder?.gridView?.requestFocus()
//                    mBrowseDummyView?.setBackgroundColor(mDarkOverlayColor)
                }
            }
        } else {
            showGridMenu()
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)

    }

    private val isAnimationHideGridMenuShowVideoInfoRunning by lazy {
        AtomicBoolean()
    }

    open fun getSearchFilter(): String {
        return ""
    }

    open fun getSearchHint(): String? {
        return null
    }

    override fun onDpadUp() {
        if (isAnimationHideGridMenuShowVideoInfoRunning.get()) {
            return
        }
        when {
            !isMenuShowed() -> {
                showGridMenu()
            }

            progressBarContainer?.visibility == View.VISIBLE && progressBar?.isFocused == true -> {
                isAnimationHideGridMenuShowVideoInfoRunning.set(false)
            }

            mPlayPauseIcon?.isFocused == true -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "xemtv://iptv/search?" +
                                    "filter=${getSearchFilter()}" +
                                    (getSearchHint()?.let {
                                        "&query_hint=$it"
                                    } ?: "")
                        )
                    )
                )
                isAnimationHideGridMenuShowVideoInfoRunning.set(false)
            }

            mGridViewHolder!!.gridView.selectedPosition in 0..4 -> {
                isAnimationHideGridMenuShowVideoInfoRunning.set(true)
                mGridViewHolder?.gridView?.clearFocus()
                mPlaybackInfoContainerView?.fadeIn {
                    focusOnDpadUp()
                    isAnimationHideGridMenuShowVideoInfoRunning.set(false)
                }
                mPlayPauseIcon?.fadeIn()
                mGridViewOverlays?.translateY(mGridViewPickHeight, onAnimationEnd = {
//                    mBrowseDummyView?.setBackgroundColor(mLightOverlaysColor)
                    mPlaybackInfoContainerView?.alpha = 1f
                    focusOnDpadUp()
                    isAnimationHideGridMenuShowVideoInfoRunning.set(false)
                }, onAnimationCancel = {
                    isAnimationHideGridMenuShowVideoInfoRunning.set(false)
                    mPlaybackInfoContainerView?.alpha = 1f
                    focusOnDpadUp()
                    mGridViewOverlays?.translationY = mGridViewPickHeight
                })
            }
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
    }

    private fun focusOnDpadUp() {
        if (progressBarContainer?.isVisible == true && progressBar?.isActivated == true) {
            progressBar?.requestFocus()
        } else {
            mPlayPauseIcon?.requestFocus()
        }
    }

    override fun onDpadLeft() {
        if (progressBar?.isFocused == true) {
            exoPlayerManager.exoPlayer?.seekTo(contentPosition - MIN_SEEK_DURATION)
            exoPlayerManager.exoPlayer?.let {
                updateProgress(it)
            }
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
    }

    override fun onKeyCodeForward() {
        exoPlayerManager.exoPlayer?.seekTo(contentPosition + MIN_SEEK_DURATION)
    }

    override fun onKeyCodeRewind() {
        exoPlayerManager.exoPlayer?.seekTo(contentPosition - MIN_SEEK_DURATION)
    }

    override fun onDpadRight() {
        if (progressBar?.isFocused == true) {
            exoPlayerManager.exoPlayer?.seekTo(contentPosition + MIN_SEEK_DURATION)
            exoPlayerManager.exoPlayer?.let {
                updateProgress(it)
            }
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
    }

    override fun onKeyCodeChannelUp() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
    }

    override fun onKeyCodeChannelDown() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
    }

    override fun onKeyCodeMediaNext() {
    }

    override fun onKeyCodeVolumeDown() {
    }

    override fun onKeyCodeVolumeUp() {
    }

    override fun onKeyCodeMediaPrevious() {
    }

    override fun onKeyCodePause() {
    }

    override fun onKeyCodePlay() {
    }

    override fun onKeyCodeMenu() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        if (mPlaybackOverlaysContainerView?.visibility != View.VISIBLE) {
            mPlaybackOverlaysContainerView?.fadeIn {
                mPlaybackInfoContainerView?.gone()
                mPlayPauseIcon?.fadeOut()
                mGridViewOverlays?.translateY(0f) {
                    mGridViewOverlays?.visible()
                    mGridViewHolder?.gridView?.requestFocus()
                }
            }
        } else if (mPlaybackInfoContainerView?.visibility == View.VISIBLE) {
            mGridViewOverlays?.translateY(0f) {
                mPlaybackInfoContainerView?.gone()
                mGridViewOverlays?.visible()
                mGridViewHolder?.gridView?.requestFocus()
                mPlayPauseIcon?.fadeOut()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (mGridViewPickHeight == mGridViewOverlays?.translationY) {
            mPlayPauseIcon?.requestFocus()
        } else {
            mGridViewOverlays?.requestFocus()
        }
    }

    class BasePlaybackSupportFragmentGlueHost(
        private val mFragment: BasePlaybackFragment
    ) : PlaybackSupportFragmentGlueHost(mFragment), SurfaceHolderGlueHost {
        override fun setSurfaceHolderCallback(callback: SurfaceHolder.Callback?) {
            mFragment.setSurfaceHolderCallback(callback)
        }
    }

    override fun onDetach() {
        Logger.d(this, message = "onDetach")
        mGridViewOverlays = null
        mPlayPauseIcon = null
        mPlaybackOverlaysContainerView = null
        mBackgroundView = null
        mVideoSurface = null
        mBrowseDummyView = null
        exoPlayerManager.detach(mPlayerListener)
        mGlueHost.setSurfaceHolderCallback(null)
        setSurfaceHolderCallback(null)
        mMediaPlaybackCallback = null
        super.onDetach()
    }

    override fun onStop() {
        timer?.cancel()
        timer = null
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val MAX_RETRY_TIME = 3

        private val DEFAULT_OVERLAY_PICK_HEIGHT by lazy {
            200.dpToPx().toFloat()
        }
        private val mLightOverlaysColor by lazy {
            Color.parseColor("#80000000")
        }
        private val mDarkOverlayColor by lazy {
            Color.parseColor("#B3000000")
        }
        private const val SURFACE_NOT_CREATED = 0
        private const val SURFACE_CREATED = 1
    }
}