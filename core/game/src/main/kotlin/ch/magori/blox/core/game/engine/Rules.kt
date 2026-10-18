package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicEntryDelay
import ch.magori.blox.core.game.rules.ClassicGravityTable
import ch.magori.blox.core.game.rules.ClassicInputRepeat
import ch.magori.blox.core.game.rules.ClassicLevelProgression
import ch.magori.blox.core.game.rules.ClassicLineClearDelay
import ch.magori.blox.core.game.rules.ClassicRotationSystem
import ch.magori.blox.core.game.rules.ClassicScoringRule
import ch.magori.blox.core.game.rules.ClassicStackDangerRule
import ch.magori.blox.core.game.rules.EntryDelay
import ch.magori.blox.core.game.rules.GravityTable
import ch.magori.blox.core.game.rules.InputRepeat
import ch.magori.blox.core.game.rules.LevelProgression
import ch.magori.blox.core.game.rules.LineClearDelay
import ch.magori.blox.core.game.rules.RotationSystem
import ch.magori.blox.core.game.rules.ScoringRule
import ch.magori.blox.core.game.rules.StackDangerRule

/**
 * The rules of a game mode, as the engine applies them.
 *
 * @property rotationSystem the orientations of the pieces, and how they turn.
 * @property levelProgression the starting levels, and the level reached as lines are cleared.
 * @property spawnPivot where the pivot of a new piece appears.
 * @property gravityTable how many ticks the falling piece waits, at each level, before dropping one row.
 * @property entryDelay how many ticks pass between a piece locking and the next one appearing.
 * @property lineClearDelay how many ticks the line clear animation lasts, before the entry delay.
 * @property scoringRule the points earned by clearing lines and by soft dropping.
 * @property stackDangerRule whether the stack is high enough to be in danger.
 * @property inputRepeat how a held shift (DAS) and a held soft drop repeat.
 */
public data class Rules(
    public val rotationSystem: RotationSystem,
    public val levelProgression: LevelProgression,
    public val spawnPivot: Position,
    public val gravityTable: GravityTable,
    public val entryDelay: EntryDelay,
    public val lineClearDelay: LineClearDelay,
    public val scoringRule: ScoringRule,
    public val stackDangerRule: StackDangerRule,
    public val inputRepeat: InputRepeat,
)

/** The rules of the Classic mode (NES, 1989). Pieces appear with their pivot at (5, 0) (ROM, meatfighter.com). */
public val ClassicRules: Rules = Rules(
    rotationSystem = ClassicRotationSystem,
    levelProgression = ClassicLevelProgression,
    spawnPivot = Position(5, 0),
    gravityTable = ClassicGravityTable,
    entryDelay = ClassicEntryDelay,
    lineClearDelay = ClassicLineClearDelay,
    scoringRule = ClassicScoringRule,
    stackDangerRule = ClassicStackDangerRule,
    inputRepeat = ClassicInputRepeat,
)
