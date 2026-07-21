package no.nav.tms.statuskort.statuskort

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.MessageException
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription

class OppdaterStatuskortSubscriber(
    private val repository: StatuskortRepository,
) : Subscriber() {

    private val log = KotlinLogging.logger {}
    private val objectMapper = defaultObjectMapper()

    override fun subscribe(): Subscription = Subscription
        .forEvent("oppdater")
        .withFields(
            "statuskortId",
            "innhold",
        )

    override suspend fun receive(jsonMessage: JsonMessage) {
        val statuskortId = jsonMessage["statuskortId"].asText()
        log.info { "Oppdater-event mottatt for statuskort $statuskortId" }

        val statuskort = repository.hentStatuskort(statuskortId)
            ?: run {
                log.warn { "Fant ikke statuskort $statuskortId å oppdatere" }
                throw StatuskortIkkeFunnetException()
            }

        if (!statuskort.aktiv) {
            log.warn { "Avviste oppdatering av inaktivert statuskort $statuskortId" }
            throw StatuskortInaktivtException()
        }

        val innhold = objectMapper.treeToValue<Innhold>(jsonMessage["innhold"])
        repository.oppdaterInnhold(statuskort.statuskortId, innhold)
        repository.loggEvent(statuskort.statuskortId, statuskort.ident, "oppdater", innhold)
        log.info { "Oppdaterte innhold på statuskort $statuskortId etter event fra kafka" }
    }
}

class StatuskortIkkeFunnetException : MessageException("Fant ikke statuskort som skulle oppdateres")

class StatuskortInaktivtException : MessageException("Kan ikke oppdatere et inaktivert statuskort")
