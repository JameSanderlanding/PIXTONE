package com.vova.glyph.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.vova.glyph.data.Track
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun CoverUpdateDialog(
    onFindMissing: () -> Unit,
    onUpdateAll: () -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                MicroLabel("ОБЛОЖКИ", color = GlyphRed)
                Spacer(Modifier.height(14.dp))
                CoverOptionCard(
                    title = "НЕДОСТАЮЩИЕ ОБЛОЖКИ",
                    body = "Найти обложки для треков, где PIXTONE нарисовал точки.",
                    onClick = onFindMissing,
                )
                Spacer(Modifier.height(10.dp))
                CoverOptionCard(
                    title = "ВСЕ ОБЛОЖКИ",
                    body = "Перекачать обложки для всех треков библиотеки.",
                    onClick = onUpdateAll,
                )
                Spacer(Modifier.height(10.dp))
                CoverOptionCard(
                    title = "ОБЛОЖКИ ПО УМОЛЧАНИЮ",
                    body = "Вернуть обложки, встроенные в файлы песен.",
                    onClick = onResetDefaults,
                    buttonText = "ВЕРНУТЬ ПО УМОЛЧАНИЮ",
                    filled = false,
                )
                Spacer(Modifier.height(16.dp))
                GlyphButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    filled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CoverOptionCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    buttonText: String = "ОБНОВИТЬ ОБЛОЖКИ",
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, GlyphLine, shape)
            .padding(12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray, fontSize = 11.sp),
        )
        Spacer(Modifier.height(10.dp))
        GlyphButton(
            text = buttonText,
            onClick = onClick,
            filled = filled,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun FilesDialog(
    folderName: String?,
    scanning: Boolean,
    onChooseFolder: () -> Unit,
    onScan: () -> Unit,
    onAddTrack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(20.dp)) {
                MicroLabel("ФАЙЛЫ", color = GlyphRed)
                Spacer(Modifier.height(14.dp))
                CoverOptionCard(
                    title = "ПАПКА С МУЗЫКОЙ",
                    body = if (folderName.isNullOrBlank()) {
                        "Папка не выбрана. Выберите папку с музыкой на устройстве."
                    } else {
                        "Текущая папка: «$folderName». Можно выбрать другую."
                    },
                    onClick = onChooseFolder,
                    buttonText = "ВЫБРАТЬ ПАПКУ",
                )
                Spacer(Modifier.height(10.dp))
                CoverOptionCard(
                    title = "СКАНИРОВАТЬ ПАПКУ",
                    body = "Найти новые треки в выбранной папке.",
                    onClick = onScan,
                    buttonText = "СКАНИРОВАТЬ",
                    enabled = !scanning && !folderName.isNullOrBlank(),
                )
                Spacer(Modifier.height(10.dp))
                CoverOptionCard(
                    title = "ДОБАВИТЬ ТРЕК",
                    body = "Выберите отдельный аудиофайл — папку выбирать не обязательно.",
                    onClick = onAddTrack,
                    buttonText = "ВЫБРАТЬ ФАЙЛ",
                    filled = false,
                    enabled = !scanning,
                )
                Spacer(Modifier.height(16.dp))
                GlyphButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    filled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    coverUri: Uri?,
    onPickCover: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shape = RoundedCornerShape(2.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .imePadding()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MicroLabel("НОВЫЙ ПЛЕЙЛИСТ", color = GlyphRed)
                    Spacer(Modifier.weight(1f))
                    GlyphIconButton(
                        glyph = Glyph.Close,
                        onClick = onDismiss,
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = GlyphGray,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(shape)
                            .background(GlyphBlack)
                            .border(1.dp, GlyphLine, shape)
                            .clickable(onClick = onPickCover),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (coverUri != null) {
                            AsyncImage(
                                model = coverUri,
                                contentDescription = com.vova.glyph.i18n.tr("Обложка плейлиста"),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            GlyphPlaceholder(seed = name, modifier = Modifier.fillMaxSize())
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .background(GlyphWhite, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            GlyphIcon(Glyph.Plus, tint = GlyphBlack, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        MicroLabel("ОБЛОЖКА", color = GlyphGray)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (coverUri == null) "Выберите .PNG или .JPG" else "Обложка выбрана",
                            style = MaterialTheme.typography.bodySmall.copy(color = GlyphWhite),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Необязательно. Без обложки PIXTONE сам нарисует точечный узор.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GlyphGray,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                GlyphTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(60)
                        error = null
                    },
                    placeholder = "Название плейлиста",
                )
                Spacer(Modifier.height(10.dp))
                GlyphTextField(
                    value = description,
                    onValueChange = { description = it.take(240) },
                    placeholder = "Описание — настроение, повод, пара слов",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3,
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(color = GlyphRed),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "Отмена",
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = "Создать",
                        onClick = {
                            if (name.isBlank()) {
                                error = "Дайте плейлисту название"
                            } else {
                                onCreate(name.trim(), description.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun TrackEditDialog(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String) -> Unit,
) {
    var title by remember(track.id) { mutableStateOf(track.effectiveTitle) }
    var artist by remember(track.id) { mutableStateOf(track.effectiveArtist) }
    val shape = RoundedCornerShape(2.dp)
    val tagTitle = track.title.trim()
    val tagArtist = track.artist.trim()
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .imePadding()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(
                Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MicroLabel("НАЗВАНИЕ И ИСПОЛНИТЕЛЬ", color = GlyphRed)
                    Spacer(Modifier.weight(1f))
                    GlyphIconButton(
                        glyph = Glyph.Close,
                        onClick = onDismiss,
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = GlyphGray,
                    )
                }
                Spacer(Modifier.height(16.dp))
                TrackFieldLabel(
                    text = "НАЗВАНИЕ",
                    canReset = track.hasCustomTitle,
                    onReset = { title = tagTitle },
                )
                Spacer(Modifier.height(7.dp))
                GlyphTextField(
                    value = title,
                    onValueChange = { title = it.take(160) },
                    placeholder = "Название трека",
                )
                if (track.hasCustomTitle) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "В теге: $tagTitle",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GlyphDim,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(16.dp))
                TrackFieldLabel(
                    text = "ИСПОЛНИТЕЛЬ",
                    canReset = track.hasCustomArtist,
                    onReset = { artist = tagArtist },
                )
                Spacer(Modifier.height(7.dp))
                GlyphTextField(
                    value = artist,
                    onValueChange = { artist = it.take(120) },
                    placeholder = "Имя исполнителя",
                )
                if (track.hasCustomArtist) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "В теге: $tagArtist",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GlyphDim,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Название и исполнитель хранятся в PIXTONE. Файл на устройстве не меняется.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GlyphGray,
                        fontSize = 11.sp,
                    ),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "ОТМЕНА",
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = "СОХРАНИТЬ",
                        onClick = { onSave(title, artist) },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun MissingTrackDialog(
    trackTitle: String,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(20.dp)) {
                MicroLabel("ТРЕК НЕДОСТУПЕН", color = GlyphRed)
                Spacer(Modifier.height(14.dp))
                Text(
                    trackTitle,
                    style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Трек удалён или переименован. Запустите сканирование заново.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = GlyphGray,
                        fontSize = 11.sp,
                    ),
                )
                Spacer(Modifier.height(18.dp))
                GlyphButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    filled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun SaveSoundPresetDialog(
    onSave: (name: String) -> Unit,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(2.dp)

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MicroLabel("НОВЫЙ ПРЕСЕТ", color = GlyphRed)
                    Spacer(Modifier.weight(1f))
                    GlyphIconButton(
                        glyph = Glyph.Close,
                        onClick = onClose,
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = GlyphGray,
                    )
                }
                Spacer(Modifier.height(16.dp))
                GlyphTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(60)
                        error = false
                    },
                    placeholder = "Название пресета",
                )
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Введите название пресета",
                        style = MaterialTheme.typography.bodySmall.copy(color = GlyphRed),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "ЗАКРЫТЬ",
                        onClick = onClose,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = "СОХРАНИТЬ",
                        onClick = {
                            if (name.isBlank()) {
                                error = true
                            } else {
                                onSave(name.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackFieldLabel(
    text: String,
    canReset: Boolean,
    onReset: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MicroLabel(text, color = GlyphGray)
        Spacer(Modifier.weight(1f))
        if (canReset) {
            Text(
                "СБРОСИТЬ",
                modifier = Modifier.clickable(onClick = onReset),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GlyphRed,
                    fontSize = 8.sp,
                    letterSpacing = 0.8.sp,
                ),
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val shape = RoundedCornerShape(2.dp)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(20.dp)) {
                MicroLabel(if (destructive) "ПОДТВЕРДИТЕ ДЕЙСТВИЕ" else "ПОДТВЕРЖДЕНИЕ", color = GlyphRed)
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(color = GlyphWhite),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "Отмена",
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = confirmText,
                        onClick = onConfirm,
                        filled = !destructive,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
