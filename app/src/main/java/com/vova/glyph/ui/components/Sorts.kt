package com.vova.glyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vova.glyph.data.TrackSort
import com.vova.glyph.data.TrackSorting
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphRed
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun FilterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Int = 34,
) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier
            .size(size.dp)
            .clip(shape)
            .background(if (active) GlyphWhite else GlyphSurface)
            .border(1.dp, if (active) GlyphWhite else GlyphLine, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(
            glyph = Glyph.Filter,
            tint = if (active) GlyphBlack else GlyphWhite,
            modifier = Modifier.size((size * 0.44f).dp),
            stroke = 1.6.dp,
        )
    }
}

@Composable
fun SortDialog(
    options: List<Pair<TrackSort, String>>,
    sorting: TrackSorting,
    onSortBy: (TrackSort) -> Unit,
    onReverse: (Boolean) -> Unit,
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MicroLabel("СОРТИРОВКА", color = GlyphRed)
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
                options.forEach { (sort, label) ->
                    SortOptionRow(
                        label = label,
                        selected = sorting.by == sort,
                        onClick = { onSortBy(sort) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(10.dp))
                Hairline()
                Spacer(Modifier.height(12.dp))
                SortOptionRow(
                    label = "РЕВЕРС",
                    selected = sorting.reverse,
                    onClick = { onReverse(!sorting.reverse) },
                )
                Spacer(Modifier.height(18.dp))
                GlyphButton(
                    text = "ЗАКРЫТЬ",
                    onClick = onDismiss,
                    filled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) GlyphWhite else GlyphBlack)
            .border(1.dp, if (selected) GlyphWhite else GlyphLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MicroLabel(label, color = if (selected) GlyphBlack else GlyphGray)
        Spacer(Modifier.weight(1f))
        if (selected) {
            GlyphIcon(
                glyph = Glyph.Check,
                tint = GlyphBlack,
                modifier = Modifier.size(13.dp),
                stroke = 1.8.dp,
            )
        } else {
            Box(Modifier.size(13.dp).border(1.dp, GlyphLine, RoundedCornerShape(1.dp)))
        }
    }
}
