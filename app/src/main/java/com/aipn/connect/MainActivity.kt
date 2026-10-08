package com.aipn.connect

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipn.connect.ui.ApnTheme
import com.aipn.connect.ui.AppViewModel
import com.aipn.connect.ui.ChatScreen
import com.aipn.connect.ui.KeysScreen
import com.aipn.connect.ui.LocaleState
import com.aipn.connect.ui.SettingsScreen
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ApnRoot() }
    }
}

private enum class Tab(val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CHAT(R.string.nav_chat, Icons.Rounded.AddComment),
    KEYS(R.string.nav_keys, Icons.Rounded.Key),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApnRoot() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.CHAT) }

    // Keep the persisted theme and language applied to the whole activity.
    androidx.compose.runtime.LaunchedEffect(settings.theme) {
        AppCompatDelegate.setDefaultNightMode(
            when (settings.theme) {
                ThemeChoice.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeChoice.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeChoice.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
    androidx.compose.runtime.LaunchedEffect(settings.languageTag) {
        val tag = settings.languageTag
        if (tag.isBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    val locales = androidx.compose.ui.platform.LocalConfiguration.current.locales
    LocaleState.isPersian = locales.isNotEmpty() &&
        (locales[0]?.language ?: Locale.getDefault().language).equals("fa", ignoreCase = true)

    ApnTheme(
        darkTheme = when (settings.theme) {
            ThemeChoice.LIGHT -> false
            ThemeChoice.DARK -> true
            ThemeChoice.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            label = { Text(stringResource(entry.titleRes)) },
                        )
                    }
                }
            },
        ) { padding ->
            when (tab) {
                Tab.CHAT -> ChatScreen(viewModel, onOpenKeys = { tab = Tab.KEYS })
                Tab.KEYS -> KeysScreen(
                    viewModel = viewModel,
                    isPersian = LocaleState.isPersian,
                    openUrl = { url ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: ActivityNotFoundException) {
                            // no browser available - nothing useful to do here
                        }
                    },
                )
                Tab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

/** Small alias so this file does not need the lifecycle-compose import twice. */
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateWithLifecycleCompat() =
    androidx.lifecycle.compose.collectAsStateWithLifecycle(this)