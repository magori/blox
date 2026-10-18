package ch.magori.blox.feature.game

/** What the system back does on the game screen. */
internal enum class BackAction {
    /** The game pauses. */
    PAUSE,

    /** The paused game resumes, as Reprendre does. */
    RESUME,

    /** The game is left, as Menu does: the usual back of the navigation. */
    LEAVE,
}

/**
 * What the system back does in [uiState]: it pauses the game, resumes the paused game, and leaves a game
 * that is over (the game over screen has priority, even over a pause).
 */
internal fun backAction(uiState: GameUiState): BackAction = when {
    uiState.gameOver != null -> BackAction.LEAVE
    uiState.paused -> BackAction.RESUME
    else -> BackAction.PAUSE
}
