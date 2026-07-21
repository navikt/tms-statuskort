package no.nav.tms.statuskort.statuskort

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.statuskort.database.LocalPostgresDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

class OppdaterStatuskortSubscriberTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = StatuskortRepository(database)
    private val broadcaster = MessageBroadcaster(
        OpprettStatuskortSubscriber(repository),
        OppdaterStatuskortSubscriber(repository),
        enableTracking = true,
    )

    @AfterEach
    fun cleanUp() {
        LocalPostgresDatabase.cleanDb()
        broadcaster.clearHistory()
    }

    @Test
    fun `oppdaterer innhold fra oppdater-event`() {
        val statuskortId = randomUUID().toString()
        broadcaster.broadcastJson(opprettEvent(statuskortId, tittel = "Før"))

        broadcaster.broadcastJson(oppdaterEvent(statuskortId, tittel = "Etter"))

        val statuskort = repository.hentStatuskort(statuskortId)
        statuskort.shouldNotBeNull()
        statuskort.innhold.bokmaal.tittel shouldBe "Etter"
        statuskort.innhold.nynorsk.tittel shouldBe "Etter"
        statuskort.innhold.engelsk.tittel shouldBe "Etter"
        statuskort.aktiv shouldBe true

        val historikk = repository.hentEventHistorikk(statuskortId)
        val oppdaterHistorikk = historikk.filter { it.eventType == "oppdater" }
        oppdaterHistorikk.size shouldBe 1
        oppdaterHistorikk[0].statuskortId shouldBe statuskortId
        oppdaterHistorikk[0].data.shouldNotBeNull()
    }

    @Test
    fun `forkaster oppdater-event for ukjent statuskort`() {
        val statuskortId = randomUUID().toString()

        broadcaster.broadcastJson(oppdaterEvent(statuskortId))

        repository.hentStatuskort(statuskortId) shouldBe null

        broadcaster.history().findFailedOutcome(OppdaterStatuskortSubscriber::class) {
            it["statuskortId"].asText() == statuskortId
        }.let {
            it.shouldNotBeNull()
            it.cause::class shouldBe StatuskortIkkeFunnetException::class
        }
    }

    @Test
    fun `forkaster oppdater-event for inaktivert statuskort`() {
        val statuskortId = randomUUID().toString()
        broadcaster.broadcastJson(opprettEvent(statuskortId, tittel = "Før"))
        repository.inaktiverStatuskort(statuskortId)

        broadcaster.broadcastJson(oppdaterEvent(statuskortId, tittel = "Etter"))

        val statuskort = repository.hentStatuskort(statuskortId)
        statuskort.shouldNotBeNull()
        statuskort.innhold.bokmaal.tittel shouldBe "Før"

        broadcaster.history().findFailedOutcome(OppdaterStatuskortSubscriber::class) {
            it["statuskortId"].asText() == statuskortId
        }.let {
            it.shouldNotBeNull()
            it.cause::class shouldBe StatuskortInaktivtException::class
        }

        val historikk = repository.hentEventHistorikk(statuskortId)
        historikk.none { it.eventType == "oppdater" } shouldBe true
    }
}
