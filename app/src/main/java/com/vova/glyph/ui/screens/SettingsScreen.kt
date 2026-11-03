package com.vova.glyph.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.i18n.AppLanguage
import com.vova.glyph.i18n.LangController
import com.vova.glyph.i18n.tr
import com.vova.glyph.ui.components.ColorPickerDialog
import com.vova.glyph.ui.components.ConfirmDialog
import com.vova.glyph.ui.components.CoverUpdateDialog
import com.vova.glyph.ui.components.DotGrid
import com.vova.glyph.ui.components.DotSpinner
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.GlyphIconButton
import com.vova.glyph.ui.components.LicenseTextDialog
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.theme.AppFont
import com.vova.glyph.ui.theme.ColorSlot
import com.vova.glyph.ui.theme.FontController
import com.vova.glyph.ui.theme.FontRole
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite
import com.vova.glyph.ui.theme.ThemeController
import com.vova.glyph.ui.theme.ThemePreset

private enum class SettingsPage { MAIN, THEME, THEME_EDITOR, FONTS, LANGUAGE, LICENSES }

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var page by remember { mutableStateOf(SettingsPage.MAIN) }

    BackHandler(enabled = page != SettingsPage.MAIN) {
        page = when (page) {
            SettingsPage.THEME_EDITOR -> {
                ThemeController.cancelDraft()
                SettingsPage.THEME
            }
            else -> SettingsPage.MAIN
        }
    }

    Box(Modifier.fillMaxSize().background(GlyphBlack)) {
        DotGrid(Modifier.fillMaxSize(), color = GlyphWhite.copy(alpha = 0.035f), step = 24.dp)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 30.dp)
        ) {
            when (page) {
                SettingsPage.MAIN -> SettingsMain(
                    vm = vm,
                    onTheme = { page = SettingsPage.THEME },
                    onFonts = { page = SettingsPage.FONTS },
                    onLanguage = { page = SettingsPage.LANGUAGE },
                    onLicenses = { page = SettingsPage.LICENSES },
                )
                SettingsPage.THEME -> ThemeSettingsPage(
                    onBack = { page = SettingsPage.MAIN },
                    onCreatePreset = {
                        ThemeController.startDraft()
                        page = SettingsPage.THEME_EDITOR
                    },
                    onEditPreset = { page = SettingsPage.THEME_EDITOR },
                )
                SettingsPage.THEME_EDITOR -> ThemeEditorPage(
                    onCancel = {
                        ThemeController.cancelDraft()
                        page = SettingsPage.THEME
                    },
                    onSaved = { page = SettingsPage.THEME },
                )
                SettingsPage.FONTS -> FontsSettingsPage(onBack = { page = SettingsPage.MAIN })
                SettingsPage.LANGUAGE -> LanguageSettingsPage(onBack = { page = SettingsPage.MAIN })
                SettingsPage.LICENSES -> LicensesSettingsPage(onBack = { page = SettingsPage.MAIN })
            }
        }
    }
}

