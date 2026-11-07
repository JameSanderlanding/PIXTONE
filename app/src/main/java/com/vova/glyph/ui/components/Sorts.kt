package com.vova.glyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vova.glyph.data.TrackSort
import com.vova.glyph.data.TrackSorting
import com.vova.glyph.ui.theme.GlyphBlack
import com.vova.glyph.ui.theme.GlyphGray
import com.vova.glyph.ui.theme.GlyphLine
import com.vova.glyph.ui.theme.GlyphSurface
import com.vova.glyph.ui.theme.GlyphWhite

@Composable
fun SortRow(
    options: List<Pair<TrackSort, String>>,
    sorting: TrackSorting,
    onSortBy: (TrackSort) -> Unit,
    onReverse: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (sort, label) ->
            SortChip(
                label = label,
                selected = sorting.by == sort,
                onClick = { onSortBy(sort) },
            )
        }
        Box(Modifier.width(4.dp))
        SortChip(
            label = "РЕВЕРС",
            selected = sorting.reverse,
            onClick = { onReverse(!sorting.reverse) },
        )
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) GlyphWhite else GlyphSurface)
            .border(1.dp, if (selected) GlyphWhite else GlyphLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        MicroLabel(label, color = if (selected) GlyphBlack else GlyphGray, fontSize = 8)
    }
}
