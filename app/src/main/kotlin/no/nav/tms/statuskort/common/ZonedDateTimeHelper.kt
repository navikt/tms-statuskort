package no.nav.tms.statuskort.common

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object ZonedDateTimeHelper {
    fun nowAtUtc(): ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
}
