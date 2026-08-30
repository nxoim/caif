package com.nxoim.sample.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal object KanbanDefaults {
    val cardShape = RoundedCornerShape(24.dp)
    val nestedCardShape = RoundedCornerShape(18.dp)
    val cardPadding = 20.dp
    val expandedCardPadding = 24.dp
    val sectionSpacing = 16.dp
    val listItemSpacing = 2.dp
}

object ListItemShape {
    val First = RoundedCornerShape(18.dp, 18.dp, 6.dp, 6.dp)
    val Middle = RoundedCornerShape(6.dp)
    val Last = RoundedCornerShape(6.dp, 6.dp, 18.dp, 18.dp)
    val Single = RoundedCornerShape(18.dp)

    fun auto(
        index: Int,
        listSize: Int,
    ): RoundedCornerShape = when {
        listSize <= 1 -> Single
        index == 0 -> First
        index == listSize - 1 -> Last
        else -> Middle
    }
}
