package com.aliothmoon.maameow

import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.presentation.navigation.AppNavigation
import com.aliothmoon.maameow.theme.MaaMeowTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    @Volatile
    private var isUiReady: Boolean = false

    private val appSettingsManager: AppSettingsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !isUiReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.viewTreeObserver.addOnPreDrawListener(object :
            ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                isUiReady = true
                window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
        setContent {
            val themeMode by appSettingsManager.themeMode.collectAsStateWithLifecycle()

            MaaMeowTheme(themeMode = themeMode) {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
