package com.vova.glyph.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vova.glyph.data.LyricQuote
import com.vova.glyph.data.Prefs
import com.vova.glyph.data.Track
import com.vova.glyph.ui.theme.AppFont
import com.vova.glyph.ui.theme.FontController
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite
import kotlinx.coroutines.launch

@Composable
fun QuoteImageDialog(
    track: Track,
    quote: LyricQuote,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(2.dp)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val prefs = remember(context.applicationContext) { Prefs(context.applicationContext) }
    var quoteFont by remember(quote.id) {
        mutableStateOf(AppFont.fromId(prefs.quoteFont, FontController.body))
    }
    var quoteSize by remember(quote.id) {
        mutableStateOf(QuoteTextSize.fromName(prefs.quoteSize))
    }

    val colors = remember(GlyphBlack, GlyphWhite, GlyphRed) {
        QuoteCardColors(
            background = GlyphBlack.toArgb(),
            text = GlyphWhite.toArgb(),
            accent = GlyphRed.toArgb(),
        )
    }
    val spec = remember(
        quote.text,
        track.effectiveTitle,
        track.effectiveArtist,
        track.artwork,
        colors,
        quoteFont,
        quoteSize,
        FontController.display,
    ) {
        QuoteCardSpec(
            quote = quote.text,
            title = track.effectiveTitle,
            artist = track.effectiveArtist,
            artwork = track.artwork,
            colors = colors,
            quoteFont = quoteFont,
            titleFont = FontController.display,
            textSize = quoteSize,
        )
    }

    var bitmap by remember(quote.id) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(quote.id) { mutableStateOf(false) }
    var notice by remember(quote.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(spec) {
        failed = false
        val rendered = renderQuoteCard(context, spec)
        if (rendered != null) {
            bitmap = rendered
        } else {
            bitmap = null
            failed = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(
                Modifier
                    .heightIn(max = screenHeight * 0.86f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MicroLabel("КАРТИНКА ЦИТАТЫ", color = GlyphRed)
                    Spacer(Modifier.weight(1f))
                    MicroLabel("1:1", color = GlyphDim, fontSize = 8)
                    Spacer(Modifier.width(8.dp))
                    GlyphIconButton(
                        glyph = Glyph.Close,
                        onClick = onDismiss,
                        size = 32.dp,
                        iconSize = 16.dp,
                        tint = GlyphGray,
                    )
                }
                Spacer(Modifier.height(12.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val side = minOf(maxWidth, screenHeight * 0.38f)
                    Box(
                        Modifier
                            .size(side)
                            .align(Alignment.Center)
                            .background(GlyphBlack)
                            .border(1.dp, GlyphLine),
                        contentAlignment = Alignment.Center,
                    ) {
                        val image = bitmap
                        when {
                            failed -> MicroLabel(
                                "НЕ УДАЛОСЬ ПОСТРОИТЬ КАРТИНКУ",
                                color = GlyphDim,
                                fontSize = 9,
                            )

                            image == null -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                DotSpinner(Modifier.size(22.dp))
                                Spacer(Modifier.height(10.dp))
                                MicroLabel("ПОДГОТОВКА…", color = GlyphDim)
                            }

                            else -> {
                                val imageBitmap = remember(image) { image.asImageBitmap() }
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                MicroLabel("ШРИФТ", color = GlyphDim, fontSize = 8)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppFont.entries.forEach { font ->
                        GlyphChip(
                            text = font.quoteLabel(),
                            selected = font == quoteFont,
                            onClick = {
                                quoteFont = font
                                prefs.quoteFont = font.id
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                MicroLabel("РАЗМЕР", color = GlyphDim, fontSize = 8)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuoteTextSize.entries.forEach { option ->
                        GlyphChip(
                            text = option.label,
                            selected = option == quoteSize,
                            onClick = {
                                quoteSize = option
                                prefs.quoteSize = option.name
                            },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                notice?.let { text ->
                    MicroLabel(text, color = GlyphRed)
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "Закрыть",
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = "Поделиться",
                        onClick = {
                            val image = bitmap ?: return@GlyphButton
                            notice = null
                            scope.launch {
                                val shared = shareQuoteCard(context, image, track.id, quote.id)
                                if (!shared) notice = "НЕ УДАЛОСЬ ПОДЕЛИТЬСЯ"
                            }
                        },
                        enabled = bitmap != null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun AppFont.quoteLabel(): String = when (this) {
    AppFont.DOTO -> "DOTO"
    AppFont.PIXEL -> "PIXEL"
    AppFont.GROTESK -> "GROTESK"
    AppFont.SYSTEM -> "SYSTEM"
}
