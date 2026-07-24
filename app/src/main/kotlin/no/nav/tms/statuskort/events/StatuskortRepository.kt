package no.nav.tms.statuskort.events

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import no.nav.tms.common.postgres.JsonbHelper.json
import no.nav.tms.common.postgres.JsonbHelper.jsonOrNull
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import no.nav.tms.common.postgres.PostgresDatabase
import no.nav.tms.statuskort.EventHistorikkEntry
import no.nav.tms.statuskort.Innhold
import no.nav.tms.statuskort.Sensitivitet
import no.nav.tms.statuskort.Statuskort
import no.nav.tms.statuskort.common.ZonedDateTimeHelper

class StatuskortRepository(private val database: PostgresDatabase) {

    fun opprettStatuskort(statuskort: Statuskort) {
        database.update {
            queryOf(
                """
                    insert into statuskort(
                        statuskortId,
                        ident,
                        tjeneste,
                        innhold,
                        sensitivitet,
                        produsent,
                        aktiv,
                        inaktivert,
                        opprettet,
                        sistEndret
                    ) values (
                        :statuskortId,
                        :ident,
                        :tjeneste,
                        :innhold,
                        :sensitivitet,
                        :produsent,
                        :aktiv,
                        :inaktivert,
                        :opprettet,
                        :sistEndret
                    )
                """,
                mapOf(
                    "statuskortId" to statuskort.statuskortId,
                    "ident" to statuskort.ident,
                    "tjeneste" to statuskort.tjeneste,
                    "innhold" to statuskort.innhold.toJsonb(),
                    "sensitivitet" to statuskort.sensitivitet.name.lowercase(),
                    "produsent" to statuskort.produsent.toJsonb(),
                    "aktiv" to statuskort.aktiv,
                    "opprettet" to statuskort.opprettet,
                    "sistEndret" to statuskort.sistEndret,
                    "inaktivert" to statuskort.inaktivert,
                )
            )
        }
    }

    fun oppdaterInnhold(statuskortId: String, innhold: Innhold) {
        database.update {
            queryOf(
                """
                    update statuskort set
                        innhold = :innhold,
                        sistEndret = :sistEndret
                    where statuskortId = :statuskortId and aktiv = true
                """,
                mapOf(
                    "statuskortId" to statuskortId,
                    "innhold" to innhold.toJsonb(),
                    "sistEndret" to ZonedDateTimeHelper.nowAtUtc(),
                )
            )
        }
    }

    fun inaktiverStatuskort(statuskortId: String) {
        val tidspunkt = ZonedDateTimeHelper.nowAtUtc()
        database.update {
            queryOf(
                """
                    update statuskort set
                        aktiv = false,
                        inaktivert = :tidspunkt,
                        sistEndret = :tidspunkt
                    where statuskortId = :statuskortId and aktiv = true
                """,
                mapOf(
                    "statuskortId" to statuskortId,
                    "tidspunkt" to tidspunkt,
                )
            )
        }
    }

    fun loggEvent(statuskortId: String, eventType: String, innhold: Any? = null) {
        database.update {
            queryOf(
                """
                    insert into statuskort_event_historikk(
                        statuskortId,
                        eventType,
                        innhold,
                        tidspunkt
                    ) values (
                        :statuskortId,
                        :eventType,
                        :innhold,
                        :tidspunkt
                    )
                """,
                mapOf(
                    "statuskortId" to statuskortId,
                    "eventType" to eventType,
                    "innhold" to innhold?.toJsonb(),
                    "tidspunkt" to ZonedDateTimeHelper.nowAtUtc(),
                )
            )
        }
    }

    fun hentEventHistorikk(statuskortId: String): List<EventHistorikkEntry> =
        database.list {
            queryOf(
                "select * from statuskort_event_historikk where statuskortId = :statuskortId order by tidspunkt",
                mapOf("statuskortId" to statuskortId)
            ).map { row ->
                EventHistorikkEntry(
                    statuskortId = row.string("statuskortId"),
                    eventType = row.string("eventType"),
                    innhold = row.jsonOrNull<JsonNode>("innhold"),
                    tidspunkt = row.zonedDateTime("tidspunkt"),
                )
            }
        }

    fun hentStatuskort(statuskortId: String): Statuskort? =
        database.singleOrNull {
            queryOf(
                "select * from statuskort where statuskortId = :statuskortId",
                mapOf("statuskortId" to statuskortId)
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
