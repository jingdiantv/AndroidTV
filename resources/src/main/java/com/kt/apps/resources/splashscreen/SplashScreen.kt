package com.kt.apps.resources.splashscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import android.window.SplashScreenView
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.kt.apps.resources.R

@SuppressLint("CustomSplashScreen")
class SplashScreen private constructor(activity: Activity) {

    private val impl = when {
        Build.VERSION.SDK_INT >= 31 -> Impl31(activity)
        else -> Impl(activity)
    }

    public companion object {

        private const val MASK_FACTOR = 2 / 3f

        /**
         * Creates a [SplashScreen] instance associated with this [Activity] and handles
         * setting the theme to [R.attr.postSplashScreenTheme].
         *
         * This needs to be called before [Activity.setContentView] or other view operations on
         * the root view (e.g setting flags).
         *
         * Alternatively, if a [SplashScreen] instance is not required, the theme can manually be
         * set using [Activity.setTheme].
         */
        @JvmStatic
        public fun Activity.installSplashScreen(): SplashScreen {
            val splashScreen = SplashScreen(this)
            splashScreen.install()
            return splashScreen
        }
    }

    /**
     * Sets the condition to keep the splash screen visible.
     *
     * The splash will stay visible until the condition isn't met anymore.
     * The condition is evaluated before each request to draw the application, so it needs to be
     * fast to avoid blocking the UI.
     *
     * @param condition The condition evaluated to decide whether to keep the splash screen on
     * screen
     */
    public fun setKeepOnScreenCondition(condition: KeepOnScreenCondition) {
        impl.setKeepOnScreenCondition(condition)
    }

    /**
     * Sets a listener that will be called when the splashscreen is ready to be removed.
     *
     * If a listener is set, the splashscreen won't be automatically removed and the application
     * needs to manually call [SplashScreenViewProvider.remove].
     *
     * IF no listener is set, the splashscreen will be automatically removed once the app is
     * ready to draw.
     *
     * The listener will be called on the ui thread.
     *
     * @param listener The [OnExitAnimationListener] that will be called when the splash screen
     * is ready to be dismissed.
     *
     * @see setKeepOnScreenCondition
     * @see OnExitAnimationListener
     * @see SplashScreenViewProvider
     */
    @SuppressWarnings("ExecutorRegistration") // Always runs on the MainThread
    public fun setOnExitAnimationListener(listener: OnExitAnimationListener) {
        impl.setOnExitAnimationListener(listener)
    }

    private fun install() {
        impl.install()
    }

    /**
     * Listener to be passed in [SplashScreen.setOnExitAnimationListener].
     *
     * The listener will be called once the splash screen is ready to be removed and provides a
     * reference to a [SplashScreenViewProvider] that can be used to customize the exit
     * animation of the splash screen.
     */
    public fun interface OnExitAnimationListener {

        /**
         * Callback called when the splash screen is ready to be dismissed. The caller is
         * responsible for animating and removing splash screen using the provided
         * [splashScreenViewProvider].
         *
         * The caller **must** call [SplashScreenViewProvider.remove] once it's done with the
         * splash screen.
         *
         * @param splashScreenViewProvider An object holding a reference to the displayed splash
         * screen.
         */
        @MainThread
        public fun onSplashScreenExit(splashScreenViewProvider: SplashScreenViewProvider)
    }

    /**
     * Condition evaluated to check if the splash screen should remain on screen
     *
     * The splash screen will stay visible until the condition isn't met anymore.
     * The condition is evaluated before each request to draw the application, so it needs to be
     * fast to avoid blocking the UI.
     */
    public fun interface KeepOnScreenCondition {

        /**
         * Callback evaluated before every requests to draw the Activity. If it returns `true`, the
         * splash screen will be kept visible to hide the Activity below.
         *
         * This callback is evaluated in the main thread.
         */
        @MainThread
        public fun shouldKeepOnScreen(): Boolean
    }

    private open class Impl(val activity: Activity) {
        var finalThemeId: Int = 0
        var backgroundResId: Int? = null
        var backgroundColor: Int? = null
        var icon: Drawable? = null
        var hasBackground: Boolean = false

        var splashScreenWaitPredicate = KeepOnScreenCondition { false }
        private var animationListener: OnExitAnimationListener? = null
        private var mSplashScreenViewProvider: SplashScreenViewProvider? = null

