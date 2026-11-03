package com.vova.glyph.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.vova.glyph.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.ui.components.BrandMark
import com.vova.glyph.ui.components.DotGrid
import com.vova.glyph.ui.components.DotSpinner
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.theme.DotoFamily
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanStatus by vm.scanStatus.collectAsStateWithLifecycle()
    val scanFound by vm.scanFound.collectAsStateWithLifecycle()
    val error by vm.onboardingError.collectAsStateWithLifecycle()
    val folderName by vm.folderName.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.onFolderPicked(uri, fromOnboarding = true)
    }

    Box(Modifier.fillMaxSize().background(GlyphBlack)) {
        DotGrid(Modifier.fillMaxSize(), color = GlyphWhite.copy(alpha = 0.04f), step = 24.dp)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 16.dp, bottom = 26.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(GlyphRed, CircleShape))
                Spacer(Modifier.width(8.dp))
                BrandMark(fontSize = 13)
                Spacer(Modifier.weight(1f))
                MicroLabel("ПЕРВЫЙ ЗАПУСК / 00", color = GlyphDim)
            }
            Spacer(Modifier.height(34.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphRadar(Modifier.size(118.dp))
                Spacer(Modifier.width(22.dp))
                Column {
                    MicroLabel("ЗНАКОМСТВО", color = GlyphRed)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "ГДЕ ТВОЯ\nМУЗЫКА?",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 37.sp,
                            lineHeight = 39.sp,
                        ),
                        color = GlyphWhite,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "PIXTONE — плеер, который вы запустите снова. Он умеет читать вашу папку с MP3 и хранить музыку только на этом устройстве.",
                style = MaterialTheme.typography.bodyMedium.copy(color = GlyphGray),
            )
            Spacer(Modifier.height(24.dp))

            if (scanning) {
                ScanCard(
                    status = scanStatus,
                    found = scanFound,
                )
            } else {
                ChoiceCard(
                    number = "01",
                    title = "У МЕНЯ ЕСТЬ ПАПКА С МУЗЫКОЙ",
                    body = if (folderName.isNullOrBlank()) {
                        "Укажите папку на устройстве — PIXTONE прочитает MP3, M4A, FLAC, WAV и OGG вместе с обложками и тегами."
                    } else {
                        "Текущая папка: «$folderName». Можно выбрать другую."
                    },
                    buttonText = if (folderName.isNullOrBlank()) "ВЫБРАТЬ ПАПКУ" else "СМЕНИТЬ ПАПКУ",
                    glyph = Glyph.Folder,
                    onClick = { folderPicker.launch(null) },
                )
                Spacer(Modifier.height(12.dp))
                ChoiceCard(
                    number = "02",
                    title = "ГОТОВОЙ ПАПКИ НЕТ",
                    body = "Пропустите шаг и выберите папку позже в библиотеке или настройках.",
                    buttonText = "ПРОПУСТИТЬ",
                    glyph = Glyph.Search,
                    outlined = true,
                    onClick = { vm.skipOnboarding() },
                )
            }

            if (!error.isNullOrBlank() && !scanning) {
                Spacer(Modifier.height(14.dp))
                Text(
                    error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(color = GlyphRed),
                )
            }

            Spacer(Modifier.height(26.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MicroLabel("MP3 · M4A · FLAC · WAV · OGG", color = GlyphDim, fontSize = 8)
                Spacer(Modifier.weight(1f))
                MicroLabel("ЛОКАЛЬНО · БЕЗ ОБЛАКА", color = GlyphDim, fontSize = 8)
            }
        }
    }
}

@Composable
private fun GlyphRadar(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "radar-angle",
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f - 3.dp.toPx()
        drawCircle(GlyphLine, outer, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawCircle(GlyphLine.copy(alpha = 0.7f), outer * 0.66f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawCircle(GlyphWhite.copy(alpha = 0.35f), outer * 0.3f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawLine(GlyphLine.copy(alpha = 0.6f), Offset(center.x - outer, center.y), Offset(center.x + outer, center.y), 1.dp.toPx())
        drawLine(GlyphLine.copy(alpha = 0.6f), Offset(center.x, center.y - outer), Offset(center.x, center.y + outer), 1.dp.toPx())
        for (i in 0 until 16) {
            val a = (i * 22.5f) * PI.toFloat() / 180f
            val r = if (i % 4 == 0) outer * 0.78f else outer * 0.86f
            drawCircle(
                color = if (i == 4) GlyphRed else GlyphWhite.copy(alpha = 0.35f),
                radius = if (i == 4) 3.2.dp.toPx() else 1.6.dp.toPx(),
                center = Offset(center.x + cos(a) * r, center.y + sin(a) * r),
            )
        }
        val beam = (angle - 90f) * PI.toFloat() / 180f
        drawLine(
            GlyphRed.copy(alpha = 0.65f),
            center,
            Offset(center.x + cos(beam) * outer * 0.96f, center.y + sin(beam) * outer * 0.96f),
            1.dp.toPx(),
        )
        drawCircle(GlyphWhite, 4.dp.toPx(), center)
        drawCircle(GlyphRed, 1.8.dp.toPx(), center)
    }
}

@Composable
private fun ChoiceCard(
    number: String,
    title: String,
    body: String,
    buttonText: String,
    glyph: Glyph,
    onClick: () -> Unit,
    outlined: Boolean = false,
) {
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(GlyphSurface.copy(alpha = 0.92f), shape)
            .border(1.dp, GlyphLine, shape)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                number,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = DotoFamily,
                    color = GlyphRed,
                    fontSize = 12.sp,
                ),
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(1.dp).height(12.dp).background(GlyphLine))
            Spacer(Modifier.width(10.dp))
            MicroLabel(if (outlined) "АЛЬТЕРНАТИВА" else "ЛОКАЛЬНАЯ ФОНОТЕКА", color = GlyphDim)
            Spacer(Modifier.weight(1f))
            GlyphIcon(glyph, tint = GlyphGray, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = GlyphWhite,
                fontSize = 15.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray),
        )
        Spacer(Modifier.height(14.dp))
        GlyphButton(
            text = buttonText,
            onClick = onClick,
            filled = !outlined,
            leading = if (outlined) null else Glyph.Folder,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScanCard(status: String, found: Int) {
    val shape = RoundedCornerShape(2.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(GlyphSurface, shape)
            .border(1.dp, GlyphLine, shape)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DotSpinner(Modifier.size(30.dp), color = GlyphWhite)
        Spacer(Modifier.height(14.dp))
        Text(
            "ЧИТАЮ ПАПКУ",
            style = MaterialTheme.typography.titleLarge.copy(color = GlyphWhite, fontSize = 17.sp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            status.ifBlank { "ПОДГОТОВКА…" },
            style = MaterialTheme.typography.labelSmall.copy(
                color = GlyphGray,
                fontSize = 8.sp,
                letterSpacing = 0.4.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MicroLabel("НАЙДЕНО", color = GlyphDim)
            Spacer(Modifier.width(8.dp))
            Text(
                found.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = DotoFamily,
                    color = GlyphRed,
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                ),
            )
            Spacer(Modifier.width(8.dp))
            MicroLabel("ТРЕКОВ", color = GlyphDim)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Не закрывайте приложение. Большая фонотека может занять несколько минут.",
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim),
        )
    }
}
