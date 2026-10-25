package ch.magori.blox.core.designsystem.theme

import android.content.res.Configuration

/**
 * The night of the system, where the Colorful theme takes its dark variant (ADR 0020): the `uiMode` of a preview
 * rendered at night, shared by the screenshot tests of every module. A constant, as an annotation asks.
 */
const val UI_MODE_NIGHT: Int = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
