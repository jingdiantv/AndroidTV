package com.kt.apps.resources.splashscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.window.SplashScreenView
import androidx.annotation.RequiresApi
import com.kt.apps.resources.R

@SuppressLint("ViewConstructor")
public class SplashScreenViewProvider internal constructor(ctx: Activity) {

    @RequiresApi(31)
    internal constructor(platformView: SplashScreenView, ctx: Activity) : this(ctx) {
        (impl as ViewImpl31).platformView = platformView
    }

    private val impl: ViewImpl = when {
        Build.VERSION.SDK_INT >= 31 -> ViewImpl31(ctx)
        else -> ViewImpl(ctx)
    }.apply {
        createSplashScreenView()
    }

    /**
     * The splash screen view, copied into this application process.
     *
     * This view can be used to create custom animation from the splash screen to the application
     */
    public val view: View get() = impl.splashScreenView

    /**
     * The view containing the splashscreen icon as defined by
     * [R.attr.windowSplashScreenAnimatedIcon]
     */
    public val iconView: View get() = impl.iconView

    /**
     * Start time of the icon animation.
     *
     * On API 31+, returns the number of millisecond since the Epoch time (1970-1-1T00:00:00Z)
     *
     * Below API 31, returns 0 because the icon cannot be animated.
     */
    public val iconAnimationStartMillis: Long get() = impl.iconAnimationStartMillis

    /**
     * Duration of the icon animation as provided in [R.attr.
     */
    public val iconAnimationDurationMillis: Long get() = impl.iconAnimationDurationMillis

    /**
     * Remove the SplashScreen's view from the view hierarchy.
     *
     * This always needs to be called when an
     * [androidx.core.splashscreen.SplashScreen.OnExitAnimationListener]
     * is set.
     */
    public fun remove(): Unit = impl.remove()

    private open class ViewImpl(val activity: Activity) {

        private val _splashScreenView: ViewGroup by lazy {
            FrameLayout.inflate(
                activity,
                R.layout.splash_screen_view,
                null
            ) as ViewGroup
        }

        open fun createSplashScreenView() {
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            (content.rootView as? ViewGroup)?.addView(_splashScreenView)
        }

        open val splashScreenView: ViewGroup get() = _splashScreenView
        open val iconView: View get() = splashScreenView.findViewById(R.id.splashscreen_icon_view)
        open val iconAnimationStartMillis: Long get() = 0
        open val iconAnimationDurationMillis: Long get() = 0
        open fun remove() {
            (splashScreenView.parent as? ViewGroup)?.removeView(splashScreenView)
        }
    }

    @RequiresApi(31)
    private class ViewImpl31(activity: Activity) : ViewImpl(activity) {
        lateinit var platformView: SplashScreenView

        override fun createSplashScreenView() {
            // Do nothing
        }

        override val splashScreenView get() = platformView

        override val iconView: View
            get() = if (platformView.iconView != null) platformView.iconView!! else View(activity)

        override val iconAnimationStartMillis: Long
            get() = platformView.iconAnimationStart?.toEpochMilli() ?: 0

        override val iconAnimationDurationMillis: Long
            get() = platformView.iconAnimationDuration?.toMillis() ?: 0

        override fun remove() {
            platformView.remove()
            ThemeUtils.Api31.applyThemesSystemBarAppearance(
                activity.theme,
                activity.window.decorView
            )
        }
    }
}