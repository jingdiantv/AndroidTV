package com.kt.apps.core.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.app.PlaybackSupportFragmentGlueHost
import androidx.leanback.app.ProgressBarManager
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.SurfaceHolderGlueHost
import androidx.leanback.widget.*
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.kt.apps.core.R
import com.kt.apps.core.base.logging.Logger
import com.kt.apps.core.base.player.ExoPlayerManager
import com.kt.apps.core.base.player.LinkStream
import com.kt.apps.core.utils.*
import com.kt.skeleton.makeGone
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import kotlin.math.max

abstract class BasePlaybackFragment : PlaybackSupportFragment(),
    HasAndroidInjector, IKeyCodeHandler {
    private val progressManager by lazy {
        ProgressBarManager()
    }

    @Inject
    lateinit var injector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var exoPlayerManager: ExoPlayerManager

    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<LeanbackPlayerAdapter>
    private val glueHost by lazy {
        BasePlaybackSupportFragmentGlueHost(this@BasePlaybackFragment)
    }
    protected var mAdapter: ObjectAdapter? = null
    protected var mGridViewHolder: VerticalGridPresenter.ViewHolder? = null
    protected var mGridPresenter: VerticalGridPresenter? = null
    protected val mChildLaidOutListener = OnChildLaidOutListener { _, _, position, _ ->
        Logger.d(this@BasePlaybackFragment, message = "childLaidOutPosition: $position")
    }
    private var mVideoSurface: SurfaceView? = null
    private var mMediaPlaybackCallback: SurfaceHolder.Callback? = null
    private var mState = SURFACE_NOT_CREATED
    private var mSelectedPosition = -1
    private var mPlayingPosition = -1
    private var mPlaybackInfoContainerView: View? = null
    private var mPlayPauseIcon: ImageButton? = null
    protected var mGridView: View? = null
    protected var onItemClickedListener: OnItemViewClickedListener? = null
    abstract val numOfRowColumns: Int
    private val _playerListener by lazy {
        object : Player.Listener {
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
        }
    }
    open fun onHandlePlayerError(error: PlaybackException) {
        Logger.e(this, tag = "onHandlePlayerError", exception = error)
    }

    private val mHandler by lazy {
        Handler(Looper.getMainLooper())
    }
    private val autoHideOverlayRunnable by lazy {
        Runnable {
            view?.findViewById<FrameLayout>(R.id.browse_dummy)?.startHideOrShowAnimation(false) {

            }
            view?.findViewById<FrameLayout>(R.id.browse_grid_dock)?.translationY = 1000.dpToPx().toFloat()
            view?.findViewById<FrameLayout>(R.id.browse_dummy)?.setBackgroundColor(Color.parseColor("#80000000"))
            mSelectedPosition = 0
            mGridViewHolder?.gridView?.setSelectedPositionSmooth(0)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoPlayerManager.prepare()
        exoPlayerManager.playerAdapter?.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)
        mTransportControlGlue = PlaybackTransportControlGlue(activity, exoPlayerManager.playerAdapter)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        mVideoSurface = LayoutInflater.from(context)
            .inflate(R.layout.core_layout_surfaces, root, false) as SurfaceView

        root.addView(mVideoSurface, 0)
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
        mPlaybackInfoContainerView = gridView.findViewById(R.id.browse_grid_dock)
        gridView.layoutParams = layoutParams
        setupVerticalGridView(gridView)
        root.addView(gridView)
        hideControlsOverlay(false)
        val controlBackground = root.findViewById<View>(androidx.leanback.R.id.playback_controls_dock)
        controlBackground.makeGone()
        mPlayPauseIcon?.setOnClickListener {
            mHandler.removeCallbacks(autoHideOverlayRunnable)
            if (true == exoPlayerManager.playerAdapter?.isPlaying) {
                exoPlayerManager.playerAdapter?.pause()
            } else {
                exoPlayerManager.playerAdapter?.play()
                mHandler.postDelayed(autoHideOverlayRunnable, 5000)
            }
        }
        return root
    }

    private fun setupVerticalGridView(gridView: View) {
        mGridView = gridView
        mPlayPauseIcon = gridView.findViewById(R.id.ic_play_pause)
        val gridDock = gridView.findViewById<FrameLayout>(R.id.browse_grid_dock)
        mGridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM).apply {
            shadowEnabled = false
        }
        mGridPresenter!!.numberOfColumns = numOfRowColumns
        mGridViewHolder = mGridPresenter!!.onCreateViewHolder(gridDock)
        gridDock.addView(mGridViewHolder!!.view)
        mGridPresenter?.setOnItemViewSelectedListener { _, item, _, _ ->
//            val position = mGridViewHolder?.gridView?.selectedPosition ?: 0
//            mSelectedPosition = position
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
        objectList: List<T>,
        presenterSelector: PresenterSelector
    ) {
        mPlayingPosition = mSelectedPosition
        Logger.d(this, message = "setupRowAdapter: $mSelectedPosition")
        val cardPresenterSelector: PresenterSelector = presenterSelector
        mAdapter = ArrayObjectAdapter(cardPresenterSelector)
        (mAdapter as ArrayObjectAdapter).addAll(0, objectList)
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

    fun playVideo(title: String, subTitle: String, referer: String, linkStream: List<String>, isLive: Boolean) {
        playVideo(title, subTitle, linkStream.map {
            LinkStream(it, referer, title)
        }, _playerListener, isLive)
    }

    fun playVideo(
        title: String, subTitle: String,
        linkStreams: List<LinkStream>,
        listener: Player.Listener? = null,
        isLive: Boolean
    ) {
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.title = title
        mTransportControlGlue.subtitle = subTitle
        mTransportControlGlue.isSeekEnabled = false
        mTransportControlGlue.playWhenPrepared()
        exoPlayerManager.playVideo(linkStreams, listener ?: _playerListener)
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
        setVideoInfo(title, subTitle, isLive)
    }

    private fun buildMediaSource(referer: String): DefaultMediaSourceFactory {
        val dfSource: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        dfSource.setDefaultRequestProperties(
            getHeaderFromLinkStream(referer, "")
        )
        return DefaultMediaSourceFactory(dfSource)
            .setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
            })

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

    fun setVideoInfo(title: String?, info: String?, isLive: Boolean = false) {
        requireView().findViewById<TextView>(R.id.playback_title)!!.text = title
        view?.findViewById<TextView>(R.id.playback_info)?.text = info
        if (isLive) {
            view?.findViewById<TextView>(R.id.playback_live)!!.visible()
        } else {
            view?.findViewById<TextView>(R.id.playback_live)!!.gone()
        }

        requireView().findViewById<LinearLayout>(R.id.info_container).visible()
        mGridView?.visibility = View.VISIBLE
        mPlayPauseIcon?.requestFocus()
        view?.findViewById<FrameLayout>(R.id.browse_grid_dock)!!
            .animate()
            ?.translationY(1000.dpToPx().toFloat())
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    view?.findViewById<FrameLayout>(R.id.browse_dummy)?.setBackgroundColor(Color.parseColor("#80000000"))
                }
            })
            ?.start()
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
        return mGridView?.visibility != View.VISIBLE
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
        val visible = mGridView?.visibility == View.VISIBLE
        if (!visible) {
            requireView().findViewById<LinearLayout>(R.id.info_container).visible()
            mGridView?.visibility = View.VISIBLE
            mPlayPauseIcon?.requestFocus()
            setSelectedPosition(0)
        }
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        mHandler.postDelayed(autoHideOverlayRunnable, 5000)
    }

    override fun onDpadDown() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
        Logger.d(this, message = "y = ${view?.findViewById<FrameLayout>(R.id.browse_grid_dock)!!.translationY}")

        val visible = mGridView?.visibility == View.VISIBLE
        if (visible) {
            Logger.d(this, message = "y = ${view?.findViewById<FrameLayout>(R.id.browse_grid_dock)!!.translationY}")
            if (mPlayPauseIcon?.visibility == View.VISIBLE) {
                requireView().findViewById<LinearLayout>(R.id.info_container).startHideOrShowAnimation(false) {
                }
            }
            if (view?.findViewById<FrameLayout>(R.id.browse_grid_dock)!!.translationY >= 350.dpToPx().toFloat()) {
                view?.findViewById<FrameLayout>(R.id.browse_grid_dock)!!
                    .animate()
                    ?.translationY(0f)
                    ?.setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            view?.findViewById<FrameLayout>(R.id.browse_dummy)?.setBackgroundColor(Color.parseColor("#B3000000"))
                        }
                    })
                    ?.start()
            }
        }
    }

    override fun onDpadUp() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
    }

    override fun onDpadLeft() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
    }

    override fun onDpadRight() {
        mHandler.removeCallbacks(autoHideOverlayRunnable)
    }

    override fun onKeyCodeChannelUp() {
    }

    override fun onKeyCodeChannelDown() {
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

    class BasePlaybackSupportFragmentGlueHost(
        private val mFragment: BasePlaybackFragment
    ) : PlaybackSupportFragmentGlueHost(mFragment), SurfaceHolderGlueHost {
        override fun setSurfaceHolderCallback(callback: SurfaceHolder.Callback?) {
            mFragment.setSurfaceHolderCallback(callback)
        }
    }

    override fun onDetach() {
        Logger.d(this, message = "onDetach")
        mTransportControlGlue.host = null
        exoPlayerManager.detach()
        setSurfaceHolderCallback(null)
        super.onDetach()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val SURFACE_NOT_CREATED = 0
        private const val SURFACE_CREATED = 1
    }
}