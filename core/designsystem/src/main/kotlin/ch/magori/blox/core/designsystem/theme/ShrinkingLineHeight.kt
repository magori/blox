package ch.magori.blox.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em

/**
 * This style, for a text that shrinks: its line height in sp, which would stay as large while the text shrinks, made
 * a proportion of its size (20 sp for 14 sp give 1.43 em), the same at the largest size.
 */
fun TextStyle.shrinkingLineHeight(): TextStyle =
    if (lineHeight.isSp && fontSize.isSp) copy(lineHeight = (lineHeight.value / fontSize.value).em) else this
