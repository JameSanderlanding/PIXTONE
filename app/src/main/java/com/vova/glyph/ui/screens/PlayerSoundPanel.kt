package com.vova.glyph.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vova.glyph.MainViewModel
import com.vova.glyph.playback.dsp.EqPreset
import com.vova.glyph.ui.components.Glyph
import com.vova.glyph.ui.components.GlyphButton
import com.vova.glyph.ui.components.GlyphChip
import com.vova.glyph.ui.components.GlyphIcon
import com.vova.glyph.ui.components.GlyphSection
import com.vova.glyph.ui.components.GlyphSlider
import com.vova.glyph.ui.components.GlyphToggle
import com.vova.glyph.ui.components.MicroLabel
import com.vova.glyph.ui.components.Text
import com.vova.glyph.ui.theme.GlyphDim
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PlayerSoundSection(
    vm: MainViewModel,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val dsp by vm.dsp.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(2.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .background(GlyphSurface, shape)
            .border(1.dp, if (expanded) GlyphRed else GlyphLine, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(
            glyph = Glyph.Settings,
            tint = if (expanded) GlyphRed else GlyphDim,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(9.dp))
        MicroLabel("ЗВУК", color = GlyphWhite, fontSize = 9)
        Spacer(Modifier.weight(1f))
        Text(
            String.format(Locale.US, "×%.2f", dsp.speed),
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphGray, fontSize = 11.sp),
        )
        Spacer(Modifier.width(8.dp))
        MicroLabel(
            if (dsp.anyEffectEnabled) "ВКЛ" else "ВЫКЛ",
            color = if (dsp.anyEffectEnabled) GlyphRed else GlyphDim,
            fontSize = 8,
        )
        Spacer(Modifier.width(10.dp))
        GlyphIcon(
            glyph = Glyph.Plus,
            tint = GlyphRed,
            modifier = Modifier
                .size(13.dp)
                .rotate(if (expanded) 45f else 0f),
        )
    }

    if (!expanded) return

    Spacer(Modifier.height(10.dp))

    GlyphSection(number = "01", title = "СКОРОСТЬ") {
        GlyphSlider(
            label = "",
            valueText = String.format(Locale.US, "×%.2f", dsp.speed),
            value = dsp.speed,
            valueRange = 0.5f..1.5f,
            steps = 19,
            onValueChange = vm::setSpeed,
        )
        Spacer(Modifier.height(2.dp))
        MicroLabel(
            "ТОН МЕНЯЕТСЯ ВМЕСТЕ СО СКОРОСТЬЮ",
            color = GlyphDim,
            fontSize = 7,
        )
    }

    Spacer(Modifier.height(10.dp))

    GlyphSection(number = "02", title = "РЕВЕРБЕРАЦИЯ") {
        GlyphToggle(
            label = "ВКЛЮЧИТЬ",
            checked = dsp.reverbEnabled,
            onCheckedChange = vm::setReverbEnabled,
        )
        Spacer(Modifier.height(8.dp))
        GlyphSlider(
            label = "РАЗМЕР",
            valueText = "${dsp.reverbRoom}%",
            value = dsp.reverbRoom.toFloat(),
            valueRange = 0f..100f,
            onValueChange = { vm.setReverbRoom(it.roundToInt()) },
        )
        Spacer(Modifier.height(4.dp))
        GlyphSlider(
            label = "ЗАТУХАНИЕ",
            valueText = "${dsp.reverbDamp}%",
            value = dsp.reverbDamp.toFloat(),
            valueRange = 0f..100f,
            onValueChange = { vm.setReverbDamp(it.roundToInt()) },
        )
        Spacer(Modifier.height(4.dp))
        GlyphSlider(
            label = "УРОВЕНЬ",
            valueText = "${dsp.reverbWet}%",
            value = dsp.reverbWet.toFloat(),
            valueRange = 0f..100f,
            onValueChange = { vm.setReverbWet(it.roundToInt()) },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Реверб считается на телефоне и работает на любом устройстве.",
            style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim, fontSize = 11.sp),
        )
    }

    Spacer(Modifier.height(10.dp))

    GlyphSection(number = "03", title = "БАС") {
        val available = dsp.bassSupported || !dsp.attached
        GlyphToggle(
            label = "ВКЛЮЧИТЬ",
            checked = dsp.bassEnabled,
            onCheckedChange = vm::setBassEnabled,
            enabled = available,
        )
        if (available) {
            Spacer(Modifier.height(8.dp))
            GlyphSlider(
                label = "СИЛА",
                valueText = "${(dsp.bassStrength / 10f).roundToInt()}%",
                value = dsp.bassStrength.toFloat(),
                valueRange = 0f..1000f,
                onValueChange = { vm.setBassStrength(it.roundToInt()) },
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "Устройство не поддерживает усиление баса.",
                style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim, fontSize = 11.sp),
            )
        }
    }

    Spacer(Modifier.height(10.dp))

    GlyphSection(number = "04", title = "ГРОМКОСТЬ") {
        val available = dsp.loudnessSupported || !dsp.attached
        GlyphToggle(
            label = "ВКЛЮЧИТЬ",
            checked = dsp.loudnessEnabled,
            onCheckedChange = vm::setLoudnessEnabled,
            enabled = available,
        )
        if (available) {
            Spacer(Modifier.height(8.dp))
            GlyphSlider(
                label = "УСИЛЕНИЕ",
                valueText = dbLabel(dsp.loudnessGainMb),
                value = dsp.loudnessGainMb.toFloat(),
                valueRange = 0f..1500f,
                onValueChange = { vm.setLoudnessGain(it.roundToInt()) },
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "Устройство не поддерживает усиление громкости.",
                style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim, fontSize = 11.sp),
            )
        }
    }

    Spacer(Modifier.height(10.dp))

    GlyphSection(number = "05", title = "ЭКВАЛАЙЗЕР") {
        val available = dsp.eqSupported || !dsp.attached
        GlyphToggle(
            label = "ВКЛЮЧИТЬ",
            checked = dsp.eqEnabled,
            onCheckedChange = vm::setEqEnabled,
            enabled = available,
        )
        if (available) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AudioPresets.forEach { (preset, label) ->
                    GlyphChip(
                        text = label,
                        selected = dsp.eqPreset == preset,
                        onClick = { vm.setEqPreset(preset) },
                    )
                }
            }
        }
        if (dsp.eqSupported) {
            Spacer(Modifier.height(10.dp))
            dsp.bands.forEachIndexed { index, band ->
                if (index > 0) Spacer(Modifier.height(4.dp))
                GlyphSlider(
                    label = bandLabel(band.centerHz),
                    valueText = dbLabel(band.levelMb),
                    value = band.levelMb.toFloat(),
                    valueRange = dsp.levelMinMb.toFloat()..dsp.levelMaxMb.toFloat(),
                    onValueChange = { vm.setBandLevel(index, it.roundToInt()) },
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Набор полос зависит от устройства. При изменении скорости пресеты подстраиваются под неё.",
                style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim, fontSize = 11.sp),
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                if (dsp.attached) {
                    "Устройство не поддерживает системный эквалайзер."
                } else {
                    "Полосы появятся, когда начнётся воспроизведение."
                },
                style = MaterialTheme.typography.bodySmall.copy(color = GlyphDim, fontSize = 11.sp),
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    GlyphButton(
        text = "СБРОСИТЬ НАСТРОЙКИ",
        onClick = vm::resetDsp,
        filled = false,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val AudioPresets = listOf(
    EqPreset.FLAT to "ПЛОСКИЙ",
    EqPreset.BASS to "БАС",
    EqPreset.VOCAL to "ВОКАЛ",
    EqPreset.ROCK to "РОК",
    EqPreset.CUSTOM to "СВОЙ",
)

private fun bandLabel(hz: Int): String {
    if (hz < 1000) return "$hz Hz"
    val khz = hz / 1000f
    return if (khz == khz.toInt().toFloat()) {
        "${khz.toInt()} kHz"
    } else {
        String.format(Locale.US, "%.1f kHz", khz)
    }
}

private fun dbLabel(mb: Int): String = String.format(Locale.US, "%+.1f dB", mb / 100f)
