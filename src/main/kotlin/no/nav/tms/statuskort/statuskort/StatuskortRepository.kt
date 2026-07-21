package no.nav.tms.statuskort.statuskort

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import no.nav.tms.common.postgres.JsonbHelper.jsonOrNull
import no.nav.tms.common.postgres.JsonbHelper.toJsonb
import no.nav.tms.common.postgres.PostgresDatabase
import java.util.UUID

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

    fun loggEvent(statuskortId: String, ident: String, eventType: String, data: Any? = null) {
        database.update {
            queryOf(
                """
                    insert into statuskort_event_historikk(
                        hendelsesId,
                        statuskortId,
                        ident,
                        eventType,
                        data,
                        konsumert
                    ) values (
                        :hendelsesId,
                        :statuskortId,
                        :ident,
                        :eventType,
                        :data,
                        :konsumert
                    )
                """,
                mapOf(
                    "hendelsesId" to UUID.randomUUID().toString(),
                    "statuskortId" to statuskortId,
                    "ident" to ident,
                    "eventType" to eventType,
                    "data" to data?.toJsonb(),
                    "konsumert" to ZonedDateTimeHelper.nowAtUtc(),
                )
            )
        }
    }

    fun hentEventHistorikk(statuskortId: String): List<EventHistorikkRad> =
        database.list {
            queryOf(
                "select * from statuskort_event_historikk where statuskortId = :statuskortId order by konsumert",
                mapOf("statuskortId" to statuskortId)
            ).map { row ->
                EventHistorikkRad(
                    hendelsesId = row.string("hendelsesId"),
                    statuskortId = row.string("statuskortId"),
                    ident = row.string("ident"),
                    eventType = row.string("eventType"),
                    data = row.jsonOrNull<JsonNode>("data"),
                    konsumert = row.zonedDateTime("konsumert"),
                )
            }
        }

    fun hentStatuskort(statuskortId: String): Statuskort? =
        database.singleOrNull {
            queryOf(
                "select * from statuskort where statuskortId = :statuskortId",
                mapOf("statuskortId" to statuskortId)
            ).map { it.tilStatuskort() }
        }
}
