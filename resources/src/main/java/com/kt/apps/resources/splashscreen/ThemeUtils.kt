package com.kt.apps.resources.splashscreen

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

internal object ThemeUtils {
    object Api31 {

        /**
         * Apply the theme's values for the system bar appearance to the decorView.
         *
         * This needs to be called when the [SplashScreenViewProvider] is added and after it's been
         * removed.
         */
        @RequiresApi(30)
        @JvmStatic
        @JvmOverloads
        @DoNotInline
        fun applyThemesSystemBarAppearance(
            theme: Resources.Theme,
            decor: View,
            tv: TypedValue = TypedValue()
        ) {
            var appearance = 0
            val mask =
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            if (theme.resolveAttribute(android.R.attr.windowLightStatusBar, tv, true)) {
                if (tv.data != 0) {
                    appearance = appearance or WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                }
            }
            if (theme.resolveAttribute(android.R.attr.windowLightNavigationBar, tv, true)) {
                if (tv.data != 0) {
                    appearance =
                        appearance or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                }
            }
            decor.windowInsetsController!!.setSystemBarsAppearance(appearance, mask)
        }
    }
}