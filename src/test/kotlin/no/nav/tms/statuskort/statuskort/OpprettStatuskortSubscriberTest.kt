package no.nav.tms.statuskort.statuskort

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.statuskort.database.LocalPostgresDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

class OpprettStatuskortSubscriberTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = StatuskortRepository(database)
    private val broadcaster = MessageBroadcaster(
        OpprettStatuskortSubscriber(repository),
        enableTracking = true,
    )

    @AfterEach
    fun cleanUp() {
        LocalPostgresDatabase.cleanDb()
        broadcaster.clearHistory()
    }

    @Test
    fun `oppretter statuskort fra opprett-event`() {
        val statuskortId = randomUUID().toString()

        broadcaster.broadcastJson(opprettEvent(statuskortId, tittel = "Min tittel"))

        val statuskort = repository.hentStatuskort(statuskortId)
        statuskort.shouldNotBeNull()
        statuskort.statuskortId shouldBe statuskortId
        statuskort.ident shouldBe "12345678901"
        statuskort.aktiv shouldBe true
        statuskort.inaktivert shouldBe null
        statuskort.sensitivitet shouldBe Sensitivitet.High
        statuskort.innhold.bokmaal.tittel shouldBe "Min tittel"
        statuskort.produsent.namespace shouldBe "min-side"
    }

    @Test
    fun `forkaster duplikat opprett-event`() {
        val statuskortId = randomUUID().toString()

        broadcaster.broadcastJson(opprettEvent(statuskortId, tittel = "Original"))
        broadcaster.broadcastJson(opprettEvent(statuskortId, tittel = "Duplikat"))

        val statuskort = repository.hentStatuskort(statuskortId)
        statuskort.shouldNotBeNull()
        statuskort.innhold.bokmaal.tittel shouldBe "Original"

        broadcaster.history().findFailedOutcome(OpprettStatuskortSubscriber::class) {
            it["statuskortId"].asText() == statuskortId
        }.let {
            it.shouldNotBeNull()
            it.cause::class shouldBe DuplikatStatuskortException::class
        }
    }
}
