package no.nav.tms.statuskort.statuskort

import com.fasterxml.jackson.module.kotlin.treeToValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.common.postgres.UniqueConstraintException
import no.nav.tms.kafka.application.JsonMessage
import no.nav.tms.kafka.application.MessageException
import no.nav.tms.kafka.application.Subscriber
import no.nav.tms.kafka.application.Subscription

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
            "innhold",
            "sensitivitet",
            "produsent",
        )

    override suspend fun receive(jsonMessage: JsonMessage) {
        val statuskortId = jsonMessage["statuskortId"].asText()
        log.info { "Opprett-event mottatt for statuskort" }

        val naa = nowAtUtc()
        val statuskort = Statuskort(
            statuskortId = statuskortId,
            ident = jsonMessage["ident"].asText(),
            innhold = objectMapper.treeToValue<Innhold>(jsonMessage["innhold"]),
            sensitivitet = parseSensitivitet(jsonMessage["sensitivitet"].asText()),
            produsent = objectMapper.treeToValue<Produsent>(jsonMessage["produsent"]),
            aktiv = true,
            opprettet = naa,
            sistEndret = naa,
            inaktivert = null,
        )

        try {
            repository.opprettStatuskort(statuskort)
            log.info { "Opprettet statuskort etter event fra kafka" }
        } catch (e: UniqueConstraintException) {
            log.info { "Ignorerte duplikat statuskort" }
            throw DuplikatStatuskortException()
        }
    }
}

class DuplikatStatuskortException : MessageException("Statuskort med samme statuskortId finnes allerede")
