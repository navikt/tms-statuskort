package no.nav.tms.statuskort

import no.nav.tms.common.postgres.Postgres
import no.nav.tms.common.util.config.StringEnvVar
import no.nav.tms.kafka.application.Domain
import no.nav.tms.kafka.application.KafkaApplication
import no.nav.tms.statuskort.events.InaktiverStatuskortSubscriber
import no.nav.tms.statuskort.events.OppdaterStatuskortSubscriber
import no.nav.tms.statuskort.events.OpprettStatuskortSubscriber
import no.nav.tms.statuskort.events.StatuskortRepository
import no.nav.tms.statuskort.api.StatuskortApiRepository
import no.nav.tms.statuskort.api.statuskortApi
import org.flywaydb.core.Flyway

fun main() {
    val environment = Environment()

    val database = Postgres.connectToJdbcUrl(environment.jdbcUrl)
    val statuskortRepository = StatuskortRepository(database)
    val statuskortApiRepository = StatuskortApiRepository(database)

    KafkaApplication.build {
        kafkaConfig {
            groupId = environment.groupId
            readTopics(environment.statuskortTopic)
        }

        ktorModule {
            statuskortApi(statuskortApiRepository)
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

        minSideMdc {
            domain = Domain.custom("statuskort")
            idFieldName = "statuskortId"
            producedBySupplier { message ->
                val produsentNode = message.json.get("produsent")

                if (produsentNode?.isObject == true) {
                    val cluster = produsentNode["cluster"].asText()
                    val namespace = produsentNode["namespace"].asText()
                    val appnavn = produsentNode["appnavn"].asText()

                    "$cluster:$namespace:$appnavn"
                } else {
                    null
                }
            }
        }
    }.start()
}

private data class Environment(
    val groupId: String = StringEnvVar.getEnvVar("KAFKA_GROUP_ID"),
    val statuskortTopic: String = StringEnvVar.getEnvVar("STATUSKORT_TOPIC"),
    val jdbcUrl: String = StringEnvVar.getEnvVar("DB_JDBC_URL"),
)
