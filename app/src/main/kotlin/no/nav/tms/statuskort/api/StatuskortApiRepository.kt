package no.nav.tms.statuskort.api

import kotliquery.queryOf
import no.nav.tms.common.postgres.JsonbHelper.json
import no.nav.tms.common.postgres.PostgresDatabase
import no.nav.tms.statuskort.Sensitivitet
import no.nav.tms.statuskort.Statuskort

class StatuskortApiRepository(private val database: PostgresDatabase) {

    fun hentAktiveStatuskort(ident: String): List<Statuskort> =
        database.list {
            queryOf(
                "select * from statuskort where ident = :ident and aktiv = true order by opprettet",
                mapOf("ident" to ident)
            ).map { row ->
                Statuskort(
                    statuskortId = row.string("statuskortId"),
                    ident = row.string("ident"),
                    tjeneste = row.string("tjeneste"),
                    innhold = row.json("innhold"),
                    sensitivitet = row.string("sensitivitet").let(Sensitivitet::parse),
                    produsent = row.json("produsent"),
                    aktiv = row.boolean("aktiv"),
                    inaktivert = row.zonedDateTimeOrNull("inaktivert"),
                    opprettet = row.zonedDateTime("opprettet"),
                    sistEndret = row.zonedDateTime("sistEndret"),
                )
            }
        }
}
