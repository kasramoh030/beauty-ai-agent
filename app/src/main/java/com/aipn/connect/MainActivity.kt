package com.aipn.connect

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipn.connect.ui.ApnTheme
import com.aipn.connect.ui.AppViewModel
import com.aipn.connect.ui.ChatDrawer
import com.aipn.connect.ui.ChatScreen
import com.aipn.connect.ui.KeysScreen
import com.aipn.connect.ui.LocaleState
import com.aipn.connect.ui.ModelSheet
import com.aipn.connect.ui.SettingsScreen
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ApnRoot() }
    }
}

private enum class Page { CHAT, KEYS, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApnRoot() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.chatHistory.collectAsStateWithLifecycle()

    var page by remember { mutableStateOf(Page.CHAT) }
    var modelSheetOpen by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val darkSurface = when (settings.theme) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
    }

    LaunchedEffect(settings.theme) {
        AppCompatDelegate.setDefaultNightMode(
            when (settings.theme) {
                ThemeChoice.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeChoice.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeChoice.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    // Dark icons on the light theme, light icons on the dark one.
    val view = LocalView.current
    LaunchedEffect(darkSurface, view) {
        if (!view.isInEditMode) {
            val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkSurface
                isAppearanceLightNavigationBars = !darkSurface
            }
        }
    }
    LaunchedEffect(settings.languageTag) {
        val tag = settings.languageTag
        if (tag.isBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    val configuration = LocalConfiguration.current
    val language = if (configuration.locales.isEmpty) {
        Locale.getDefault().language
    } else {
        configuration.locales[0].language
    }
    LocaleState.isPersian = language.equals("fa", ignoreCase = true)

    val openUrl = { url: String ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // no browser on this device
        }
    }

    ApnTheme(
        darkTheme = when (settings.theme) {
            ThemeChoice.LIGHT -> false
            ThemeChoice.DARK -> true
            ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        }
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = page == Page.CHAT,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                ) {
                    ChatDrawer(
                        history = history,
                        activeId = viewModel.activeSessionId(),
                        onNewChat = viewModel::newChat,
                        onOpenSession = { id -> viewModel.openSession(id); page = Page.CHAT },
                        onDeleteSession = viewModel::deleteSession,
                        onOpenKeys = { page = Page.KEYS },
                        onClose = { scope.launch { drawerState.close() } },
                    )
                }
            },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                when (page) {
                    Page.CHAT -> ChatScreen(
                        viewModel = viewModel,
                        onOpenSettings = { page = Page.SETTINGS },
                        onOpenKeys = { page = Page.KEYS },
                        onOpenModels = { modelSheetOpen = true },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                    )

                    Page.KEYS -> KeysScreen(
                        viewModel = viewModel,
                        onClose = { page = Page.CHAT },
                        openUrl = openUrl,
                    )

                    Page.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onClose = { page = Page.CHAT },
                        openUrl = openUrl,
                    )
                }

                SnackbarHost(snackbar, Modifier.padding(bottom = 90.dp))
            }
        }

        if (modelSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { modelSheetOpen = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                ModelSheet(viewModel) { modelSheetOpen = false }
            }
        }
    }
}