@Composable
private fun SettingsMain(
    vm: MainViewModel,
    onTheme: () -> Unit,
    onFonts: () -> Unit,
    onLanguage: () -> Unit,
    onLicenses: () -> Unit,
) {
    val folderName by vm.folderName.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanStatus by vm.scanStatus.collectAsStateWithLifecycle()
    val scanFound by vm.scanFound.collectAsStateWithLifecycle()
    val artworkStatus by vm.artworkStatus.collectAsStateWithLifecycle()
    val coverPaused by vm.coverPaused.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val favoritesList by vm.favorites.collectAsStateWithLifecycle()
    var showCoverDialog by remember { mutableStateOf(false) }
    var showResetCoversConfirm by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.onFolderPicked(uri, fromOnboarding = false)
    }

    Row(
        Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIconButton(
            glyph = Glyph.Back,
            onClick = vm::closeSettings,
            size = 36.dp,
            iconSize = 19.dp,
        )
        Spacer(Modifier.width(4.dp))
        MicroLabel("PIXTONE / НАСТРОЙКИ", color = GlyphDim)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(5.dp).background(GlyphRed, CircleShape))
        Spacer(Modifier.width(7.dp))
        MicroLabel("03", color = GlyphDim)
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "НАСТРОЙКИ",
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 40.sp,
            lineHeight = 43.sp,
        ),
        color = GlyphWhite,
    )
    Spacer(Modifier.height(7.dp))
    Text(
        "Папка с музыкой и данные приложения. Всё хранится только на этом устройстве.",
        style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
    )
    Spacer(Modifier.height(22.dp))

    SettingsSection(number = "01", title = "ЛОКАЛЬНАЯ МУЗЫКА") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).border(1.dp, GlyphLine, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(Glyph.Folder, tint = GlyphGray, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                MicroLabel("ТЕКУЩАЯ ПАПКА", color = GlyphDim, fontSize = 8)
                Spacer(Modifier.height(3.dp))
                Text(
                    folderName?.takeIf { it.isNotBlank() } ?: "Папка не выбрана",
                    style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (folderName.isNullOrBlank()) {
                Box(Modifier.size(6.dp).background(GlyphDim, CircleShape))
            } else {
                Box(Modifier.size(6.dp).background(GlyphRed, CircleShape))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "PIXTONE читает MP3, M4A, FLAC, WAV, OGG и OPUS вместе с тегами и встроенными обложками. Обложки, вставленные сайтами-складчиками, заменяются настоящими (Genius, Deezer, iTunes) при добавлении новых треков — всю библиотеку можно обновить кнопкой ниже. Доступ к папке сохраняется между запусками.",
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim, fontSize = 11.sp),
        )
        if (scanning || artworkStatus != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(GlyphBlack, RoundedCornerShape(2.dp))
                    .border(1.dp, GlyphLine, RoundedCornerShape(2.dp))
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DotSpinner(Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (scanning) "ЧИТАЮ ПАПКУ" else "ОБЛОЖКИ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GlyphWhite,
                            fontSize = 8.sp,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (scanning) scanStatus.ifBlank { "ПОДГОТОВКА…" }
                        else artworkStatus.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = GlyphGray,
                            fontSize = 11.sp,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (scanning) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        scanFound.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = GlyphRed,
                            fontSize = 20.sp,
                        ),
                    )
                } else {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clickable {
                                if (coverPaused) vm.resumeCoverRefresh() else vm.pauseCoverRefresh()
                            }
                            .padding(4.dp),
                    ) {
                        MicroLabel(
                            if (coverPaused) "ПРОДОЛЖИТЬ" else "ПРИОСТАНОВИТЬ",
                            color = GlyphRed,
                            fontSize = 8,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            GlyphButton(
                text = if (folderName.isNullOrBlank()) "ВЫБРАТЬ ПАПКУ" else "СМЕНИТЬ",
                onClick = { folderPicker.launch(null) },
                enabled = !scanning,
                leading = Glyph.Folder,
                modifier = Modifier.weight(1f),
            )
            GlyphButton(
                text = "ОБЛОЖКИ",
                onClick = { showCoverDialog = true },
                filled = false,
                enabled = !scanning && artworkStatus == null && tracks.isNotEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(9.dp))
        GlyphButton(
            text = "СКАНИРОВАТЬ",
            onClick = vm::rescanFolder,
            filled = false,
            enabled = !scanning && !folderName.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(11.dp))
        Row {
            MicroLabel("В БИБЛИОТЕКЕ: ${tracks.size}", color = GlyphDim, fontSize = 8)
            Spacer(Modifier.weight(1f))
            MicroLabel("ИЗБРАННОЕ: ${favoritesList.size}", color = GlyphDim, fontSize = 8)
        }
    }

    Spacer(Modifier.height(14.dp))

    SettingsSection(number = "02", title = "ОФОРМЛЕНИЕ") {
        SettingsLinkRow(
            label = "ТЕМА",
            value = ThemeController.presetName,
            onClick = onTheme,
        )
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlyphLine))
        Spacer(Modifier.height(9.dp))
        SettingsLinkRow(
            label = "ШРИФТЫ",
            value = if (FontController.isDefault) "СТАНДАРТ" else "СВОИ",
            onClick = onFonts,
        )
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlyphLine))
        Spacer(Modifier.height(9.dp))
        SettingsLinkRow(
            label = "ЯЗЫК",
            value = LangController.language.nativeName,
            onClick = onLanguage,
        )
    }

    Spacer(Modifier.height(14.dp))

    SettingsSection(number = "03", title = "О ПРИЛОЖЕНИИ") {
        InfoRow("ВЕРСИЯ", "1.0.0")
        Spacer(Modifier.height(9.dp))
        InfoRow("СБОРКА", "PIXTONE / LOCAL-FIRST")
        Spacer(Modifier.height(9.dp))
        InfoRow("ДАННЫЕ", "ЛОКАЛЬНО · БЕЗ ОБЛАКА")
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlyphLine))
        Spacer(Modifier.height(9.dp))
        SettingsLinkRow(
            label = "ЛИЦЕНЗИИ",
            value = "OPEN SOURCE",
            onClick = onLicenses,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Сочетания точек, общая сегментация и цветные акценты привносят свежий взгляд на привычное и ставшее классикой единение чёрного и белого цветов в вопросе дизайна приложения. Шрифты Doto, Pixelify Sans и Space Grotesk распространяются по SIL Open Font License.",
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim, fontSize = 11.sp),
        )
    }

    Spacer(Modifier.height(26.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(4.dp).background(GlyphRed, CircleShape))
        Spacer(Modifier.width(7.dp))
        MicroLabel("PIXTONE · ЛИЧНЫЙ ПЛЕЕР", color = GlyphDim, fontSize = 8)
        Spacer(Modifier.weight(1f))
        MicroLabel("СДЕЛАНО ДЛЯ МУЗЫКИ", color = GlyphDim, fontSize = 8)
    }

    if (showCoverDialog) {
        CoverUpdateDialog(
            onFindMissing = {
                showCoverDialog = false
                vm.findMissingCovers()
            },
            onUpdateAll = {
                showCoverDialog = false
                vm.refreshAllCovers()
            },
            onResetDefaults = {
                showCoverDialog = false
                showResetCoversConfirm = true
            },
            onDismiss = { showCoverDialog = false },
        )
    }

    if (showResetCoversConfirm) {
        ConfirmDialog(
            title = "Восстановить исходные обложки?",
            message = "Скачанные обложки будут заменены обложками из файлов песен.",
            confirmText = "Восстановить",
            destructive = true,
            onConfirm = {
                showResetCoversConfirm = false
                vm.resetCovers()
            },
            onDismiss = { showResetCoversConfirm = false },
        )
    }
}

