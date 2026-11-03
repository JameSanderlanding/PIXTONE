package com.vova.glyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite
import kotlin.math.roundToInt

 
@Composable
fun ColorPickerDialog(
    title: String,
    initial: Color,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit,
) {
    var red by remember { mutableIntStateOf((initial.red * 255f).roundToInt()) }
    var green by remember { mutableIntStateOf((initial.green * 255f).roundToInt()) }
    var blue by remember { mutableIntStateOf((initial.blue * 255f).roundToInt()) }
    val current = Color(red, green, blue)
    val shape = RoundedCornerShape(2.dp)

    var hexEditing by remember { mutableStateOf(false) }
    var hexText by remember { mutableStateOf(hexDigits(red, green, blue)) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(red, green, blue) {
        if (!hexEditing) hexText = hexDigits(red, green, blue)
    }
    LaunchedEffect(hexEditing) {
        if (hexEditing) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

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
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(current, shape)
                            .border(1.dp, GlyphLine, shape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        if (hexEditing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "#",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = GlyphGray,
                                        letterSpacing = 1.sp,
                                    ),
                                )
                                BasicTextField(
                                    value = hexText,
                                    onValueChange = { raw ->
                                        val filtered = raw
                                            .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                                            .take(6)
                                            .uppercase()
                                        hexText = filtered
                                        if (filtered.length == 6) {
                                            red = filtered.substring(0, 2).toInt(16)
                                            green = filtered.substring(2, 4).toInt(16)
                                            blue = filtered.substring(4, 6).toInt(16)
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = GlyphWhite,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp,
                                    ),
                                    cursorBrush = SolidColor(GlyphRed),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Done,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            hexEditing = false
                                            keyboard?.hide()
                                        },
                                    ),
                                    modifier = Modifier
                                        .width(96.dp)
                                        .focusRequester(focusRequester),
                                )
                            }
                        } else {
                            Text(
                                hexOf(current),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = GlyphWhite,
                                    letterSpacing = 1.sp,
                                ),
                                modifier = Modifier.clickable {
                                    hexText = hexDigits(red, green, blue)
                                    hexEditing = true
                                },
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "R $red · G $green · B $blue",
                            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray, fontSize = 11.sp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                ChannelSlider("R", red, GlyphRed) { red = it }
                ChannelSlider("G", green, Color(0xFF6FCF6F)) { green = it }
                ChannelSlider("B", blue, Color(0xFF6F9BFF)) { blue = it }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlyphButton(
                        text = "Отмена",
                        onClick = onDismiss,
                        filled = false,
                        modifier = Modifier.weight(1f),
                    )
                    GlyphButton(
                        text = "ПРИМЕНИТЬ",
                        onClick = { onApply(current) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Int,
    color: Color,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = GlyphDim),
            modifier = Modifier.width(16.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = GlyphLine,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            value.toString().padStart(3, ' '),
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray, fontSize = 11.sp),
            modifier = Modifier.width(34.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

private fun hexOf(color: Color): String {
    val argb = color.toArgb()
    return "#%06X".format(argb and 0xFFFFFF)
}

private fun hexDigits(red: Int, green: Int, blue: Int): String =
    "%02X%02X%02X".format(red, green, blue)
