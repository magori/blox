package ch.magori.blox.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ch.magori.blox.core.data.DailyChallengeRepository
import ch.magori.blox.core.data.DailyResults
import ch.magori.blox.core.game.model.PieceType
import ch.magori.blox.core.game.rules.ClassicRandomizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.reflect.KClass

/**
 * The entry of the Graine du jour (V38, docs/graine-du-jour.md): the date of [today], the local date of the phone, read
 * when the view model is made and again each time the screen shows ([onShown]), never by a timer; the first pieces of
 * its seed, [seedOf] it (`app` gives the seed of its daily game, so that they are the pieces that game draws); and the
 * results of [dailyChallenge] for that date only, none for another date; the [startLevel] of the daily game, which `app`
 * imposes, for the reminder under Jouer.
 */
class DailyViewModel(
    dailyChallenge: DailyChallengeRepository,
    private val startLevel: Int,
    private val today: () -> LocalDate,
    private val seedOf: (LocalDate) -> Long,
) : ViewModel() {
    private val date = MutableStateFlow(today())

    /** The screen, null until the results are read: nothing shows before. */
    internal val uiState: StateFlow<DailyUiState?> = combine(date, dailyChallenge.results, ::uiStateOf)
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** The screen shows, or comes back to the foreground: the date is read again, a new day gives a new seed. */
    fun onShown() {
        date.value = today()
    }

    private fun uiStateOf(
        date: LocalDate,
        results: DailyResults?,
    ): DailyUiState {
        val ofToday = results?.takeIf { it.date == date }
        return DailyUiState(
            date = date,
            firstPieces = firstPiecesOf(seedOf(date)),
            startLevel = startLevel,
            tries = ofToday?.tries ?: 0,
            firstTry = ofToday?.takeIf { it.firstTryOver }?.firstTry,
            best = ofToday?.takeIf { it.firstTryOver || it.best > 0 }?.best,
        )
    }

    /** The [FIRST_PIECES] first pieces of [seed], drawn as the daily game draws them (`ClassicRandomizer.stable`). */
    private fun firstPiecesOf(seed: Long): List<PieceType> {
        val randomizer = ClassicRandomizer.stable(seed)
        return generateSequence(randomizer.next(previous = null)) { randomizer.next(previous = it) }
            .take(FIRST_PIECES)
            .toList()
    }

    /**
     * Builds the [DailyViewModel] of the entry, on the results of [dailyChallenge]; [startLevel], [today] and [seedOf]
     * come from `app`, which gives the daily game the same level, the same date and the same seed (ADR 0007, 0013).
     */
    class Factory(
        private val dailyChallenge: DailyChallengeRepository,
        private val startLevel: Int,
        private val today: () -> LocalDate,
        private val seedOf: (LocalDate) -> Long,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T {
            @Suppress("UNCHECKED_CAST") // The only view model this factory builds.
            return DailyViewModel(dailyChallenge, startLevel, today, seedOf) as T
        }
    }

    private companion object {
        /** « LES 7 PREMIÈRES ». */
        const val FIRST_PIECES = 7
    }
}