@Composable
private fun ThemeSettingsPage(
    onBack: () -> Unit,
    onCreatePreset: () -> Unit,
    onEditPreset: () -> Unit,
) {
    val presets = ThemeController.allPresets()

    SettingsPageHeader(title = "ТЕМА", onBack = onBack)
    Spacer(Modifier.height(7.dp))
    Text(
        "Цвета интерфейса применяются сразу.",
        style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
    )
    Spacer(Modifier.height(22.dp))

    SettingsSection(number = "01", title = "ПРЕСЕТЫ") {
        presets.forEachIndexed { index, preset ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            PresetRow(
                preset = preset,
                selected = ThemeController.presetId == preset.id,
                onClick = { ThemeController.applyPreset(preset) },
                onEdit = if (ThemeController.customPresets.any { it.id == preset.id }) {
                    {
                        ThemeController.startEdit(preset)
                        onEditPreset()
                    }
                } else {
                    null
                },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    GlyphButton(
        text = "СОЗДАТЬ ПРЕСЕТ",
        onClick = onCreatePreset,
        filled = false,
        leading = Glyph.Plus,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ThemeEditorPage(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    var editing by remember { mutableStateOf<ColorSlot?>(null) }
    val palette = ThemeController.palette
    val editingPreset = if (ThemeController.isEditingDraft) {
        ThemeController.customPresets.firstOrNull { it.id == ThemeController.presetId }
    } else {
        null
    }
    var name by remember(editingPreset?.id) {
        mutableStateOf(
            editingPreset?.name
                ?: (tr("МОЙ ПРЕСЕТ") + " " + (ThemeController.customPresets.size + 1)),
        )
    }

    SettingsPageHeader(
        title = if (editingPreset != null) "РЕДАКТИРОВАТЬ ПРЕСЕТ" else "НОВЫЙ ПРЕСЕТ",
        onBack = onCancel,
    )
    Spacer(Modifier.height(7.dp))
    Text(
        "Настройте цвета и сохраните пресет.",
        style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
    )
    Spacer(Modifier.height(22.dp))

    SettingsSection(number = "01", title = "НАЗВАНИЕ ПРЕСЕТА") {
        PresetNameField(
            value = name,
            onValueChange = { name = it },
        )
    }

    Spacer(Modifier.height(14.dp))

    SettingsSection(number = "02", title = "ПАЛИТРА") {
        ColorSlot.entries.forEachIndexed { index, slot ->
            if (index > 0) Spacer(Modifier.height(9.dp))
            ColorSlotRow(
                slot = slot,
                color = palette.colorOf(slot),
                onClick = { editing = slot },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlyphButton(
            text = "СОХРАНИТЬ",
            onClick = {
                ThemeController.saveDraft(name)
                onSaved()
            },
            modifier = Modifier.weight(1f),
        )
        GlyphButton(
            text = "Отмена",
            onClick = onCancel,
            filled = false,
            modifier = Modifier.weight(1f),
        )
    }

    editing?.let { slot ->
        ColorPickerDialog(
            title = slot.label,
            initial = palette.colorOf(slot),
            onDismiss = { editing = null },
            onApply = { color ->
                ThemeController.setDraftColor(slot, color)
                editing = null
            },
        )
    }
}

@Composable
private fun PresetNameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(GlyphBlack.copy(alpha = 0.55f), shape)
            .border(1.dp, GlyphLine, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(
                "НАЗВАНИЕ ПРЕСЕТА",
                style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 28) onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(color = GlyphWhite, fontSize = 14.sp),
            cursorBrush = SolidColor(GlyphRed),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FontsSettingsPage(onBack: () -> Unit) {
    SettingsPageHeader(title = "ШРИФТЫ", onBack = onBack)
    Spacer(Modifier.height(7.dp))
    Text(
        "Выберите шрифты интерфейса.",
        style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
    )
    Spacer(Modifier.height(22.dp))

    FontRole.entries.forEachIndexed { roleIndex, role ->
        if (roleIndex > 0) Spacer(Modifier.height(14.dp))
        SettingsSection(number = "0${roleIndex + 1}", title = role.label) {
            AppFont.entries.forEachIndexed { fontIndex, font ->
                if (fontIndex > 0) Spacer(Modifier.height(9.dp))
                FontRow(
                    font = font,
                    selected = FontController.fontFor(role) == font,
                    onClick = { FontController.select(role, font) },
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    GlyphButton(
        text = "СБРОСИТЬ К СТАНДАРТНЫМ",
        onClick = { FontController.reset() },
        filled = false,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FontRow(
    font: AppFont,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .border(1.dp, GlyphLine, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Aa",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = font.family,
                    color = GlyphWhite,
                    fontSize = 15.sp,
                ),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            font.title,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (selected) GlyphWhite else GlyphGray,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
            ),
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(if (selected) 7.dp else 5.dp)
                .background(if (selected) GlyphRed else GlyphDim, CircleShape)
        )
    }
}

private data class LicenseEntry(
    val name: String,
    val license: String,
    val asset: String,
)

private val FontLicenses = listOf(
    LicenseEntry("DOTO", "SIL OFL 1.1", "licenses/OFL-Doto.txt"),
    LicenseEntry("PIXELIFY SANS", "SIL OFL 1.1", "licenses/OFL-PixelifySans.txt"),
    LicenseEntry("SPACE GROTESK", "SIL OFL 1.1", "licenses/OFL-SpaceGrotesk.txt"),
)

private val LibraryLicenses = listOf(
    LicenseEntry("ANDROIDX · JETPACK COMPOSE", "APACHE 2.0", "licenses/Apache-2.0.txt"),
    LicenseEntry("MEDIA3 · EXOPLAYER", "APACHE 2.0", "licenses/Apache-2.0.txt"),
    LicenseEntry("ANDROIDX DOCUMENTFILE", "APACHE 2.0", "licenses/Apache-2.0.txt"),
    LicenseEntry("COIL", "APACHE 2.0", "licenses/Apache-2.0.txt"),
    LicenseEntry("OKHTTP", "APACHE 2.0", "licenses/Apache-2.0.txt"),
    LicenseEntry("KOTLIN COROUTINES", "APACHE 2.0", "licenses/Apache-2.0.txt"),
    LicenseEntry("JSOUP", "MIT", "licenses/MIT.txt"),
)

@Composable
private fun LicensesSettingsPage(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<LicenseEntry?>(null) }

    SettingsPageHeader(title = "ЛИЦЕНЗИИ", onBack = onBack)
    Spacer(Modifier.height(7.dp))
    Text(
        "Открытые лицензии и уведомления правообладателей. Нажмите на строку, чтобы прочитать текст.",
        style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
    )
    Spacer(Modifier.height(22.dp))

    SettingsSection(number = "01", title = "ШРИФТЫ") {
        FontLicenses.forEachIndexed { index, entry ->
            if (index > 0) {
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GlyphLine))
                Spacer(Modifier.height(9.dp))
            }
            LicenseRow(entry) { selected = entry }
        }
    }

    Spacer(Modifier.height(14.dp))

    SettingsSection(number = "02", title = "БИБЛИОТЕКИ") {
        LibraryLicenses.forEachIndexed { index, entry ->
            if (index > 0) {
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GlyphLine))
                Spacer(Modifier.height(9.dp))
            }
            LicenseRow(entry) { selected = entry }
        }
    }

    Spacer(Modifier.height(14.dp))

    SettingsSection(number = "03", title = "УВЕДОМЛЕНИЯ") {
        val notices = LicenseEntry(
            name = "THIRD-PARTY NOTICES",
            license = "TXT",
            asset = "licenses/THIRD_PARTY_NOTICES.txt",
        )
        LicenseRow(notices) { selected = notices }
    }

    selected?.let { entry ->
        LicenseTextDialog(
            title = entry.name,
            assetPath = entry.asset,
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun LicenseRow(entry: LicenseEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            entry.name,
            style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
            modifier = Modifier.weight(1f),
        )
        MicroLabel(entry.license, color = GlyphDim, fontSize = 8)
        Spacer(Modifier.width(6.dp))
        Text(
            "›",
            style = MaterialTheme.typography.titleMedium.copy(color = GlyphRed),
        )
    }
}

@Composable
private fun LanguageSettingsPage(onBack: () -> Unit) {
    SettingsPageHeader(title = "ЯЗЫК", onBack = onBack)
    Spacer(Modifier.height(7.dp))
    Text(
        "Выберите язык интерфейса.",
        style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
    )
    Spacer(Modifier.height(22.dp))

    SettingsSection(number = "01", title = "ЯЗЫК") {
        AppLanguage.entries.forEachIndexed { index, language ->
            if (index > 0) Spacer(Modifier.height(9.dp))
            LanguageRow(
                language = language,
                selected = LangController.language == language,
                onClick = { LangController.select(language) },
            )
        }
    }
}

@Composable
private fun SettingsPageHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIconButton(
            glyph = Glyph.Back,
            onClick = onBack,
            size = 36.dp,
            iconSize = 19.dp,
        )
        Spacer(Modifier.width(4.dp))
        MicroLabel("PIXTONE / НАСТРОЙКИ", color = GlyphDim)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(5.dp).background(GlyphRed, CircleShape))
    }
    Spacer(Modifier.height(16.dp))
    Text(
        title,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 40.sp,
            lineHeight = 43.sp,
        ),
        color = GlyphWhite,
    )
}

@Composable
private fun PresetRow(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(2.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(GlyphBlack.copy(alpha = 0.55f), shape)
            .border(1.dp, if (selected) GlyphRed else GlyphLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PresetSwatch(preset.palette.background)
        Spacer(Modifier.width(4.dp))
        PresetSwatch(preset.palette.text)
        Spacer(Modifier.width(4.dp))
        PresetSwatch(preset.palette.accent)
        Spacer(Modifier.width(10.dp))
        Text(
            preset.name,
            style = MaterialTheme.typography.labelLarge.copy(color = GlyphWhite, fontSize = 10.sp),
            modifier = Modifier.weight(1f),
        )
        if (onEdit != null) {
            Box(
                Modifier
                    .size(28.dp)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(
                    glyph = Glyph.Pencil,
                    tint = GlyphGray,
                    modifier = Modifier.size(14.dp),
                    stroke = 1.5.dp,
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(
            Modifier
                .size(7.dp)
                .background(if (selected) GlyphRed else GlyphDim, CircleShape)
        )
    }
}

@Composable
private fun PresetSwatch(color: Color) {
    Box(
        Modifier
            .size(18.dp)
            .background(color, RoundedCornerShape(2.dp))
            .border(1.dp, GlyphLine, RoundedCornerShape(2.dp))
    )
}

@Composable
private fun ColorSlotRow(
    slot: ColorSlot,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(color, RoundedCornerShape(2.dp))
                .border(1.dp, GlyphLine, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            slot.label,
            style = MaterialTheme.typography.titleSmall.copy(color = GlyphWhite),
            modifier = Modifier.weight(1f),
        )
        Text(
            hexOf(color),
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray, fontSize = 11.sp),
        )
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            language.nativeName,
            style = MaterialTheme.typography.titleSmall.copy(
                color = if (selected) GlyphWhite else GlyphGray,
            ),
            modifier = Modifier.weight(1f),
        )
        MicroLabel(language.code.uppercase(), color = GlyphDim, fontSize = 8)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(if (selected) 7.dp else 5.dp)
                .background(if (selected) GlyphRed else GlyphDim, CircleShape)
        )
    }
}

@Composable
private fun SettingsLinkRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MicroLabel(label, color = GlyphWhite, fontSize = 9)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray, fontSize = 12.sp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "›",
            style = MaterialTheme.typography.titleMedium.copy(color = GlyphRed),
        )
    }
}

private fun hexOf(color: Color): String {
    val argb = color.toArgb()
    return "#%06X".format(argb and 0xFFFFFF)
}

@Composable
private fun SettingsSection(
    number: String,
    title: String,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(GlyphSurface.copy(alpha = 0.94f), shape)
            .border(1.dp, GlyphLine, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                number,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = GlyphRed,
                    fontSize = 11.sp,
                ),
            )
            Spacer(Modifier.width(9.dp))
            Box(Modifier.width(1.dp).height(11.dp).background(GlyphLine))
            Spacer(Modifier.width(9.dp))
            MicroLabel(title, color = GlyphGray, fontSize = 9)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MicroLabel(label, color = GlyphDim, fontSize = 8)
        Spacer(Modifier.weight(1f))
        MicroLabel(value, color = GlyphGray, fontSize = 8)
    }
}
