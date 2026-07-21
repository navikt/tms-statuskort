package no.nav.tms.statuskort.statuskort

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.MessageException
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription

class InaktiverStatuskortSubscriber(
    private val repository: StatuskortRepository,
) : Subscriber() {

    private val log = KotlinLogging.logger {}

    override fun subscribe(): Subscription = Subscription
        .forEvent("inaktiver")
        .withFields("statuskortId")

    override suspend fun receive(jsonMessage: JsonMessage) {
        val statuskortId = jsonMessage["statuskortId"].asText()
        log.info { "Inaktiver-event mottatt for statuskort $statuskortId" }

        val statuskort = repository.hentStatuskort(statuskortId)
            ?: run {
                log.warn { "Fant ikke statuskort $statuskortId å inaktivere" }
                throw StatuskortInaktiveringMissingException()
            }

        if (statuskort.aktiv) {
            repository.inaktiverStatuskort(statuskort.statuskortId)
            repository.loggEvent(statuskort.statuskortId, statuskort.ident, "inaktiver")
            log.info { "Inaktiverte statuskort $statuskortId etter event fra kafka" }
        } else {
            log.warn { "Mottatt inaktiver-event for allerede inaktivert statuskort $statuskortId" }
        }
    }
}

class StatuskortInaktiveringMissingException : MessageException("Fant ikke statuskort som skulle inaktiveres")
