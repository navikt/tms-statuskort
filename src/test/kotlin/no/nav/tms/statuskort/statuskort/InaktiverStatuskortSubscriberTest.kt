package no.nav.tms.statuskort.statuskort

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tms.kafka.application.MessageBroadcaster
import no.nav.tms.statuskort.database.LocalPostgresDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID

class InaktiverStatuskortSubscriberTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = StatuskortRepository(database)
    private val broadcaster = MessageBroadcaster(
        OpprettStatuskortSubscriber(repository),
        InaktiverStatuskortSubscriber(repository),
        enableTracking = true,
    )

    @AfterEach
    fun cleanUp() {
        LocalPostgresDatabase.cleanDb()
        broadcaster.clearHistory()
    }

    @Test
    fun `inaktiverer statuskort fra inaktiver-event`() {
        val statuskortId = randomUUID().toString()
        broadcaster.broadcastJson(opprettEvent(statuskortId))

        broadcaster.broadcastJson(inaktiverEvent(statuskortId))

        val statuskort = repository.hentStatuskort(statuskortId)
        statuskort.shouldNotBeNull()
        statuskort.aktiv shouldBe false
        statuskort.inaktivert.shouldNotBeNull()
    }

    @Test
    fun `inaktiver-event for allerede inaktivt statuskort er idempotent`() {
        val statuskortId = randomUUID().toString()
        broadcaster.broadcastJson(opprettEvent(statuskortId))
        broadcaster.broadcastJson(inaktiverEvent(statuskortId))

        broadcaster.broadcastJson(inaktiverEvent(statuskortId))

        val statuskort = repository.hentStatuskort(statuskortId)
        statuskort.shouldNotBeNull()
        statuskort.aktiv shouldBe false

        broadcaster.history().allFailedOutcomes(InaktiverStatuskortSubscriber::class).size shouldBe 0
    }

    @Test
    fun `forkaster inaktiver-event for ukjent statuskort`() {
        val statuskortId = randomUUID().toString()

        broadcaster.broadcastJson(inaktiverEvent(statuskortId))

        repository.hentStatuskort(statuskortId) shouldBe null

        broadcaster.history().findFailedOutcome(InaktiverStatuskortSubscriber::class) {
            it["statuskortId"].asText() == statuskortId
        }.let {
            it.shouldNotBeNull()
            it.cause::class shouldBe StatuskortInaktiveringMissingException::class
        }
    }
}
