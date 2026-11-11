package com.vova.glyph.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.Tab
import com.vova.glyph.data.Track
import com.vova.glyph.ui.components.GlyphBottomNav
import com.vova.glyph.ui.components.GlyphMessageHost
import com.vova.glyph.ui.components.MiniPlayer
import com.vova.glyph.ui.components.TrackEditDialog
import com.vova.glyph.ui.screens.AlbumDetailScreen
import com.vova.glyph.ui.screens.AlbumsScreen
import com.vova.glyph.ui.screens.FavoritesScreen
import com.vova.glyph.ui.screens.LibraryScreen
import com.vova.glyph.ui.screens.OnlineScreen
import com.vova.glyph.ui.screens.OnboardingScreen
import com.vova.glyph.ui.screens.PlayerOverlay
import com.vova.glyph.ui.screens.PlaylistDetailScreen
import com.vova.glyph.ui.screens.SettingsScreen
import com.vova.glyph.ui.theme.CoverTint
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.ThemeController

@Composable
fun GlyphRoot(vm: MainViewModel) {
    val onboardingVisible by vm.onboardingVisible.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val showPlayer by vm.showPlayer.collectAsStateWithLifecycle()
    val showSettings by vm.showSettings.collectAsStateWithLifecycle()
    val openPlaylistId by vm.openPlaylistId.collectAsStateWithLifecycle()
    val openAlbumKey by vm.openAlbumKey.collectAsStateWithLifecycle()
    val trackEditorTrack by vm.trackEditorTrack.collectAsStateWithLifecycle()
    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val playerState by vm.playerState.collectAsStateWithLifecycle()

    val artwork = currentTrack?.artwork
    LaunchedEffect(artwork) {
        ThemeController.updateCoverPalette(CoverTint.paletteFor(artwork))
    }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            runCatching {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val canGoBack = showPlayer || showSettings || openPlaylistId != null ||
        openAlbumKey != null || tab != Tab.LIBRARY

    BackHandler(enabled = canGoBack) {
        vm.handleBack()
    }

    Box(Modifier.fillMaxSize().background(GlyphBlack)) {
        if (onboardingVisible) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                OnboardingScreen(vm)
            }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.LIBRARY -> {
                            if (openPlaylistId != null) {
                                PlaylistDetailScreen(vm)
                            } else {
                                LibraryScreen(vm)
                            }
                        }

                        Tab.ALBUMS -> {
                            if (openAlbumKey != null) {
                                AlbumDetailScreen(vm)
                            } else {
                                AlbumsScreen(vm)
                            }
                        }

                        Tab.FAVORITES -> FavoritesScreen(vm)

                        Tab.ONLINE -> OnlineScreen(vm)
                    }
                }
                currentTrack?.let { track ->
                    MiniPlayerBar(
                        vm = vm,
                        track = track,
                        isPlaying = playerState.isPlaying,
                    )
                }
                Box(Modifier.navigationBarsPadding()) {
                    GlyphBottomNav(
                        selected = tab,
                        onSelect = vm::selectTab,
                    )
                }
            }

            if (showPlayer) {
                PlayerOverlay(vm)
            }

            if (showSettings) {
                SettingsScreen(vm)
            }

            trackEditorTrack?.let { track ->
                TrackEditDialog(
                    track = track,
                    onDismiss = vm::closeTrackEditor,
                    onSave = { title, artist -> vm.saveTrackInfo(track, title, artist) },
                )
            }
        }

        GlyphMessageHost(
            messages = vm.messages,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = if (!onboardingVisible && !showPlayer && !showSettings && currentTrack != null) {
                        72.dp
                    } else {
                        12.dp
                    },
                ),
        )
    }
}

@Composable
private fun MiniPlayerBar(
    vm: MainViewModel,
    track: Track,
    isPlaying: Boolean,
) {
    val progress by vm.progress.collectAsStateWithLifecycle()

    MiniPlayer(
        track = track,
        isPlaying = isPlaying,
        positionMs = progress.positionMs,
        durationMs = progress.durationMs,
        onOpen = vm::openPlayer,
        onToggle = vm::togglePlayPause,
        onNext = vm::next,
    )
}
