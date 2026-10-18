package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicLevelProgression
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.LevelProgression
import ch.magori.blox.core.game.rules.RotationSystem

/**
 * The rules of a game mode, as the engine applies them. The other policies (gravity, scoring,
 * entry delay, held inputs) join as the engine needs them.
 *
 * @property rotationSystem the orientations of the pieces, and how they turn.
 * @property levelProgression the starting levels, and the level reached as lines are cleared.
 * @property spawnPivot where the pivot of a new piece appears.
 */
public data class Rules(
    public val rotationSystem: RotationSystem,
    public val levelProgression: LevelProgression,
    public val spawnPivot: Position,
)

/** The rules of the Classic mode (NES, 1989). Pieces appear with their pivot at (5, 0) (ROM, meatfighter.com). */
public val ClassicRules: Rules = Rules(
    rotationSystem = ClassicRotationSystem,
    levelProgression = ClassicLevelProgression,
    spawnPivot = Position(5, 0),
)
