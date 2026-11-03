package com.vova.glyph.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LicenseTextDialog(
    title: String,
    assetPath: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val text by produceState<String?>(initialValue = null, assetPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetPath).bufferedReader().use { it.readText() }
            }.getOrDefault("")
        }
    }
    val shape = RoundedCornerShape(2.dp)
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlyphLine, shape),
            color = GlyphSurface,
            shape = shape,
        ) {
            Column(Modifier.padding(20.dp)) {
                MicroLabel(title, color = GlyphRed)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
                    val body = text
                    if (body == null) {
                        DotSpinner(Modifier.size(24.dp).align(Alignment.Center))
                    } else {
                        Text(
                            text = body,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GlyphGray,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                GlyphButton(
                    text = "Закрыть",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
