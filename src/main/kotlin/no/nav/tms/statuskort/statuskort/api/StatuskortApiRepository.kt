package no.nav.tms.statuskort.statuskort.api

import kotliquery.queryOf
import no.nav.tms.common.postgres.PostgresDatabase
import no.nav.tms.statuskort.statuskort.Statuskort
import no.nav.tms.statuskort.statuskort.tilStatuskort

class StatuskortApiRepository(private val database: PostgresDatabase) {

    fun hentAktiveStatuskort(ident: String): List<Statuskort> =
        database.list {
            queryOf(
                "select * from statuskort where ident = :ident and aktiv = true order by opprettet",
                mapOf("ident" to ident)
            ).map { it.tilStatuskort() }
        }
}
