package com.aliothmoon.maameow.presentation.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.presentation.components.ResourceLoadingOverlay
import com.aliothmoon.maameow.presentation.view.background.BackgroundTaskView
import com.aliothmoon.maameow.presentation.view.home.HomeView
import com.aliothmoon.maameow.presentation.view.settings.ErrorLogView
import com.aliothmoon.maameow.presentation.view.settings.LogHistoryView
import com.aliothmoon.maameow.presentation.view.settings.SettingsView
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    appSettings: AppSettingsManager = koinInject(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current

    var isFullscreen by remember { mutableStateOf(false) }

    // Tab 切换状态 — 独立于 NavHost，避免 AnimatedContent 导致的重叠
    var selectedTab by rememberSaveable { mutableStateOf(Routes.HOME) }

    // 执行模式状态 - 用于底部导航拦截
    val runMode by appSettings.runMode.collectAsStateWithLifecycle()

    // 是否处于 push 页面（设置、日志等）
    val isOnPushPage = currentNavRoute != null && currentNavRoute != Routes.HOME

    // 判断是否显示底部导航
    val showBottomBar = !isFullscreen && !isOnPushPage

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    AppBottomNavigation(
                        currentRoute = selectedTab,
                        onTabSelected = { tab ->
                            // 前台模式下，禁止切换到后台任务
                            if (tab.route == Routes.BACKGROUND_TASK && runMode == RunMode.FOREGROUND) {
                                Toast.makeText(
                                    context,
                                    "当前是前台模式，请先切换到后台模式",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@AppBottomNavigation
                            }
                            selectedTab = tab.route
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // NavHost 仅处理 HOME 页面 + push 导航（设置、日志等）
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                ) {
                    composable(
                        route = Routes.HOME,
                        enterTransition = {
                            when (initialState.destination.route) {
                                Routes.SETTINGS -> slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(350)
                                )

                                else -> null
                            }
                        },
                        exitTransition = {
                            when (targetState.destination.route) {
                                Routes.SETTINGS -> slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = tween(350)
                                )

                                else -> null
                            }
                        },
                        popEnterTransition = {
                            when (initialState.destination.route) {
                                Routes.SETTINGS -> slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = tween(350)
                                )

                                else -> null
                            }
                        },
                        popExitTransition = {
                            when (targetState.destination.route) {
                                Routes.SETTINGS -> slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(350)
                                )

                                else -> null
                            }
                        }
                    ) {
                        HomeView(navController = navController)
                    }

                    composable(
                        route = Routes.SETTINGS,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        },
                        exitTransition = {
                            when (targetState.destination.route) {
                                Routes.HOME -> slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(350)
                                )

                                Routes.LOG_HISTORY -> slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = tween(350)
                                )

                                Routes.ERROR_LOG -> slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = tween(350)
                                )

                                else -> null
                            }
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(350)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        }
                    ) {
                        SettingsView(navController = navController)
                    }

                    composable(
                        route = Routes.LOG_HISTORY,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        }
                    ) {
                        LogHistoryView(navController = navController)
                    }

                    composable(
                        route = Routes.ERROR_LOG,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350)
                            )
                        }
                    ) {
                        ErrorLogView(navController = navController)
                    }
                }

                // 后台任务页 — 独立于 NavHost，直接覆盖在上层，
                // 避免 AnimatedContent 导致的 SurfaceView 闪影和重叠问题
                if (selectedTab == Routes.BACKGROUND_TASK && !isOnPushPage) {
                    BackHandler { selectedTab = Routes.HOME }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        BackgroundTaskView(onFullscreenChanged = { isFullscreen = it })
                    }
                }
            }
        }

        ResourceLoadingOverlay()
    }
}
