package no.nav.tms.statuskort

import no.nav.tms.common.postgres.Postgres
import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.statuskort.setup.Environment
import no.nav.tms.statuskort.statuskort.InaktiverStatuskortSubscriber
import no.nav.tms.statuskort.statuskort.OppdaterStatuskortSubscriber
import no.nav.tms.statuskort.statuskort.OpprettStatuskortSubscriber
import no.nav.tms.statuskort.statuskort.StatuskortRepository
import org.flywaydb.core.Flyway

fun main() {
    val environment = Environment()

    val database = Postgres.connectToJdbcUrl(environment.jdbcUrl)
    val statuskortRepository = StatuskortRepository(database)

    KafkaApplication.build {
        kafkaConfig {
            groupId = environment.groupId
            readTopics(environment.statuskortTopic)
        }

        subscribers(
            OpprettStatuskortSubscriber(statuskortRepository),
            OppdaterStatuskortSubscriber(statuskortRepository),
            InaktiverStatuskortSubscriber(statuskortRepository),
        )

        onStartup {
            Flyway.configure()
                .dataSource(database.dataSource)
                .load()
                .migrate()
        }
    }.start()
}

object TmsStatuskort {
    const val appnavn = "tms-statuskort"
}
