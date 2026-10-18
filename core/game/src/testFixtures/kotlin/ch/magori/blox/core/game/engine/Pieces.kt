package ch.magori.blox.core.game.engine

import ch.magori.blox.core.game.model.Piece
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.model.Position
import ch.magori.blox.core.game.rules.ClassicRotationSystem

/** A piece of [type] as it appears in the Classic mode: pivot at (5, 0), in its spawn orientation. */
public fun spawned(type: PieceType): Piece = Piece(type, ClassicRotationSystem.spawnOrientation(type), Position(5, 0))
