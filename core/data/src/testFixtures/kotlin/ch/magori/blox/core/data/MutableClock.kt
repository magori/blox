package ch.magori.blox.core.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A clock that tells [now], which a test moves, in Greenwich: the clock of a library in the tests, which dates its
 * changes. It starts at [MODIFIED_AT], the time the fake library tells unless it is given another clock.
 */
class MutableClock(var now: Instant = MODIFIED_AT) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = throw UnsupportedOperationException("A library needs no zone")
}