        open fun install() {
            val typedValue = TypedValue()
            val currentTheme = activity.theme
            if (currentTheme.resolveAttribute(
                    R.attr.windowSplashScreenBackground,
                    typedValue,
                    true
                )
            ) {
                backgroundResId = typedValue.resourceId
                backgroundColor = typedValue.data
            }
            if (currentTheme.resolveAttribute(
                    R.attr.windowSplashScreenAnimatedIcon,
                    typedValue,
                    true
                )
            ) {
                icon = currentTheme.getDrawable(typedValue.resourceId)
            }

            if (currentTheme.resolveAttribute(R.attr.splashScreenIconSize, typedValue, true)) {
                hasBackground =
                    typedValue.resourceId == R.dimen.splashscreen_icon_size_with_background
            }
            setPostSplashScreenTheme(currentTheme, typedValue)
        }

        protected fun setPostSplashScreenTheme(
            currentTheme: Resources.Theme,
            typedValue: TypedValue
        ) {
            if (currentTheme.resolveAttribute(R.attr.postSplashScreenTheme, typedValue, true)) {
                finalThemeId = typedValue.resourceId
                if (finalThemeId != 0) {
                    activity.setTheme(finalThemeId)
                }
            }
        }

        open fun setKeepOnScreenCondition(keepOnScreenCondition: KeepOnScreenCondition) {
            splashScreenWaitPredicate = keepOnScreenCondition
            val contentView = activity.findViewById<View>(android.R.id.content)
            val observer = contentView.viewTreeObserver
            observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (splashScreenWaitPredicate.shouldKeepOnScreen()) {
                        return false
                    }
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                    mSplashScreenViewProvider?.let(::dispatchOnExitAnimation)
                    return true
                }
            })
        }

        open fun setOnExitAnimationListener(exitAnimationListener: OnExitAnimationListener) {
            animationListener = exitAnimationListener

            val splashScreenViewProvider = SplashScreenViewProvider(activity)
            val finalBackgroundResId = backgroundResId
            val finalBackgroundColor = backgroundColor
            val splashScreenView = splashScreenViewProvider.view

            if (finalBackgroundResId != null && finalBackgroundResId != Resources.ID_NULL) {
                splashScreenView.setBackgroundResource(finalBackgroundResId)
            } else if (finalBackgroundColor != null) {
                splashScreenView.setBackgroundColor(finalBackgroundColor)
            } else {
                splashScreenView.background = activity.window.decorView.background
            }

            icon?.let { displaySplashScreenIcon(splashScreenView, it) }

            splashScreenView.addOnLayoutChangeListener(
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        view: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int
                    ) {
                        if (!view.isAttachedToWindow) {
                            return
                        }

                        view.removeOnLayoutChangeListener(this)
                        if (!splashScreenWaitPredicate.shouldKeepOnScreen()) {
                            dispatchOnExitAnimation(splashScreenViewProvider)
                        } else {
                            mSplashScreenViewProvider = splashScreenViewProvider
                        }
                    }
                })
        }

        private fun displaySplashScreenIcon(splashScreenView: View, icon: Drawable) {
            val iconView = splashScreenView.findViewById<ImageView>(R.id.splashscreen_icon_view)
            iconView.apply {
                val maskSize: Float
                if (hasBackground) {
                    // If the splash screen has an icon background we need to mask both the
                    // background and foreground.
                    val iconBackgroundDrawable = ContextCompat.getDrawable(
                        context,
                        R.drawable.icon_background
                    )

                    val iconSize =
                        resources.getDimension(R.dimen.splashscreen_icon_size_with_background)
                    maskSize = iconSize * MASK_FACTOR

                    if (iconBackgroundDrawable != null) {
                        background = MaskedDrawable(iconBackgroundDrawable, maskSize)
                    }
                } else {
                    val iconSize =
                        resources.getDimension(R.dimen.splashscreen_icon_size_no_background)
                    maskSize = iconSize * MASK_FACTOR
                }
                setImageDrawable(MaskedDrawable(icon, maskSize))
            }
        }

        fun dispatchOnExitAnimation(splashScreenViewProvider: SplashScreenViewProvider) {
            val finalListener = animationListener ?: return
            animationListener = null
            splashScreenViewProvider.view.postOnAnimation {
                splashScreenViewProvider.view.bringToFront()
                finalListener.onSplashScreenExit(splashScreenViewProvider)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private class Impl31(activity: Activity) : Impl(activity) {
        var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null
        var mDecorFitWindowInsets = true

        val hierarchyListener = object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {

                if (child is SplashScreenView) {
                    /*
                     * On API 31, the SplashScreenView sets window.setDecorFitsSystemWindows(false)
                     * when an OnExitAnimationListener is used. This also affects the application
                     * content that will be pushed up under the status bar even though it didn't
                     * requested it. And once the SplashScreenView is removed, the whole layout
                     * jumps back below the status bar. Fortunately, this happens only after the
                     * view is attached, so we have time to record the value of
                     * window.setDecorFitsSystemWindows() before the splash screen modifies it and
                     * reapply the correct value to the window.
                     */
                    mDecorFitWindowInsets = computeDecorFitsWindow(child)
                    (activity.window.decorView as ViewGroup).setOnHierarchyChangeListener(null)
                }
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
                // no-op
            }
        }

        fun computeDecorFitsWindow(child: SplashScreenView): Boolean {
            val inWindowInsets = WindowInsets.Builder().build()
            val outLocalInsets = Rect(
                Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE,
                Int.MAX_VALUE
            )

            // If setDecorFitWindowInsets is set to false, computeSystemWindowInsets
            // will return the same instance of WindowInsets passed in its parameter and
            // will set outLocalInsets to empty, so we check that both conditions are
            // filled to extrapolate the value of setDecorFitWindowInsets
            return !(inWindowInsets === child.rootView.computeSystemWindowInsets
                (inWindowInsets, outLocalInsets) && outLocalInsets.isEmpty)
        }

        override fun install() {
            setPostSplashScreenTheme(activity.theme, TypedValue())
            (activity.window.decorView as ViewGroup).setOnHierarchyChangeListener(
                hierarchyListener
            )
        }

        override fun setKeepOnScreenCondition(keepOnScreenCondition: KeepOnScreenCondition) {
            splashScreenWaitPredicate = keepOnScreenCondition
            val contentView = activity.findViewById<View>(android.R.id.content)
            val observer = contentView.viewTreeObserver

            if (preDrawListener != null && observer.isAlive) {
                observer.removeOnPreDrawListener(preDrawListener)
            }
            preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (splashScreenWaitPredicate.shouldKeepOnScreen()) {
                        return false
                    }
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            }
            observer.addOnPreDrawListener(preDrawListener)
        }

        override fun setOnExitAnimationListener(
            exitAnimationListener: OnExitAnimationListener
        ) {
            activity.splashScreen.setOnExitAnimationListener { splashScreenView ->
                applyAppSystemUiTheme()
                val splashScreenViewProvider = SplashScreenViewProvider(splashScreenView, activity)
                exitAnimationListener.onSplashScreenExit(splashScreenViewProvider)
            }
        }

        /**
         * Apply the system ui related theme attribute defined in the application to override the
         * ones set on the [SplashScreenView]
         *
         * On API 31, if an OnExitAnimationListener is set, the Window layout params are only
         * applied only when the [SplashScreenView] is removed. This lead to some
         * flickers.
         *
         * To fix this, we apply these attributes as soon as the [SplashScreenView]
         * is visible.
         */
        private fun applyAppSystemUiTheme() {
            val tv = TypedValue()
            val theme = activity.theme
            val window = activity.window

            if (theme.resolveAttribute(android.R.attr.statusBarColor, tv, true)) {
                window.statusBarColor = tv.data
            }

            if (theme.resolveAttribute(android.R.attr.navigationBarColor, tv, true)) {
                window.navigationBarColor = tv.data
            }

            if (theme.resolveAttribute(android.R.attr.windowDrawsSystemBarBackgrounds, tv, true)) {
                if (tv.data != 0) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                }
            }

            if (theme.resolveAttribute(android.R.attr.enforceNavigationBarContrast, tv, true)) {
                window.isNavigationBarContrastEnforced = tv.data != 0
            }

            if (theme.resolveAttribute(android.R.attr.enforceStatusBarContrast, tv, true)) {
                window.isStatusBarContrastEnforced = tv.data != 0
            }

            val decorView = window.decorView as ViewGroup
            com.kt.apps.resources.splashscreen.ThemeUtils.Api31.applyThemesSystemBarAppearance(theme, decorView, tv)

            // Fix setDecorFitsSystemWindows being overridden by the SplashScreenView
            decorView.setOnHierarchyChangeListener(null)
            window.setDecorFitsSystemWindows(mDecorFitWindowInsets)
        }
    }
}