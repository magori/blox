package ch.magori.blox.core.designsystem.state

import androidx.compose.runtime.saveable.SaveableStateRegistry

/**
 * The saved state of a screen, as a recreation (a rotation) keeps it: provided as `LocalSaveableStateRegistry`, it
 * saves what is composed under it ([performSave]); once its [delegate] is swapped for one built on what was saved, what
 * is composed again under it gets it back.
 */
class SwappableRegistry : SaveableStateRegistry {
    var delegate: SaveableStateRegistry = SaveableStateRegistry(null) { true }

    override fun canBeSaved(value: Any): Boolean = delegate.canBeSaved(value)

    override fun consumeRestored(key: String): Any? = delegate.consumeRestored(key)

    override fun registerProvider(
        key: String,
        valueProvider: () -> Any?,
    ): SaveableStateRegistry.Entry = delegate.registerProvider(key, valueProvider)

    override fun performSave(): Map<String, List<Any?>> = delegate.performSave()
}
