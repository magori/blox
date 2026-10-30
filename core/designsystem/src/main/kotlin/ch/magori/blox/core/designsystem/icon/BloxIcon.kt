package ch.magori.blox.core.designsystem.icon

/**
 * The icons of BloX (maquette, section Icônes), each drawn twice: [pixels], 8 lines of 8 pixels, full (`#`) or empty
 * (`.`), for the Original theme; [strokes], the path data of its strokes on a grid of 24 (as an SVG `d`), [strokeWidth]
 * thick, for the others, or the shapes it fills when it is [filled]. [BloxIcon] (the composable) draws the one of the
 * theme.
 */
enum class BloxIcon(
    internal val pixels: List<String>,
    internal val strokes: String,
    internal val strokeWidth: Float = STROKE_WIDTH,
    internal val filled: Boolean = false,
) {
    /** Jouer, Écouter, Reprendre. */
    PLAY(
        listOf("..#.....", "..##....", "..###...", "..####..", "..####..", "..###...", "..##....", "..#....."),
        "M7 4.5v15l12-7.5z",
    ),

    /** Arrêter. */
    STOP(
        listOf("........", ".######.", ".######.", ".######.", ".######.", ".######.", ".######.", "........"),
        "M6 6h12v12H6z",
    ),

    /** MES MUSIQUES. */
    LIST(
        listOf("........", "##.#####", "........", "##.#####", "........", "##.#####", "........", "........"),
        "M9 6h11M9 12h11M9 18h11M4 6h1M4 12h1M4 18h1",
    ),

    /** Au hasard. */
    DICE(
        listOf("########", "#......#", "#.#..#.#", "#......#", "#......#", "#.#..#.#", "#......#", "########"),
        "M7 4h10a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z " +
            "M9 9h.01M15 9h.01M12 12h.01M9 15h.01M15 15h.01",
    ),

    /** Nouvelle. */
    PLUS(
        listOf("...##...", "...##...", "...##...", "########", "########", "...##...", "...##...", "...##..."),
        "M12 5v14M5 12h14",
    ),

    /** Modifier: a pencil. */
    EDIT(
        listOf("......#.", ".....###", "....###.", "...###..", "..###...", ".###....", ".##.....", "#......."),
        "M4 20h4L19 9l-4-4L4 16zM13 7l4 4",
    ),

    /** Renommer: a T. */
    RENAME(
        listOf("........", "#######.", "#..#..#.", "...#....", "...#....", "...#....", "..###...", "........"),
        "M5 6h14M12 6v12M9 18h6",
    ),

    /** Supprimer: a bin. */
    TRASH(
        listOf("..####..", "########", "........", ".######.", ".#.##.#.", ".#.##.#.", ".#.##.#.", ".######."),
        "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6",
    ),

    /** COMPOSER UNE MUSIQUE, Utiliser: a note. */
    NOTE(
        listOf("..######", "..######", "..#....#", "..#....#", "..#....#", "###..###", "###..###", "........"),
        "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM20 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z",
    ),

    /** The music « Aucune »: a note, struck through (pictogrammes.md, P5.87). */
    NOTE_OFF(
        listOf("#.######", ".#.#####", "..#....#", "..##...#", "..#.#..#", "###..#..", "###...#.", ".......#"),
        "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM20 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM3 3l18 18",
    ),

    /** The row Musique of the home, which opens its sheet: a chevron down (pictogrammes.md, P5.87). */
    CHEVRON_DOWN(
        listOf("........", "........", "#......#", ".#....#.", "..#..#..", "...##...", "........", "........"),
        "M6 9l6 6 6-6",
        strokeWidth = 2.5f,
    ),

    /** Vibrations, in the settings: a phone that shakes (docs/design-system/pictogrammes.md). */
    VIBRATE(
        listOf("..####..", "..#..#..", "#.#..#.#", "#.#..#.#", "#.#..#.#", "..#..#..", "..####..", "........"),
        "M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM3 8v8M21 8v8",
    ),

    /** Vibration des touches, in the settings: a finger that touches. */
    TOUCH(
        listOf("...#....", "...#....", "...###..", "#..####.", "##.####.", ".######.", "..####..", "..####.."),
        "M10 11V5.5a1.5 1.5 0 0 1 3 0V11M13 10.5a1.5 1.5 0 0 1 3 0V12M16 11.5a1.5 1.5 0 0 1 3 0V15a6 6 0 0 1-6 6h-1.5" +
            "a5 5 0 0 1-4-2l-3-4a1.4 1.4 0 0 1 2.1-1.9L10 15",
    ),

    /** The volumes, in the settings: a loudspeaker and its waves. */
    VOLUME(
        listOf("...#....", "..##..#.", "####.#.#", "####.#.#", "####.#.#", "..##..#.", "...#....", "........"),
        "M4 9h4l5-4v14l-5-4H4zM16.5 9a4 4 0 0 1 0 6M19 6.5a7.5 7.5 0 0 1 0 11",
    ),

    /** Effets réduits, in the settings: rays of light, struck through (docs/design-system/pictogrammes.md). */
    REDUCED_EFFECTS(
        listOf("#..##...", ".#....#.", "..#.....", "#..#...#", "#...#..#", ".....#..", ".#....#.", "...##..#"),
        "M12 3v3M12 18v3M3 12h3M18 12h3M6.3 6.3l2 2M15.7 15.7l2 2M6.3 17.7l2-2M15.7 8.3l2-2M3 3l18 18",
    ),

    /** Record du Classique, in the settings: a cup. */
    TROPHY(
        listOf(".######.", "#.####.#", "#.####.#", ".######.", "..####..", "...##...", "..####..", "........"),
        "M8 4h8v5a4 4 0 0 1-8 0zM8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4",
    ),

    /** Menu: a house. */
    HOME(
        listOf("...##...", "..####..", ".######.", "########", ".######.", ".##..##.", ".##..##.", ".##..##."),
        "M3 11l9-8 9 8M5 9.5V21h5v-6h4v6h5V9.5",
    ),

    /** Rejouer: an arrow in a loop. */
    REPLAY(LoopPixels, LOOP_STROKES),

    /** Paramètres: a gear. */
    GEAR(
        listOf("...##...", ".######.", ".##..##.", "###..###", "###..###", ".##..##.", ".######.", "...##..."),
        "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z " +
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 " +
            "1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06 " +
            "a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09 " +
            "A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 " +
            "1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 " +
            "1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21 " +
            "a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
    ),

    /** Annuler: an arrow turning back. */
    UNDO(
        listOf("..#.....", ".##.....", "#######.", ".##....#", "..#....#", ".......#", "......#.", "..####.."),
        "M9 14L4 9l5-5M4 9h11a5 5 0 0 1 0 10h-3",
    ),

    /** Copier: two frames. */
    COPY(
        listOf("#####...", "#...#...", "#..#####", "#..#...#", "####...#", "...#...#", "...#####", "........"),
        "M9 9h11v11H9zM5 15H4V4h11v1",
    ),

    /** Effacer: an eraser. */
    ERASE(
        listOf("....##..", "...####.", "..######", ".#.####.", "#...##..", ".#.#....", "..#.....", "########"),
        "M20 20H8l-4-4 10-10 6 6-8 8M9 11l5 5",
    ),

    /** Poser: a cell. */
    PLACE(
        listOf("........", ".######.", ".#....#.", ".#.##.#.", ".#.##.#.", ".#....#.", ".######.", "........"),
        "M5 5h14v14H5zM9 9h6v6H9z",
    ),

    /** Allonger: an arrow to the right. */
    EXTEND(
        listOf("........", ".....#..", ".....##.", "#######.", "########", ".....##.", ".....#..", "........"),
        "M3 12h16M14 7l5 5-5 5",
    ),

    /** The key B: a rotation anticlockwise. */
    ROTATE_LEFT(
        listOf("#.####..", "##....#.", "###....#", ".......#", ".......#", "#......#", ".#....#.", "..####.."),
        "M4 12a8 8 0 1 0 2.34-5.66M4 4v5h5",
    ),

    /** The key A: a rotation clockwise, the arrow in a loop of [REPLAY]. */
    ROTATE_RIGHT(LoopPixels, LOOP_STROKES),

    /** Changer de jeu: a cartridge (pictogrammes.md, P5.87). */
    CARTRIDGE(
        listOf(".######.", ".#....#.", ".#....#.", ".######.", ".######.", ".######.", "..####..", "..#..#.."),
        "M6 3h12v13l-2 5H8l-2-5zM9 7h6v6H9zM10 18h4",
    ),

    /** The record chip of the home: a star (pictogrammes.md, P5.87). */
    STAR(
        listOf("...##...", "...##...", "########", ".######.", "..####..", ".##..##.", ".#....#.", "........"),
        "M12 3.2l2.6 5.6 6.1.7-4.5 4.1 1.2 6.1L12 16.6l-5.4 3.1 1.2-6.1-4.5-4.1 6.1-.7z",
    ),

    /** The tile Composer une musique: a note and a pencil (pictogrammes.md, P5.87). */
    COMPOSE(
        listOf("..######", "..######", "..#....#", "..#..#.#", "..#.###.", "#####...", "####....", "........"),
        "M9 18V5l11-2v6M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM14 21h2.5l5-5-2.5-2.5-5 5z",
    ),

    /** The choice made (Utiliser, En jeu, the sheets): a check (pictogrammes.md, P5.87). */
    CHECK(
        listOf("........", ".......#", "......##", "#....##.", "##..##..", ".####...", "..##....", "........"),
        "M4 12.5l5 5L20 6.5",
        strokeWidth = 3f,
    ),

    /** A message of error (Enregistrer): an exclamation mark in a circle (pictogrammes.md, P5.87). */
    ALERT(
        listOf("..####..", ".#....#.", "#..##..#", "#..##..#", "#......#", "#..##..#", ".#....#.", "..####.."),
        "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0zM12 7v6M12 16.5v.5",
        strokeWidth = 2.5f,
    ),

    /** The console of the choice of the game, waiting: an arrow down, into it (pictogrammes.md, P5.87). */
    INSERT(
        listOf("...##...", "...##...", "...##...", "...##...", "#..##..#", ".######.", "..####..", "...##..."),
        "M12 3v16M5 13l7 7 7-7",
        strokeWidth = 2.5f,
    ),

    /** The eject of the console of the choice of the game: a triangle over a bar, filled (pictogrammes.md, P5.87). */
    EJECT(
        listOf("...##...", "..####..", ".######.", "########", "........", "########", "########", "........"),
        "M12 4L20 13H4z" + "M5.5 16h13a1.5 1.5 0 0 1 1.5 1.5v.5a1.5 1.5 0 0 1-1.5 1.5h-13a1.5 1.5 0 0 1-1.5-1.5v-.5" +
            "a1.5 1.5 0 0 1 1.5-1.5z",
        filled = true,
    ),
}

/** The arrow in a loop, turning clockwise: [BloxIcon.REPLAY] and [BloxIcon.ROTATE_RIGHT]. */
private val LoopPixels =
    listOf("..####.#", ".#....##", "#....###", "#.......", "#.......", "#......#", ".#....#.", "..####..")
private const val LOOP_STROKES = "M20 12a8 8 0 1 1-2.34-5.66M20 4v5h-5"
