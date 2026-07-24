package no.nav.tms.statuskort.events

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.common.postgres.UniqueConstraintException
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.SkippableMessageException
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription
import no.nav.tms.statuskort.Innhold
import no.nav.tms.statuskort.Produsent
import no.nav.tms.statuskort.Sensitivitet
import no.nav.tms.statuskort.Statuskort
import no.nav.tms.statuskort.common.ZonedDateTimeHelper
import no.nav.tms.statuskort.common.defaultObjectMapper

class OpprettStatuskortSubscriber(
    private val repository: StatuskortRepository,
) : Subscriber() {

    private val log = KotlinLogging.logger {}
    private val objectMapper = defaultObjectMapper()

    override fun subscribe(): Subscription = Subscription
        .forEvent("opprett")
        .withFields(
            "statuskortId",
            "ident",
            "tjeneste",
            "innhold",
            "sensitivitet",
            "produsent",
        )

    override suspend fun receive(jsonMessage: JsonMessage) {
        val statuskortId = jsonMessage["statuskortId"].asText()

        log.info { "Opprett-event mottatt for statuskort $statuskortId" }

        val now = ZonedDateTimeHelper.nowAtUtc()

        val statuskort = Statuskort(
            statuskortId = statuskortId,
            ident = jsonMessage["ident"].asText(),
            tjeneste = jsonMessage["tjeneste"].asText(),
            innhold = objectMapper.treeToValue<Innhold>(jsonMessage["innhold"]),
            sensitivitet = Sensitivitet.parse(jsonMessage["sensitivitet"].asText()),
            produsent = objectMapper.treeToValue<Produsent>(jsonMessage["produsent"]),
            aktiv = true,
            opprettet = now,
            sistEndret = now,
            inaktivert = null,
        )

        try {
            repository.opprettStatuskort(statuskort)
            repository.loggEvent(statuskort.statuskortId, "opprett", statuskort.innhold)
            log.info { "Opprettet statuskort $statuskortId etter event fra kafka" }
        } catch (e: UniqueConstraintException) {
            log.info { "Ignorerte duplikat statuskort $statuskortId" }
            throw DuplikatStatuskortException()
        }
    }
}

class DuplikatStatuskortException : SkippableMessageException("Statuskort med samme statuskortId finnes allerede")
