package no.nav.tms.statuskort.database

import kotliquery.queryOf
import no.nav.tms.common.postgres.Postgres
import no.nav.tms.common.postgres.PostgresDatabase
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

object LocalPostgresDatabase {

    private val container = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:15"))

    val instance: PostgresDatabase by lazy {
        container.start()
        Postgres.connectToJdbcUrl(container.jdbcUrl) {
            username = container.username
            password = container.password
        }.also { migrate(it) }
    }

    fun cleanDb(): PostgresDatabase {
        instance.update { queryOf("delete from statuskort") }
        instance.update { queryOf("delete from statuskort_event_historikk") }
        return instance
    }

    private fun migrate(database: PostgresDatabase) {
        Flyway.configure()
            .connectRetries(3)
            .dataSource(database.dataSource)
            .load()
            .migrate()
    }
}
