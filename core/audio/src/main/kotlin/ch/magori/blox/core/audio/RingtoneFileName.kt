package ch.magori.blox.core.audio

/**
 * The name of the file of a ringtone made of the music named [name] (ADR 0031): the name, each character a file
 * name may not hold replaced by `_`, and a dot it starts with too, which would hide the file, with the `wav` extension;
 * `BloX.wav` when the name holds neither a letter nor a digit. The name is not cut: a name of Mes musiques holds 40
 * characters at the most (`SavedMusic.MAX_NAME_LENGTH`), 160 bytes in UTF-8, far below the 255 bytes of a file name.
 */
internal fun ringtoneFileName(name: String): String {
    val safe = if (name.none { it.isLetterOrDigit() }) {
        FALLBACK_NAME
    } else {
        name
            .mapIndexed { index, char ->
                val hiding = index == 0 && char == '.'
                if (char in FORBIDDEN || char.isISOControl() || hiding) REPLACEMENT else char
            }.joinToString("")
    }
    return "$safe.wav"
}

/** The characters FAT, which the shared storage of Android follows, refuses in a file name. */
private const val FORBIDDEN = "/\\:*?\"<>|"

private const val REPLACEMENT = '_'

private const val FALLBACK_NAME = "BloX"
