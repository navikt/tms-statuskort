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
        log.info { "Inaktiver-event mottatt for statuskort" }

        val statuskort = repository.hentStatuskort(statuskortId)
            ?: run {
                log.warn { "Fant ikke statuskort å inaktivere" }
                throw StatuskortInaktiveringMissingException()
            }

        if (statuskort.aktiv) {
            repository.inaktiverStatuskort(statuskort.statuskortId)
            repository.loggEvent(statuskort.statuskortId, statuskort.ident, "inaktiver")
            log.info { "Inaktiverte statuskort etter event fra kafka" }
        } else {
            repository.loggEvent(statuskort.statuskortId, statuskort.ident, "inaktiver")
            log.info { "Behandlet inaktiver-event for allerede inaktivt statuskort" }
        }
    }
}

class StatuskortInaktiveringMissingException : MessageException("Fant ikke statuskort som skulle inaktiveres")
