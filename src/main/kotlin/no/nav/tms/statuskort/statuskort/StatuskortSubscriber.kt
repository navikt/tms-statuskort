package no.nav.tms.statuskort.statuskort

import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription

class StatuskortSubscriber(
    private val repository: StatuskortRepository,
) : Subscriber() {

    override fun subscribe(): Subscription = Subscription
        .forEvent("statuskort")
        .withFields("ident", "statuskortId")

    override suspend fun receive(jsonMessage: JsonMessage) {
    }
}
